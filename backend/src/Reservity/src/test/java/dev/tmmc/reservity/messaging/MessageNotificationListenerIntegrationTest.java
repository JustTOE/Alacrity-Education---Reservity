package dev.tmmc.reservity.messaging;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.tmmc.reservity.auth.dto.RegisterRequest;
import dev.tmmc.reservity.auth.repository.RefreshTokenRepository;
import dev.tmmc.reservity.messaging.entity.Conversation;
import dev.tmmc.reservity.messaging.repository.ConversationParticipantRepository;
import dev.tmmc.reservity.messaging.repository.ConversationRepository;
import dev.tmmc.reservity.messaging.repository.MessageRepository;
import dev.tmmc.reservity.messaging.service.MessageService;
import dev.tmmc.reservity.notifications.entity.Notification;
import dev.tmmc.reservity.notifications.entity.enums.NotificationType;
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
import dev.tmmc.reservity.user.entity.UserSettings;
import dev.tmmc.reservity.user.repository.UserRepository;
import dev.tmmc.reservity.user.repository.UserSettingsRepository;
import jakarta.mail.Message;
import jakarta.mail.internet.MimeMessage;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Verifies that sending a message triggers an in-app notification for the
 * recipient and (when prefs allow) an email via the async pool. Mirrors the
 * M6 test pattern: Mockito spy on {@link JavaMailSender} so the test does
 * not require a live MailHog.
 */
@SpringBootTest
@ActiveProfiles("test")
class MessageNotificationListenerIntegrationTest {

    @TestConfiguration
    static class MailMockConfig {
        @Bean
        @Primary
        JavaMailSender mailSender() {
            JavaMailSenderImpl impl = new JavaMailSenderImpl();
            impl.setHost("localhost");
            impl.setPort(1025);
            JavaMailSender spy = spy(impl);
            doNothing().when(spy).send(any(MimeMessage.class));
            return spy;
        }
    }

    @Autowired private SpaceTestFixtures fixtures;
    @Autowired private MessageService service;
    @Autowired private JavaMailSender mailSender;
    @Autowired private NotificationRepository notificationRepository;
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
    @Autowired private UserSettingsRepository userSettingsRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private RefreshTokenRepository refreshTokenRepository;
    @Autowired private dev.tmmc.reservity.events.repository.EventRsvpRepository eventRsvpRepository;
    @Autowired private dev.tmmc.reservity.events.repository.EventRepository eventRepository;
    private final ObjectMapper json = new ObjectMapper();

    @BeforeEach
    void cleanState() {
        reset(mailSender);
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
    }

    @Test
    void send_creates_notification_for_recipient_and_email() throws Exception {
        Scenario s = setup("hp");
        enableMessageEmails(s.ownerId);

        Conversation conv = service.findOrCreateForRequest(s.requestId, s.requesterId);
        service.send(conv.getId(), s.requesterId, "Quick question about the projector?");

        // Recipient is the owner (the OTHER party) — they get the notification.
        List<Notification> ns = notificationRepository.findAll();
        assertEquals(1, ns.size());
        Notification n = ns.get(0);
        assertEquals(NotificationType.MESSAGE_RECEIVED, n.getType());
        assertEquals(s.ownerId, n.getUser().getId());
        assertTrue(n.getLinkUrl().contains("/messages/" + conv.getId()));
        assertEquals("CONVERSATION", n.getRelatedType());
        assertEquals(conv.getId(), n.getRelatedId());

        Awaitility.await().atMost(Duration.ofSeconds(3))
                .untilAsserted(() -> verify(mailSender, times(1)).send(any(MimeMessage.class)));

        ArgumentCaptor<MimeMessage> cap = ArgumentCaptor.forClass(MimeMessage.class);
        verify(mailSender).send(cap.capture());
        MimeMessage sent = cap.getValue();
        assertTrue(sent.getSubject().contains("New message"));
        assertEquals("hp-owner@test.local",
                sent.getRecipients(Message.RecipientType.TO)[0].toString());
    }

    @Test
    void email_disabled_preference_suppresses_email_but_in_app_still_writes() throws Exception {
        Scenario s = setup("ed");
        // Owner has emailOnNewMessage = false (default — explicit for clarity).
        UserSettings settings = userSettingsRepository.findById(s.ownerId).orElseThrow();
        settings.setEmailOnNewMessage(false);
        userSettingsRepository.save(settings);

        Conversation conv = service.findOrCreateForRequest(s.requestId, s.requesterId);
        service.send(conv.getId(), s.requesterId, "First message");

        assertEquals(1, notificationRepository.count());

        try { TimeUnit.MILLISECONDS.sleep(500); } catch (InterruptedException ignored) {}
        verify(mailSender, never()).send(any(MimeMessage.class));
    }

