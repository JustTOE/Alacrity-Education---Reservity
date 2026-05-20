package dev.tmmc.reservity.messaging;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.tmmc.reservity.auth.dto.RegisterRequest;
import dev.tmmc.reservity.auth.repository.RefreshTokenRepository;
import dev.tmmc.reservity.events.repository.EventRepository;
import dev.tmmc.reservity.events.repository.EventRsvpRepository;
import dev.tmmc.reservity.messaging.repository.ConversationParticipantRepository;
import dev.tmmc.reservity.messaging.repository.ConversationRepository;
import dev.tmmc.reservity.messaging.repository.MessageRepository;
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
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ConversationControllerIntegrationTest {

    @Autowired private MockMvc mvc;
    @Autowired private SpaceTestFixtures fixtures;
    @Autowired private ReservationRequestRepository requestRepository;
    @Autowired private MessageRepository messageRepository;
    @Autowired private ConversationParticipantRepository participantRepository;
    @Autowired private ConversationRepository conversationRepository;
    @Autowired private EventRsvpRepository eventRsvpRepository;
    @Autowired private EventRepository eventRepository;
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
    private final ObjectMapper json = new ObjectMapper();

    @BeforeEach
    void cleanState() {
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
    void full_flow_create_send_list_read() throws Exception {
        Setup s = newSetup("ff");

        // Create or get conversation as the requester.
        MvcResult created = mvc.perform(post("/api/conversations")
                        .header("Authorization", "Bearer " + s.requesterToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"requestId\":\"" + s.requestId + "\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.requestId").value(s.requestId.toString()))
                .andReturn();
        UUID convId = UUID.fromString(json.readTree(
                created.getResponse().getContentAsByteArray()).get("id").asText());

        // Send a message.
        mvc.perform(post("/api/conversations/" + convId + "/messages")
                        .header("Authorization", "Bearer " + s.requesterToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\":\"Hello there\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.content").value("Hello there"))
                .andExpect(jsonPath("$.senderId").value(s.requesterId.toString()));

        // Owner lists messages — sees 1.
        mvc.perform(get("/api/conversations/" + convId + "/messages")
                        .header("Authorization", "Bearer " + s.ownerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1));

        // Owner has 1 unread.
        mvc.perform(get("/api/conversations/" + convId)
                        .header("Authorization", "Bearer " + s.ownerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.unreadCount").value(1));

        // Owner marks read; unread drops to 0.
        mvc.perform(patch("/api/conversations/" + convId + "/read")
                        .header("Authorization", "Bearer " + s.ownerToken))
                .andExpect(status().isNoContent());
        mvc.perform(get("/api/conversations/" + convId)
                        .header("Authorization", "Bearer " + s.ownerToken))
                .andExpect(jsonPath("$.unreadCount").value(0));

        // List all conversations for the owner — should include this one.
        mvc.perform(get("/api/conversations")
                        .header("Authorization", "Bearer " + s.ownerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void unauthenticated_returns_403() throws Exception {
        mvc.perform(get("/api/conversations"))
                .andExpect(status().isForbidden());
        mvc.perform(post("/api/conversations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"requestId\":\"" + UUID.randomUUID() + "\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void cross_tenant_access_returns_404() throws Exception {
        Setup s = newSetup("xt");
        // Stranger registers but is unrelated to the request.
        String strangerToken = registerAndGetToken("stranger-xt@test.local", "strxt");

        // Stranger tries to open the conversation — 403 from service.
        mvc.perform(post("/api/conversations")
                        .header("Authorization", "Bearer " + strangerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"requestId\":\"" + s.requestId + "\"}"))
                .andExpect(status().isForbidden());

        // Requester creates the conversation properly.
        MvcResult created = mvc.perform(post("/api/conversations")
                        .header("Authorization", "Bearer " + s.requesterToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"requestId\":\"" + s.requestId + "\"}"))
                .andExpect(status().isCreated())
                .andReturn();
        UUID convId = UUID.fromString(json.readTree(
                created.getResponse().getContentAsByteArray()).get("id").asText());

        // Stranger tries to GET it — 404 (cross-tenant masquerade).
        mvc.perform(get("/api/conversations/" + convId)
                        .header("Authorization", "Bearer " + strangerToken))
                .andExpect(status().isNotFound());
        mvc.perform(get("/api/conversations/" + convId + "/messages")
                        .header("Authorization", "Bearer " + strangerToken))
                .andExpect(status().isNotFound());
    }

    @Test
    void create_with_unknown_request_id_returns_404() throws Exception {
        String token = registerAndGetToken("unk-req@test.local", "unkreq");
        mvc.perform(post("/api/conversations")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"requestId\":\"" + UUID.randomUUID() + "\"}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void send_with_blank_content_returns_400() throws Exception {
        Setup s = newSetup("bl");
        MvcResult created = mvc.perform(post("/api/conversations")
                        .header("Authorization", "Bearer " + s.requesterToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"requestId\":\"" + s.requestId + "\"}"))
                .andExpect(status().isCreated())
                .andReturn();
        UUID convId = UUID.fromString(json.readTree(
                created.getResponse().getContentAsByteArray()).get("id").asText());

        mvc.perform(post("/api/conversations/" + convId + "/messages")
                        .header("Authorization", "Bearer " + s.requesterToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\":\"   \"}"))
                .andExpect(status().isBadRequest());

        mvc.perform(post("/api/conversations/" + convId + "/messages")
                        .header("Authorization", "Bearer " + s.requesterToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\":\"\"}"))
                .andExpect(status().isBadRequest());
    }

    // ────────── helpers ──────────

    private record Setup(UUID ownerId, UUID requesterId, UUID requestId,
                         String ownerToken, String requesterToken) {}

    private Setup newSetup(String prefix) throws Exception {
        String ownerToken = registerAndGetToken(prefix + "-owner@test.local", prefix + "owner");
        UUID ownerId = currentUserId(ownerToken);
        String requesterToken = registerAndGetToken(prefix + "-req@test.local", prefix + "req");
        UUID requesterId = currentUserId(requesterToken);

        User owner = userRepository.findById(ownerId).orElseThrow();
        User requester = userRepository.findById(requesterId).orElseThrow();

        Building b = fixtures.building("CtrlBldg-" + prefix, sanitizeShortCode(prefix + "x"));
        Space space = fixtures.space("ctrl-" + prefix, "Ctrl " + prefix, SpaceType.LAB, b, owner,
                BigDecimal.ZERO, (short) 4, BigDecimal.valueOf(15));
        space.setInstantBook(false);
        spaceRepository.save(space);

        Instant tomorrow = Instant.now().plus(1, ChronoUnit.DAYS).truncatedTo(ChronoUnit.SECONDS);
        ReservationRequest req = requestRepository.save(ReservationRequest.builder()
                .reservationCode("RV-CTRL" + prefix.toUpperCase() + "-1")
                .space(space)
                .requester(requester)
                .startsAt(tomorrow)
                .endsAt(tomorrow.plus(1, ChronoUnit.HOURS))
                .attendeesCount((short) 1)
                .tag(ReservationTag.SOLO_FOCUS)
                .status(ReservationStatus.PENDING)
                .autoApproved(false)
                .build());

        return new Setup(ownerId, requesterId, req.getId(), ownerToken, requesterToken);
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

    private static String sanitizeShortCode(String raw) {
        String upper = raw.replaceAll("[^A-Za-z0-9]", "").toUpperCase();
        if (upper.length() < 2) upper = "BX" + upper;
        if (upper.length() > 8) upper = upper.substring(0, 8);
        return upper;
    }
}
