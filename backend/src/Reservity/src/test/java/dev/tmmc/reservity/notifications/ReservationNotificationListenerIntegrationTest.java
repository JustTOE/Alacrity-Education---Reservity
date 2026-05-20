package dev.tmmc.reservity.notifications;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.tmmc.reservity.auth.dto.RegisterRequest;
import dev.tmmc.reservity.auth.repository.RefreshTokenRepository;
import dev.tmmc.reservity.messaging.repository.ConversationParticipantRepository;
import dev.tmmc.reservity.messaging.repository.ConversationRepository;
import dev.tmmc.reservity.messaging.repository.MessageRepository;
import dev.tmmc.reservity.notifications.entity.Notification;
import dev.tmmc.reservity.notifications.entity.enums.NotificationType;
import dev.tmmc.reservity.notifications.repository.NotificationRepository;
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
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * End-to-end test that POSTing a reservation triggers both the in-app row and
 * an email send (or correctly suppresses email per preferences). Uses a Mockito
 * spy on {@link JavaMailSender} so the test does not require a live MailHog.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ReservationNotificationListenerIntegrationTest {

    @TestConfiguration
    static class MailMockConfig {
        /**
         * A spy on JavaMailSenderImpl with the {@code send(MimeMessage)} call stubbed
         * to a no-op so tests do not require a live SMTP server. The createMimeMessage()
         * path stays real so the listener can actually build the message; only the
         * outbound send is intercepted.
         */
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

    @Autowired private MockMvc mvc;
    @Autowired private SpaceTestFixtures fixtures;
    @Autowired private JavaMailSender mailSender;
    @Autowired private NotificationRepository notificationRepository;
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
    @Autowired private MessageRepository messageRepository;
    @Autowired private ConversationParticipantRepository conversationParticipantRepository;
    @Autowired private ConversationRepository conversationRepository;
    @Autowired private dev.tmmc.reservity.events.repository.EventRsvpRepository eventRsvpRepository;
    @Autowired private dev.tmmc.reservity.events.repository.EventRepository eventRepository;
    private final ObjectMapper json = new ObjectMapper();

    @BeforeEach
    void cleanState() {
        reset(mailSender);
        messageRepository.deleteAll();
        conversationParticipantRepository.deleteAll();
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
        // user_settings has FK->users with cascade, so userRepository.deleteAll() handles both.
        userRepository.deleteAll();
    }

    @Test
    void approval_creates_in_app_row_and_sends_email() throws Exception {
        // Owner of the space.
        String ownerToken = registerAndGetToken("apv-owner@test.local", "apvowner");
        UUID ownerId = currentUserId(ownerToken);
        seedInstantSpace(ownerId, "lab-4b", "Chemistry Lab 4B");

        // Requester gets the notification.
        String requesterToken = registerAndGetToken("apv-req@test.local", "apvreq");
        UUID requesterId = currentUserId(requesterToken);

        mvc.perform(post("/api/spaces/lab-4b/reservation-requests")
                        .header("Authorization", "Bearer " + requesterToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(rangeBody(todayPlus(1, 10), todayPlus(1, 11))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("APPROVED"));

        // In-app: should have 1 RESERVATION_APPROVED for the requester.
        List<Notification> ns = notificationRepository.findAll();
        assertEquals(1, ns.size());
        Notification n = ns.get(0);
        assertEquals(NotificationType.RESERVATION_APPROVED, n.getType());
        assertEquals(requesterId, n.getUser().getId());
        assertNotNull(n.getLinkUrl());
        assertTrue(n.getLinkUrl().contains("/spaces/lab-4b"));
        assertEquals("RESERVATION", n.getRelatedType());

        // Email: async — wait up to 3s.
        Awaitility.await().atMost(Duration.ofSeconds(3))
                .untilAsserted(() -> verify(mailSender, times(1)).send(any(MimeMessage.class)));

        ArgumentCaptor<MimeMessage> cap = ArgumentCaptor.forClass(MimeMessage.class);
        verify(mailSender).send(cap.capture());
        MimeMessage sent = cap.getValue();
        assertEquals("Your booking is confirmed", sent.getSubject());
        assertEquals("apv-req@test.local", sent.getRecipients(Message.RecipientType.TO)[0].toString());
    }

    @Test
    void denial_creates_in_app_row_and_sends_email() throws Exception {
        String ownerToken = registerAndGetToken("dn-owner@test.local", "dnowner");
        UUID ownerId = currentUserId(ownerToken);
        seedNonInstantSpace(ownerId, "lab-deny");

        String requesterToken = registerAndGetToken("dn-req@test.local", "dnreq");
        UUID requesterId = currentUserId(requesterToken);

        UUID requestId = submitForId(requesterToken, "lab-deny",
                rangeBody(todayPlus(1, 11), todayPlus(1, 12)));
        // Submission of a non-instant booking does NOT publish an event in M5/M6.
        // Only the deny step does.
        notificationRepository.deleteAll();
        reset(mailSender);

        mvc.perform(post("/api/reservation-requests/" + requestId + "/deny")
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\":\"Lab closed for maintenance\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DENIED"));

        List<Notification> ns = notificationRepository.findAll();
        assertEquals(1, ns.size());
        Notification n = ns.get(0);
        assertEquals(NotificationType.RESERVATION_DENIED, n.getType());
        assertEquals(requesterId, n.getUser().getId());
        assertTrue(n.getBody().contains("Lab closed for maintenance"));
        assertEquals("RESERVATION_REQUEST", n.getRelatedType());

        Awaitility.await().atMost(Duration.ofSeconds(3))
                .untilAsserted(() -> verify(mailSender, times(1)).send(any(MimeMessage.class)));

        ArgumentCaptor<MimeMessage> cap = ArgumentCaptor.forClass(MimeMessage.class);
        verify(mailSender).send(cap.capture());
        assertEquals("Your booking request was declined", cap.getValue().getSubject());
    }

    @Test
    void quiet_hours_suppress_email_but_in_app_still_writes() throws Exception {
        String ownerToken = registerAndGetToken("qh-owner@test.local", "qhowner");
        UUID ownerId = currentUserId(ownerToken);
        seedInstantSpace(ownerId, "lab-qh", "Lab QH");

        String requesterToken = registerAndGetToken("qh-req@test.local", "qhreq");
        UUID requesterId = currentUserId(requesterToken);
        // Force quiet hours to "always".
        UserSettings settings = userSettingsRepository.findById(requesterId).orElseThrow();
        settings.setQuietHoursStart(LocalTime.of(0, 0));
        settings.setQuietHoursEnd(LocalTime.of(23, 59));
        settings.setTimezone("UTC");
        userSettingsRepository.save(settings);

        mvc.perform(post("/api/spaces/lab-qh/reservation-requests")
                        .header("Authorization", "Bearer " + requesterToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(rangeBody(todayPlus(1, 13), todayPlus(1, 14))))
                .andExpect(status().isCreated());

        // In-app row exists.
        assertEquals(1, notificationRepository.count());

        // Wait briefly to allow async — but expect NO send.
        try { TimeUnit.MILLISECONDS.sleep(500); } catch (InterruptedException ignored) {}
        verify(mailSender, never()).send(any(MimeMessage.class));
    }

    @Test
    void email_disabled_preference_suppresses_email_but_in_app_still_writes() throws Exception {
        String ownerToken = registerAndGetToken("ep-owner@test.local", "epowner");
        UUID ownerId = currentUserId(ownerToken);
        seedInstantSpace(ownerId, "lab-ep", "Lab EP");

        String requesterToken = registerAndGetToken("ep-req@test.local", "epreq");
        UUID requesterId = currentUserId(requesterToken);
        UserSettings settings = userSettingsRepository.findById(requesterId).orElseThrow();
        settings.setEmailOnRequestDecided(false);
        userSettingsRepository.save(settings);

        mvc.perform(post("/api/spaces/lab-ep/reservation-requests")
                        .header("Authorization", "Bearer " + requesterToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(rangeBody(todayPlus(1, 14), todayPlus(1, 15))))
                .andExpect(status().isCreated());

        assertEquals(1, notificationRepository.count());

        try { TimeUnit.MILLISECONDS.sleep(500); } catch (InterruptedException ignored) {}
        verify(mailSender, never()).send(any(MimeMessage.class));
    }

    // ────────── helpers ──────────

    private Space seedInstantSpace(UUID ownerId, String slug, String name) {
        Building b = fixtures.building("Building " + slug, sanitizeShortCode(slug));
        User owner = userRepository.findById(ownerId).orElseThrow();
        return fixtures.space(slug, name, SpaceType.LAB, b, owner,
                BigDecimal.valueOf(0), (short) 8, BigDecimal.valueOf(30));
    }

    private Space seedNonInstantSpace(UUID ownerId, String slug) {
        Building b = fixtures.building("Building " + slug, sanitizeShortCode(slug));
        User owner = userRepository.findById(ownerId).orElseThrow();
        Space s = fixtures.space(slug, "Lab " + slug, SpaceType.LAB, b, owner,
                BigDecimal.valueOf(0), (short) 8, BigDecimal.valueOf(30));
        s.setInstantBook(false);
        return spaceRepository.save(s);
    }

    /** Buildings.short_code matches ^[A-Z0-9]{2,8}$ — strip hyphens, uppercase, clamp 2..8 chars. */
    private static String sanitizeShortCode(String raw) {
        String upper = raw.replaceAll("[^A-Za-z0-9]", "").toUpperCase();
        if (upper.length() < 2) upper = "BX" + upper;
        if (upper.length() > 8) upper = upper.substring(0, 8);
        return upper;
    }

    private String registerAndGetToken(String email, String handle) throws Exception {
        RegisterRequest reg = new RegisterRequest(email, "secret-password-123", "Test " + handle, handle);
        MvcResult result = mvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsBytes(reg)))
                .andExpect(status().isCreated())
                .andReturn();
        return json.readTree(result.getResponse().getContentAsByteArray()).get("accessToken").asText();
    }

    private UUID currentUserId(String token) throws Exception {
        MvcResult me = mvc.perform(get("/api/auth/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode meBody = json.readTree(me.getResponse().getContentAsByteArray());
        return UUID.fromString(meBody.get("id").asText());
    }

    private UUID submitForId(String token, String slug, byte[] body) throws Exception {
        MvcResult res = mvc.perform(post("/api/spaces/" + slug + "/reservation-requests")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andReturn();
        JsonNode body2 = json.readTree(res.getResponse().getContentAsByteArray());
        return UUID.fromString(body2.get("requestId").asText());
    }

    private byte[] rangeBody(Instant start, Instant end) {
        return ("{\"startsAt\":\"" + start + "\",\"endsAt\":\"" + end + "\"}").getBytes();
    }

    private static Instant todayPlus(int days, int hour) {
        LocalDate target = LocalDate.now(ZoneOffset.UTC).plusDays(days);
        return LocalDateTime.of(target, LocalTime.of(hour, 0)).toInstant(ZoneOffset.UTC).truncatedTo(ChronoUnit.SECONDS);
    }
}
