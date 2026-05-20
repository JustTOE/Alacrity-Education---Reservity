package dev.tmmc.reservity.messaging;

import dev.tmmc.reservity.auth.repository.RefreshTokenRepository;
import dev.tmmc.reservity.common.exception.ForbiddenOperationException;
import dev.tmmc.reservity.messaging.entity.Conversation;
import dev.tmmc.reservity.messaging.entity.Message;
import dev.tmmc.reservity.messaging.repository.ConversationParticipantRepository;
import dev.tmmc.reservity.messaging.repository.ConversationRepository;
import dev.tmmc.reservity.messaging.repository.MessageRepository;
import dev.tmmc.reservity.messaging.service.MessageService;
import dev.tmmc.reservity.notifications.repository.NotificationRepository;
import dev.tmmc.reservity.reservations.entity.ReservationRequest;
import dev.tmmc.reservity.reservations.entity.ReservationStatus;
import dev.tmmc.reservity.reservations.entity.ReservationTag;
import dev.tmmc.reservity.reservations.repository.ReservationRepository;
import dev.tmmc.reservity.reservations.repository.ReservationRequestRepository;
import dev.tmmc.reservity.reservations.repository.ReservationSeriesRepository;
import dev.tmmc.reservity.reservations.repository.SavedPassRepository;
import dev.tmmc.reservity.spaces.SpaceTestFixtures;
import dev.tmmc.reservity.spaces.entity.Building;
import dev.tmmc.reservity.spaces.entity.Space;
import dev.tmmc.reservity.spaces.entity.SpaceType;
import dev.tmmc.reservity.spaces.repository.*;
import dev.tmmc.reservity.user.entity.User;
import dev.tmmc.reservity.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Service-layer test for the M7 messaging hot paths. Exercises the
 * find-or-create flow, the participant-resolution rules, and the
 * cross-tenant guard. Heavy integration scenarios live in
 * {@code MessageNotificationListenerIntegrationTest}.
 */
@SpringBootTest
@ActiveProfiles("test")
class MessageServiceTest {

    @Autowired private MessageService service;
    @Autowired private SpaceTestFixtures fixtures;
    @Autowired private ReservationRequestRepository requestRepository;
    @Autowired private ConversationRepository conversationRepository;
    @Autowired private ConversationParticipantRepository participantRepository;
    @Autowired private MessageRepository messageRepository;
    @Autowired private NotificationRepository notificationRepository;
    @Autowired private SavedPassRepository savedPassRepository;
    @Autowired private ReservationRepository reservationRepository;
    @Autowired private ReservationSeriesRepository seriesRepository;
    @Autowired private SpaceClosureRepository closureRepository;
    @Autowired private SpaceFavoriteRepository favoriteRepository;
    @Autowired private SpaceWaitlistRepository waitlistRepository;
    @Autowired private SpaceVibeRepository spaceVibeRepository;
    @Autowired private SpaceImageRepository spaceImageRepository;
    @Autowired private SpaceRepository spaceRepository;
    @Autowired private BuildingRepository buildingRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private RefreshTokenRepository refreshTokenRepository;
    @Autowired private dev.tmmc.reservity.events.repository.EventRsvpRepository eventRsvpRepository;
    @Autowired private dev.tmmc.reservity.events.repository.EventRepository eventRepository;

    private UUID requesterId;
    private UUID ownerId;
    private UUID requestId;