    @Test
    void quiet_hours_suppress_email_but_in_app_still_writes() throws Exception {
        Scenario s = setup("qh");
        enableMessageEmails(s.ownerId);
        UserSettings settings = userSettingsRepository.findById(s.ownerId).orElseThrow();
        settings.setQuietHoursStart(LocalTime.of(0, 0));
        settings.setQuietHoursEnd(LocalTime.of(23, 59));
        settings.setTimezone("UTC");
        userSettingsRepository.save(settings);

        Conversation conv = service.findOrCreateForRequest(s.requestId, s.requesterId);
        service.send(conv.getId(), s.requesterId, "Hello during quiet hours");

        assertEquals(1, notificationRepository.count());

        try { TimeUnit.MILLISECONDS.sleep(500); } catch (InterruptedException ignored) {}
        verify(mailSender, never()).send(any(MimeMessage.class));
    }

    @Test
    void sender_does_not_get_notification_about_own_message() throws Exception {
        Scenario s = setup("sn");
        enableMessageEmails(s.ownerId);
        enableMessageEmails(s.requesterId);

        Conversation conv = service.findOrCreateForRequest(s.requestId, s.requesterId);
        service.send(conv.getId(), s.requesterId, "I am the sender");

        // Exactly 1 notification — for the owner. Sender (requester) gets nothing.
        List<Notification> ns = notificationRepository.findAll();
        assertEquals(1, ns.size());
        assertEquals(s.ownerId, ns.get(0).getUser().getId());
    }

    // ────────── helpers ──────────

    private record Scenario(UUID ownerId, UUID requesterId, UUID requestId) {}

    private Scenario setup(String prefix) throws Exception {
        User owner = registerUser(prefix + "-owner@test.local", prefix + "owner");
        User requester = registerUser(prefix + "-req@test.local", prefix + "req");

        Building b = fixtures.building("MsgB-" + prefix, sanitizeShortCode(prefix + "x"));
        Space space = fixtures.space("msg-" + prefix, "Lab " + prefix, SpaceType.LAB, b, owner,
                BigDecimal.ZERO, (short) 6, BigDecimal.valueOf(20));
        space.setInstantBook(false);
        spaceRepository.save(space);

        Instant tomorrow = Instant.now().plus(1, ChronoUnit.DAYS).truncatedTo(ChronoUnit.SECONDS);
        ReservationRequest req = requestRepository.save(ReservationRequest.builder()
                .reservationCode("RV-" + prefix.toUpperCase() + "-100001")
                .space(space)
                .requester(requester)
                .startsAt(tomorrow)
                .endsAt(tomorrow.plus(1, ChronoUnit.HOURS))
                .attendeesCount((short) 1)
                .tag(ReservationTag.SOLO_FOCUS)
                .status(ReservationStatus.PENDING)
                .autoApproved(false)
                .build());

        return new Scenario(owner.getId(), requester.getId(), req.getId());
    }

    private User registerUser(String email, String handle) throws Exception {
        // Go through the real auth flow so user_settings rows are auto-created.
        RegisterRequest reg = new RegisterRequest(email, "secret-password-123", "Test " + handle, handle);
        // Bypass MockMvc — call the repos. But auth/register sets up settings, so:
        // For simplicity, just create user + settings directly here.
        User u = userRepository.save(User.builder()
                .email(email)
                .passwordHash("$2a$12$0000000000000000000000000000000000000000000000000000")
                .handle(handle)
                .displayName("Test " + handle)
                .initials("T" + handle.toUpperCase().charAt(0))
                .memberSince(java.time.LocalDate.now())
                .build());
        userSettingsRepository.save(UserSettings.defaultsFor(u));
        return u;
    }

    private void enableMessageEmails(UUID userId) {
        UserSettings settings = userSettingsRepository.findById(userId).orElseThrow();
        settings.setEmailOnNewMessage(true);
        userSettingsRepository.save(settings);
    }

    private static String sanitizeShortCode(String raw) {
        String upper = raw.replaceAll("[^A-Za-z0-9]", "").toUpperCase();
        if (upper.length() < 2) upper = "BX" + upper;
        if (upper.length() > 8) upper = upper.substring(0, 8);
        return upper;
    }
}
