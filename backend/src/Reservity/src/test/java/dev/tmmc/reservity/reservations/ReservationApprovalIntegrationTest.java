package dev.tmmc.reservity.reservations;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.tmmc.reservity.auth.dto.RegisterRequest;
import dev.tmmc.reservity.auth.repository.RefreshTokenRepository;
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
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Approval / deny / cancel state-machine end-to-end. Uses an
 * {@code instant_book = false} space so the request stays PENDING until the
 * owner approves explicitly.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ReservationApprovalIntegrationTest {

    @Autowired private MockMvc mvc;
    @Autowired private SpaceTestFixtures fixtures;
    @Autowired private RefreshTokenRepository refreshTokenRepository;
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
    private final ObjectMapper json = new ObjectMapper();

    @BeforeEach
    void cleanState() {
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
    void approval_state_machine_end_to_end() throws Exception {
        // Owner is the registered user; space is non-instant.
        String ownerToken = registerAndGetToken("owner@test.local", "ownerhandle");
        UUID ownerId = currentUserId(ownerToken);
        Space space = seedNonInstantSpace(ownerId);

        // Different requester.
        String requesterToken = registerAndGetToken("req@test.local", "reqhandle");

        // Submit -> PENDING (no pass token).
        MvcResult submit = mvc.perform(post("/api/spaces/lab-4b/reservation-requests")
                        .header("Authorization", "Bearer " + requesterToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(rangeBody(todayPlus(1, 10), todayPlus(1, 11))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.passToken").doesNotExist())
                .andReturn();

        UUID requestId = UUID.fromString(json.readTree(submit.getResponse().getContentAsByteArray())
                .get("requestId").asText());

        // Non-owner approve -> 403.
        mvc.perform(post("/api/reservation-requests/" + requestId + "/approve")
                        .header("Authorization", "Bearer " + requesterToken))
                .andExpect(status().isForbidden());

        // Owner approve -> 200 + APPROVED + reservation row exists.
        mvc.perform(post("/api/reservation-requests/" + requestId + "/approve")
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("APPROVED"));

        if (reservationRepository.count() != 1) {
            throw new AssertionError("Expected 1 reservation, got " + reservationRepository.count());
        }

        // Re-approve -> 409 (state-machine reject).
        mvc.perform(post("/api/reservation-requests/" + requestId + "/approve")
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isConflict());

        // Hour 10 is now RESERVED.
        mvc.perform(get("/api/spaces/lab-4b/availability")
                        .param("from", LocalDate.now(ZoneOffset.UTC).plusDays(1).toString())
                        .param("to", LocalDate.now(ZoneOffset.UTC).plusDays(1).toString()))
                .andExpect(jsonPath("$.days[0].hours[10].state").value("RESERVED"));

        // Requester cancels the APPROVED booking.
        mvc.perform(post("/api/reservation-requests/" + requestId + "/cancel")
                        .header("Authorization", "Bearer " + requesterToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\":\"Plans changed\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));

        // Reservation row removed -> hour 10 back to OPEN.
        if (reservationRepository.count() != 0) {
            throw new AssertionError("Expected 0 reservations after cancel, got " + reservationRepository.count());
        }
        mvc.perform(get("/api/spaces/lab-4b/availability")
                        .param("from", LocalDate.now(ZoneOffset.UTC).plusDays(1).toString())
                        .param("to", LocalDate.now(ZoneOffset.UTC).plusDays(1).toString()))
                .andExpect(jsonPath("$.days[0].hours[10].state").value("OPEN"));
    }

    @Test
    void deny_marks_request_DENIED_with_reason_and_no_reservation() throws Exception {
        String ownerToken = registerAndGetToken("denyowner@test.local", "denyowner");
        UUID ownerId = currentUserId(ownerToken);
        seedNonInstantSpace(ownerId);
        String requesterToken = registerAndGetToken("denyreq@test.local", "denyreq");

        UUID requestId = submitForId(requesterToken,
                rangeBody(todayPlus(1, 11), todayPlus(1, 12)));

        mvc.perform(post("/api/reservation-requests/" + requestId + "/deny")
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\":\"Lab is reserved for graduate students only\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DENIED"))
                .andExpect(jsonPath("$.rejectionReason").value("Lab is reserved for graduate students only"));

        if (reservationRepository.count() != 0) {
            throw new AssertionError("Deny must not create a reservation row");
        }
    }

    @Test
    void cancel_pending_does_not_touch_reservations_table() throws Exception {
        String ownerToken = registerAndGetToken("cancelown@test.local", "cancelown");
        UUID ownerId = currentUserId(ownerToken);
        seedNonInstantSpace(ownerId);
        String requesterToken = registerAndGetToken("cancelreq@test.local", "cancelreq");

        UUID requestId = submitForId(requesterToken,
                rangeBody(todayPlus(1, 12), todayPlus(1, 13)));

        mvc.perform(post("/api/reservation-requests/" + requestId + "/cancel")
                        .header("Authorization", "Bearer " + requesterToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));

        if (reservationRepository.count() != 0) {
            throw new AssertionError("Cancel of PENDING should not touch reservations");
        }
    }

    // ────────── helpers ──────────

    private Space seedNonInstantSpace(UUID ownerId) {
        Building hsc = fixtures.building("Hawthorn Sciences", "HSC");
        // Use ownerId of the just-registered user so that user IS the space owner.
        User owner = userRepository.findById(ownerId).orElseThrow();
        Space s = fixtures.space("lab-4b", "Chemistry Lab 4B", SpaceType.LAB, hsc, owner,
                BigDecimal.valueOf(18), (short) 12, BigDecimal.valueOf(48));
        s.setInstantBook(false);
        return spaceRepository.save(s);
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

    private UUID submitForId(String token, byte[] body) throws Exception {
        MvcResult res = mvc.perform(post("/api/spaces/lab-4b/reservation-requests")
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
        return LocalDateTime.of(target, java.time.LocalTime.of(hour, 0)).toInstant(ZoneOffset.UTC).truncatedTo(ChronoUnit.SECONDS);
    }
}
