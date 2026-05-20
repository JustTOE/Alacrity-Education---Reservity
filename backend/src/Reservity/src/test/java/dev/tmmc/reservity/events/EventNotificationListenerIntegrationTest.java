package dev.tmmc.reservity.events;

import dev.tmmc.reservity.auth.repository.RefreshTokenRepository;
import dev.tmmc.reservity.events.dto.EventCreateRequest;
import dev.tmmc.reservity.events.entity.Event;
import dev.tmmc.reservity.events.entity.enums.EventCategory;
import dev.tmmc.reservity.events.entity.enums.EventHostType;
import dev.tmmc.reservity.events.entity.enums.EventVisibility;
import dev.tmmc.reservity.events.entity.enums.RsvpStatus;
import dev.tmmc.reservity.events.repository.EventRepository;
import dev.tmmc.reservity.events.repository.EventRsvpRepository;
import dev.tmmc.reservity.events.service.EventRsvpService;
import dev.tmmc.reservity.events.service.EventService;
import dev.tmmc.reservity.messaging.repository.ConversationParticipantRepository;
import dev.tmmc.reservity.messaging.repository.ConversationRepository;
import dev.tmmc.reservity.messaging.repository.MessageRepository;
import dev.tmmc.reservity.notifications.entity.Notification;
import dev.tmmc.reservity.notifications.entity.enums.NotificationType;
import dev.tmmc.reservity.notifications.repository.NotificationRepository;
import dev.tmmc.reservity.organization.repository.OrgMembershipRepository;
import dev.tmmc.reservity.organization.repository.OrganizationRepository;
import dev.tmmc.reservity.reservations.repository.ReservationRepository;
import dev.tmmc.reservity.reservations.repository.ReservationRequestRepository;
import dev.tmmc.reservity.reservations.repository.ReservationSeriesRepository;
import dev.tmmc.reservity.reservations.repository.SavedPassRepository;
import dev.tmmc.reservity.spaces.SpaceTestFixtures;
import dev.tmmc.reservity.spaces.repository.*;
import dev.tmmc.reservity.user.entity.User;
import dev.tmmc.reservity.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
class EventNotificationListenerIntegrationTest {

    @Autowired private EventService eventService;
    @Autowired private EventRsvpService rsvpService;
    @Autowired private SpaceTestFixtures fixtures;
    @Autowired private EventRepository eventRepository;
    @Autowired private EventRsvpRepository eventRsvpRepository;
    @Autowired private NotificationRepository notificationRepository;
    @Autowired private OrganizationRepository organizationRepository;
    @Autowired private OrgMembershipRepository orgMembershipRepository;
    @Autowired private MessageRepository messageRepository;
    @Autowired private ConversationParticipantRepository participantRepository;
    @Autowired private ConversationRepository conversationRepository;
    @Autowired private SavedPassRepository savedPassRepository;
    @Autowired private ReservationRepository reservationRepository;
    @Autowired private ReservationRequestRepository requestRepository;
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

