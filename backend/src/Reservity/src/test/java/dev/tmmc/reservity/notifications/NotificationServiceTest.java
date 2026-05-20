package dev.tmmc.reservity.notifications;

import dev.tmmc.reservity.auth.repository.RefreshTokenRepository;
import dev.tmmc.reservity.common.exception.EntityNotFoundException;
import dev.tmmc.reservity.common.pagination.PageResponse;
import dev.tmmc.reservity.events.repository.EventRepository;
import dev.tmmc.reservity.events.repository.EventRsvpRepository;
import dev.tmmc.reservity.messaging.repository.ConversationParticipantRepository;
import dev.tmmc.reservity.messaging.repository.ConversationRepository;
import dev.tmmc.reservity.messaging.repository.MessageRepository;
import dev.tmmc.reservity.notifications.dto.NotificationResponse;
import dev.tmmc.reservity.notifications.entity.Notification;
import dev.tmmc.reservity.notifications.entity.enums.NotificationType;
import dev.tmmc.reservity.notifications.repository.NotificationRepository;
import dev.tmmc.reservity.notifications.service.NotificationService;
import dev.tmmc.reservity.reservations.repository.ReservationRepository;
import dev.tmmc.reservity.reservations.repository.ReservationRequestRepository;
import dev.tmmc.reservity.reservations.repository.ReservationSeriesRepository;
import dev.tmmc.reservity.reservations.repository.SavedPassRepository;
import dev.tmmc.reservity.spaces.repository.BuildingRepository;
import dev.tmmc.reservity.spaces.repository.SpaceClosureRepository;
import dev.tmmc.reservity.spaces.repository.SpaceFavoriteRepository;
import dev.tmmc.reservity.spaces.repository.SpaceImageRepository;
import dev.tmmc.reservity.spaces.repository.SpaceRepository;
import dev.tmmc.reservity.spaces.repository.SpaceVibeRepository;
import dev.tmmc.reservity.spaces.repository.SpaceWaitlistRepository;
import dev.tmmc.reservity.user.entity.User;
import dev.tmmc.reservity.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
class NotificationServiceTest {

    @Autowired private NotificationService service;
    @Autowired private NotificationRepository repository;
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
    @Autowired private MessageRepository messageRepository;
    @Autowired private ConversationParticipantRepository conversationParticipantRepository;
    @Autowired private ConversationRepository conversationRepository;
    @Autowired private EventRsvpRepository eventRsvpRepository;
    @Autowired private EventRepository eventRepository;

    @BeforeEach
    void cleanState() {
        // Cross-class state may leak via the shared Postgres in test profile —
        // wipe in reverse-FK order before this class creates anything.
        messageRepository.deleteAll();
        conversationParticipantRepository.deleteAll();
        conversationRepository.deleteAll();
        eventRsvpRepository.deleteAll();
        eventRepository.deleteAll();
        repository.deleteAll();
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
    }

    @Test
    void create_in_app_persists_a_row_with_expected_fields() {
        User u = saveUser("alice@test.local", "alice");
        Notification n = service.createInApp(u.getId(),
                NotificationType.RESERVATION_APPROVED,
                "Booking confirmed: Lab 4B",
                "Your reservation RV-LAB4B-123 is locked in.",
                "/spaces/lab-4b",
                "RESERVATION",
                UUID.randomUUID());

        assertNotNull(n.getId());
        assertNull(n.getReadAt());
        assertNotNull(n.getCreatedAt());
        assertEquals("Booking confirmed: Lab 4B", n.getTitle());
        assertEquals(NotificationType.RESERVATION_APPROVED, n.getType());
    }

    @Test
    void list_returns_DESC_ordered_page() throws Exception {
        User u = saveUser("bob@test.local", "bob");
        service.createInApp(u.getId(), NotificationType.RESERVATION_APPROVED, "first",  "b1", "/x", "RESERVATION", UUID.randomUUID());
        Thread.sleep(10);
        service.createInApp(u.getId(), NotificationType.RESERVATION_DENIED,   "second", "b2", "/y", "RESERVATION_REQUEST", UUID.randomUUID());
        Thread.sleep(10);
        service.createInApp(u.getId(), NotificationType.RESERVATION_APPROVED, "third",  "b3", "/z", "RESERVATION", UUID.randomUUID());

        PageResponse<NotificationResponse> page = service.listForUser(u.getId(), false, 0, 10);
        assertEquals(3, page.getTotalElements());
        assertEquals("third", page.getContent().get(0).title());
        assertEquals("second", page.getContent().get(1).title());
        assertEquals("first", page.getContent().get(2).title());
    }

    @Test
    void unread_count_reflects_state() {
        User u = saveUser("carol@test.local", "carol");
        Notification a = service.createInApp(u.getId(), NotificationType.RESERVATION_APPROVED, "a", "ba", "/", "R", UUID.randomUUID());
        service.createInApp(u.getId(), NotificationType.RESERVATION_DENIED, "b", "bb", "/", "R", UUID.randomUUID());
        service.createInApp(u.getId(), NotificationType.RESERVATION_APPROVED, "c", "bc", "/", "R", UUID.randomUUID());

        assertEquals(3, service.unreadCount(u.getId()));
        service.markRead(u.getId(), a.getId());
        assertEquals(2, service.unreadCount(u.getId()));
    }

    @Test
    void mark_read_is_idempotent() {
        User u = saveUser("dan@test.local", "dan");
        Notification n = service.createInApp(u.getId(), NotificationType.RESERVATION_APPROVED, "t", "b", "/", "R", UUID.randomUUID());

        service.markRead(u.getId(), n.getId());
        long firstReadAt = repository.findById(n.getId()).orElseThrow().getReadAt().toEpochMilli();

        // Second call should NOT bump readAt or fail.
        assertDoesNotThrow(() -> service.markRead(u.getId(), n.getId()));
        long secondReadAt = repository.findById(n.getId()).orElseThrow().getReadAt().toEpochMilli();
        assertEquals(firstReadAt, secondReadAt);
    }

    @Test
    void mark_all_read_returns_count_and_resets_unread() {
        User u = saveUser("eve@test.local", "eve");
        service.createInApp(u.getId(), NotificationType.RESERVATION_APPROVED, "a", "ba", "/", "R", UUID.randomUUID());
        service.createInApp(u.getId(), NotificationType.RESERVATION_DENIED,   "b", "bb", "/", "R", UUID.randomUUID());

        int updated = service.markAllRead(u.getId());
        assertEquals(2, updated);
        assertEquals(0, service.unreadCount(u.getId()));

        // No-op second call
        assertEquals(0, service.markAllRead(u.getId()));
    }

    @Test
    @Transactional
    void cross_tenant_delete_throws_404() {
        User a = saveUser("a@test.local", "afoo");
        User b = saveUser("b@test.local", "bfoo");
        Notification n = service.createInApp(a.getId(), NotificationType.RESERVATION_APPROVED, "t", "b", "/", "R", UUID.randomUUID());

        assertThrows(EntityNotFoundException.class,
                () -> service.delete(b.getId(), n.getId()));

        // a's notification still exists.
        assertTrue(repository.findById(n.getId()).isPresent());
    }

    @Test
    void delete_removes_the_row() {
        User u = saveUser("f@test.local", "ffoo");
        Notification n = service.createInApp(u.getId(), NotificationType.RESERVATION_APPROVED, "t", "b", "/", "R", UUID.randomUUID());

        service.delete(u.getId(), n.getId());
        assertFalse(repository.findById(n.getId()).isPresent());
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
