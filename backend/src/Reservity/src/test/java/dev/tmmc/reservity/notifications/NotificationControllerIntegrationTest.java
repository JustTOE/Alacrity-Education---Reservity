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
import dev.tmmc.reservity.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class NotificationControllerIntegrationTest {

    @Autowired private MockMvc mvc;
    @Autowired private NotificationService service;
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
    @Autowired private RefreshTokenRepository refreshTokenRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private MessageRepository messageRepository;
    @Autowired private ConversationParticipantRepository conversationParticipantRepository;
    @Autowired private ConversationRepository conversationRepository;
    @Autowired private dev.tmmc.reservity.events.repository.EventRsvpRepository eventRsvpRepository;
    @Autowired private dev.tmmc.reservity.events.repository.EventRepository eventRepository;
    private final ObjectMapper json = new ObjectMapper();

    @BeforeEach
    void cleanState() {
        // Reverse-FK order — cross-class state may leak via the shared test Postgres.
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
        userRepository.deleteAll();
    }

    @Test
    void list_unread_count_mark_read_read_all_delete_all_work() throws Exception {
        String token = registerAndGetToken("user@test.local", "ucfoo");
        UUID userId = currentUserId(token);

        Notification a = service.createInApp(userId, NotificationType.RESERVATION_APPROVED, "ta", "ba", "/x", "R", UUID.randomUUID());
        Notification b = service.createInApp(userId, NotificationType.RESERVATION_DENIED, "tb", "bb", "/y", "R", UUID.randomUUID());
        Notification c = service.createInApp(userId, NotificationType.RESERVATION_APPROVED, "tc", "bc", "/z", "R", UUID.randomUUID());

        // List all -> 3
        mvc.perform(get("/api/notifications").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(3))
                .andExpect(jsonPath("$.content.length()").value(3));

        // unread-count -> 3
        mvc.perform(get("/api/notifications/unread-count").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.count").value(3));

        // mark one read
        mvc.perform(patch("/api/notifications/" + a.getId() + "/read").header("Authorization", "Bearer " + token))
                .andExpect(status().isNoContent());
        mvc.perform(get("/api/notifications/unread-count").header("Authorization", "Bearer " + token))
                .andExpect(jsonPath("$.count").value(2));

        // unread-only filter
        mvc.perform(get("/api/notifications").param("unread", "true").header("Authorization", "Bearer " + token))
                .andExpect(jsonPath("$.totalElements").value(2));

        // read-all
        mvc.perform(post("/api/notifications/read-all").header("Authorization", "Bearer " + token))
                .andExpect(status().isNoContent());
        mvc.perform(get("/api/notifications/unread-count").header("Authorization", "Bearer " + token))
                .andExpect(jsonPath("$.count").value(0));

        // delete one
        mvc.perform(delete("/api/notifications/" + b.getId()).header("Authorization", "Bearer " + token))
                .andExpect(status().isNoContent());
        mvc.perform(get("/api/notifications").header("Authorization", "Bearer " + token))
                .andExpect(jsonPath("$.totalElements").value(2));
    }

    @Test
    void unauthenticated_returns_403() throws Exception {
        mvc.perform(get("/api/notifications"))
                .andExpect(status().isForbidden());
        mvc.perform(get("/api/notifications/unread-count"))
                .andExpect(status().isForbidden());
    }

    @Test
    void cross_tenant_patch_returns_404() throws Exception {
        String aToken = registerAndGetToken("xa@test.local", "xafoo");
        UUID aId = currentUserId(aToken);
        Notification aNotif = service.createInApp(aId, NotificationType.RESERVATION_APPROVED, "ta", "ba", "/x", "R", UUID.randomUUID());

        String bToken = registerAndGetToken("xb@test.local", "xbfoo");

        mvc.perform(patch("/api/notifications/" + aNotif.getId() + "/read")
                        .header("Authorization", "Bearer " + bToken))
                .andExpect(status().isNotFound());

        mvc.perform(delete("/api/notifications/" + aNotif.getId())
                        .header("Authorization", "Bearer " + bToken))
                .andExpect(status().isNotFound());
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
}