    @BeforeEach
    void cleanState() {
        eventRsvpRepository.deleteAll();
        eventRepository.deleteAll();
        messageRepository.deleteAll();
        participantRepository.deleteAll();
        conversationRepository.deleteAll();
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
        orgMembershipRepository.deleteAll();
        organizationRepository.deleteAll();
        buildingRepository.deleteAll();
        refreshTokenRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void cancel_event_notifies_all_RSVPed_users() {
        Event event = createEvent(0, "cn1");
        User a = fixtures.ownerUser("rsvp-cn-a@test.local", "rsvpcna");
        User b = fixtures.ownerUser("rsvp-cn-b@test.local", "rsvpcnb");
        rsvpService.rsvp(event.getId(), a.getId(), RsvpStatus.GOING);
        rsvpService.rsvp(event.getId(), b.getId(), RsvpStatus.INTERESTED);

        eventService.cancel(event.getId(), "venue conflict", event.getHostId());

        List<Notification> aNotifs = notificationRepository
                .findByUserIdOrderByCreatedAtDesc(a.getId(),
                        org.springframework.data.domain.PageRequest.of(0, 20))
                .getContent();
        List<Notification> bNotifs = notificationRepository
                .findByUserIdOrderByCreatedAtDesc(b.getId(),
                        org.springframework.data.domain.PageRequest.of(0, 20))
                .getContent();

        assertTrue(aNotifs.stream().anyMatch(n ->
                n.getType() == NotificationType.EVENT_CANCELLED));
        assertTrue(bNotifs.stream().anyMatch(n ->
                n.getType() == NotificationType.EVENT_CANCELLED));
    }

    @Test
    void waitlist_promotion_notifies_promoted_user() {
        Event event = createEvent(1, "pr1");
        User a = fixtures.ownerUser("rsvp-pr-a@test.local", "rsvppra");
        User b = fixtures.ownerUser("rsvp-pr-b@test.local", "rsvpprb");
        rsvpService.rsvp(event.getId(), a.getId(), RsvpStatus.GOING);     // GOING
        rsvpService.rsvp(event.getId(), b.getId(), RsvpStatus.GOING);     // WAITLIST

        rsvpService.cancel(event.getId(), a.getId());

        List<Notification> bNotifs = notificationRepository
                .findByUserIdOrderByCreatedAtDesc(b.getId(),
                        org.springframework.data.domain.PageRequest.of(0, 20))
                .getContent();
        assertTrue(bNotifs.stream().anyMatch(n ->
                n.getType() == NotificationType.EVENT_RSVP_PROMOTED));
    }

    @Test
    void cancelled_user_does_not_receive_event_cancelled_notification() {
        Event event = createEvent(0, "cn2");
        User a = fixtures.ownerUser("rsvp-cn2-a@test.local", "rsvpcn2a");
        User b = fixtures.ownerUser("rsvp-cn2-b@test.local", "rsvpcn2b");
        rsvpService.rsvp(event.getId(), a.getId(), RsvpStatus.GOING);
        rsvpService.rsvp(event.getId(), b.getId(), RsvpStatus.GOING);
        rsvpService.cancel(event.getId(), b.getId());   // B cancels — should not get the EVENT_CANCELLED
        notificationRepository.deleteAll();             // Clear pre-existing notifications.

        eventService.cancel(event.getId(), "host cancelled", event.getHostId());

        List<Notification> bNotifs = notificationRepository
                .findByUserIdOrderByCreatedAtDesc(b.getId(),
                        org.springframework.data.domain.PageRequest.of(0, 20))
                .getContent();
        assertTrue(bNotifs.stream().noneMatch(n ->
                n.getType() == NotificationType.EVENT_CANCELLED),
                "Cancelled-RSVP user should not be notified of cancellation");

        List<Notification> aNotifs = notificationRepository
                .findByUserIdOrderByCreatedAtDesc(a.getId(),
                        org.springframework.data.domain.PageRequest.of(0, 20))
                .getContent();
        assertTrue(aNotifs.stream().anyMatch(n ->
                n.getType() == NotificationType.EVENT_CANCELLED));
    }

    private Event createEvent(int capacity, String slugSuffix) {
        User host = fixtures.ownerUser("evhost-" + slugSuffix + "@test.local",
                truncateHandle("evh" + slugSuffix));
        Instant start = Instant.now().plus(Duration.ofDays(1));
        Instant end = start.plus(Duration.ofHours(2));
        EventCreateRequest req = new EventCreateRequest(
                "Event " + slugSuffix, "blurb", null, null,
                start, end,
                EventHostType.USER, host.getId(),
                null, null, null, "Auditorium",
                EventCategory.PERFORMANCE, "Open", capacity, false, EventVisibility.PUBLIC);
        return eventService.create(req, host.getId());
    }

    private static String truncateHandle(String h) {
        String safe = h.toLowerCase().replaceAll("[^a-z0-9_]", "");
        if (safe.length() < 3) safe = "h" + safe + "x";
        if (safe.length() > 30) safe = safe.substring(0, 30);
        return safe;
    }
}