    @BeforeEach
    void cleanState() {
        // Reverse-FK order including M7 messaging tables and M8 events.
        messageRepository.deleteAll();
        participantRepository.deleteAll();
        conversationRepository.deleteAll();
        eventRsvpRepository.deleteAll();
        eventRepository.deleteAll();
        notificationRepository.deleteAll();
        savedPassRepository.deleteAll();
        reservationRepository.deleteAll();
        requestRepository.deleteAll();
        seriesRepository.deleteAll();
        closureRepository.deleteAll();
        favoriteRepository.deleteAll();
        waitlistRepository.deleteAll();
        spaceVibeRepository.deleteAll();
        spaceImageRepository.deleteAll();
        spaceRepository.deleteAll();
        buildingRepository.deleteAll();
        refreshTokenRepository.deleteAll();
        userRepository.deleteAll();

        User owner = saveUser("owner@test.local", "msvowner");
        User requester = saveUser("requester@test.local", "msvreq");
        ownerId = owner.getId();
        requesterId = requester.getId();

        Building b = fixtures.building("MessageBldg", "MSG");
        Space space = fixtures.space("msg-space", "Message Space", SpaceType.LAB, b, owner,
                BigDecimal.ZERO, (short) 4, BigDecimal.valueOf(20));

        Instant tomorrow = Instant.now().plus(1, ChronoUnit.DAYS).truncatedTo(ChronoUnit.SECONDS);
        ReservationRequest req = ReservationRequest.builder()
                .reservationCode("RV-MSGSPACE-100001")
                .space(space)
                .requester(requester)
                .startsAt(tomorrow)
                .endsAt(tomorrow.plus(1, ChronoUnit.HOURS))
                .attendeesCount((short) 1)
                .tag(ReservationTag.SOLO_FOCUS)
                .status(ReservationStatus.PENDING)
                .autoApproved(false)
                .build();
        requestId = requestRepository.save(req).getId();
    }

    @Test
    void requester_can_open_conversation_about_own_request() {
        Conversation conv = service.findOrCreateForRequest(requestId, requesterId);
        assertNotNull(conv.getId());
        assertEquals(2, participantRepository.findByConversationId(conv.getId()).size());
    }

    @Test
    void owner_can_open_conversation_about_their_space() {
        Conversation conv = service.findOrCreateForRequest(requestId, ownerId);
        assertNotNull(conv.getId());
        assertEquals(2, participantRepository.findByConversationId(conv.getId()).size());
    }

    @Test
    void unrelated_user_cannot_open_conversation() {
        User stranger = saveUser("stranger@test.local", "stranger");
        assertThrows(ForbiddenOperationException.class,
                () -> service.findOrCreateForRequest(requestId, stranger.getId()));
    }

    @Test
    void find_or_create_is_idempotent() {
        Conversation a = service.findOrCreateForRequest(requestId, requesterId);
        Conversation b = service.findOrCreateForRequest(requestId, requesterId);
        assertEquals(a.getId(), b.getId());
        assertEquals(1, conversationRepository.findAll().size());
    }

    @Test
    void send_persists_message_and_touches_conversation() throws Exception {
        Conversation conv = service.findOrCreateForRequest(requestId, requesterId);
        Instant before = conversationRepository.findById(conv.getId()).orElseThrow().getUpdatedAt();
        Thread.sleep(10);

        Message msg = service.send(conv.getId(), requesterId, "Hi! Can I bring a guest?");
        assertNotNull(msg.getId());
        assertEquals("Hi! Can I bring a guest?", msg.getContent());

        Conversation reloaded = conversationRepository.findById(conv.getId()).orElseThrow();
        assertTrue(reloaded.getUpdatedAt().isAfter(before));
    }

    @Test
    void send_from_non_participant_throws_forbidden() {
        Conversation conv = service.findOrCreateForRequest(requestId, requesterId);
        User stranger = saveUser("stranger2@test.local", "stranger2");
        assertThrows(ForbiddenOperationException.class,
                () -> service.send(conv.getId(), stranger.getId(), "I should not be here"));
    }

    private User saveUser(String email, String handle) {
        return userRepository.save(User.builder()
                .email(email)
                .passwordHash("$2a$12$0000000000000000000000000000000000000000000000000000")
                .handle(handle)
                .displayName("Test " + handle)
                .initials("T" + handle.toUpperCase().charAt(0))
                .memberSince(LocalDate.now())
                .build());
    }
}
