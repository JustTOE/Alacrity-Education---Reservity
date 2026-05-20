package dev.tmmc.reservity.events;

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
import dev.tmmc.reservity.organization.repository.OrgMembershipRepository;
import dev.tmmc.reservity.organization.repository.OrganizationRepository;
import dev.tmmc.reservity.reservations.repository.ReservationRepository;
import dev.tmmc.reservity.reservations.repository.ReservationRequestRepository;
import dev.tmmc.reservity.reservations.repository.ReservationSeriesRepository;
import dev.tmmc.reservity.reservations.repository.SavedPassRepository;
import dev.tmmc.reservity.spaces.SpaceTestFixtures;
import dev.tmmc.reservity.spaces.repository.*;
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

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class EventControllerIntegrationTest {

    @Autowired private MockMvc mvc;
    @Autowired private SpaceTestFixtures fixtures;
    @Autowired private EventRepository eventRepository;
    @Autowired private EventRsvpRepository eventRsvpRepository;
    @Autowired private OrganizationRepository organizationRepository;
    @Autowired private OrgMembershipRepository orgMembershipRepository;
    @Autowired private MessageRepository messageRepository;
    @Autowired private ConversationParticipantRepository participantRepository;
    @Autowired private ConversationRepository conversationRepository;
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
    @Autowired private UserRepository userRepository;
    @Autowired private RefreshTokenRepository refreshTokenRepository;
    private final ObjectMapper json = new ObjectMapper();

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
    void anonymous_can_list_PUBLIC_events() throws Exception {
        String hostToken = registerAndGetToken("ev-anon-host@test.local", "evanonh");
        UUID hostId = currentUserId(hostToken);
        createEvent(hostToken, hostId, "Public happy", null);

        mvc.perform(get("/api/events"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void create_with_mismatched_USER_host_returns_403() throws Exception {
        String aToken = registerAndGetToken("ev-mm-a@test.local", "evmma");
        String bToken = registerAndGetToken("ev-mm-b@test.local", "evmmb");
        UUID bId = currentUserId(bToken);

        Instant start = Instant.now().plus(Duration.ofDays(1));
        Instant end = start.plus(Duration.ofHours(1));
        String body = """
                {
                  "title": "Sneaky", "blurb": "blurb",
                  "startsAt": "%s", "endsAt": "%s",
                  "hostType": "USER", "hostId": "%s",
                  "category": "OTHER", "capacity": 0, "visibility": "PUBLIC"
                }""".formatted(start, end, bId);
        mvc.perform(post("/api/events")
                        .header("Authorization", "Bearer " + aToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isForbidden());
    }

    @Test
    void rsvp_at_capacity_lands_WAITLIST() throws Exception {
        String hostToken = registerAndGetToken("ev-wl-host@test.local", "evwlh");
        UUID hostId = currentUserId(hostToken);
        UUID eventId = createEvent(hostToken, hostId, "Cap1", 1);

        String aToken = registerAndGetToken("ev-wl-a@test.local", "evwla");
        String bToken = registerAndGetToken("ev-wl-b@test.local", "evwlb");

        mvc.perform(post("/api/events/" + eventId + "/rsvp")
                        .header("Authorization", "Bearer " + aToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"GOING\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("GOING"));

        mvc.perform(post("/api/events/" + eventId + "/rsvp")
                        .header("Authorization", "Bearer " + bToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"GOING\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("WAITLIST"));
    }

    @Test
    void delete_rsvp_promotes_oldest_waitlist() throws Exception {
        String hostToken = registerAndGetToken("ev-dwl-host@test.local", "evdwlh");
        UUID hostId = currentUserId(hostToken);
        UUID eventId = createEvent(hostToken, hostId, "Cap1Promote", 1);

        String aToken = registerAndGetToken("ev-dwl-a@test.local", "evdwla");
        String bToken = registerAndGetToken("ev-dwl-b@test.local", "evdwlb");

        mvc.perform(post("/api/events/" + eventId + "/rsvp")
                        .header("Authorization", "Bearer " + aToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"GOING\"}"))
                .andExpect(status().isCreated());
        mvc.perform(post("/api/events/" + eventId + "/rsvp")
                        .header("Authorization", "Bearer " + bToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"GOING\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("WAITLIST"));

        // A drops; B should auto-promote.
        mvc.perform(delete("/api/events/" + eventId + "/rsvp")
                        .header("Authorization", "Bearer " + aToken))
                .andExpect(status().isNoContent());

        // Verify via attendees endpoint.
        mvc.perform(get("/api/events/" + eventId + "/attendees?status=GOING"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void cancel_event_by_non_host_returns_403() throws Exception {
        String hostToken = registerAndGetToken("ev-can-host@test.local", "evcanh");
        UUID hostId = currentUserId(hostToken);
        UUID eventId = createEvent(hostToken, hostId, "LockedToCancel", null);

        String strangerToken = registerAndGetToken("ev-can-str@test.local", "evcans");
        mvc.perform(post("/api/events/" + eventId + "/cancel")
                        .header("Authorization", "Bearer " + strangerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\":\"trolling\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void unauthenticated_create_returns_403() throws Exception {
        Instant start = Instant.now().plus(Duration.ofDays(1));
        String body = """
                {
                  "title": "Anon", "blurb": "blurb",
                  "startsAt": "%s", "endsAt": "%s",
                  "hostType": "USER", "hostId": "%s",
                  "category": "OTHER", "capacity": 0, "visibility": "PUBLIC"
                }""".formatted(start, start.plus(Duration.ofHours(1)), UUID.randomUUID());
        mvc.perform(post("/api/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isForbidden());
    }

    @Test
    void getBySlug_returns_full_payload() throws Exception {
        String hostToken = registerAndGetToken("ev-gs-host@test.local", "evgsh");
        UUID hostId = currentUserId(hostToken);
        UUID eventId = createEvent(hostToken, hostId, "Findable by slug", null);

        // Get by id first to read the slug.
        MvcResult byId = mvc.perform(get("/api/events/" + eventId))
                .andExpect(status().isOk())
                .andReturn();
        String slug = json.readTree(byId.getResponse().getContentAsByteArray()).get("slug").asText();

        mvc.perform(get("/api/events/" + slug))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(eventId.toString()))
                .andExpect(jsonPath("$.slug").value(slug));
    }

    // ────────── helpers ──────────

    /** Creates an event via REST. capacity=null means 0 (unlimited). */
    private UUID createEvent(String token, UUID hostId, String title, Integer capacity) throws Exception {
        Instant start = Instant.now().plus(Duration.ofDays(1));
        Instant end = start.plus(Duration.ofHours(2));
        String body = """
                {
                  "title": "%s", "blurb": "blurb for %s",
                  "startsAt": "%s", "endsAt": "%s",
                  "hostType": "USER", "hostId": "%s",
                  "category": "PERFORMANCE", "tag": "Open",
                  "capacity": %d, "visibility": "PUBLIC"
                }""".formatted(title, title, start, end, hostId,
                capacity == null ? 0 : capacity);
        MvcResult res = mvc.perform(post("/api/events")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andReturn();
        JsonNode root = json.readTree(res.getResponse().getContentAsByteArray());
        return UUID.fromString(root.get("id").asText());
    }

    private String registerAndGetToken(String email, String handle) throws Exception {
        RegisterRequest reg = new RegisterRequest(email, "secret-password-123",
                "Test " + handle, handle);
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
