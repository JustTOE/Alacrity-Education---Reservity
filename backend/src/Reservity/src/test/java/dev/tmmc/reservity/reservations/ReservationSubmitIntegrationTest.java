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
import dev.tmmc.reservity.spaces.entity.SpaceClosure;
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

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ReservationSubmitIntegrationTest {

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
    void requires_auth_to_submit() throws Exception {
        seedSpace();
        mvc.perform(post("/api/spaces/lab-4b/reservation-requests")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonBody(2)))
                .andExpect(status().isForbidden());
    }

    @Test
    void happy_path_returns_201_with_pass_token_and_RV_code() throws Exception {
        seedSpace();
        String token = registerAndGetToken("happy@test.local", "happyhandle");

        MvcResult result = mvc.perform(post("/api/spaces/lab-4b/reservation-requests")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonBody(2)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.status").value("APPROVED"))
                .andExpect(jsonPath("$.autoApproved").value(true))
                .andExpect(jsonPath("$.passToken").exists())
                .andExpect(jsonPath("$.recurring").value("none"))
                .andReturn();

        JsonNode body = json.readTree(result.getResponse().getContentAsByteArray());
        String code = body.get("id").asText();
        if (!code.matches("^RV-LAB4B-\\d{6}$")) {
            throw new AssertionError("Bad code format: " + code);
        }

        // Hour 14 should now be RESERVED on the availability endpoint.
        mvc.perform(get("/api/spaces/lab-4b/availability")
                        .param("from", LocalDate.now(ZoneOffset.UTC).plusDays(1).toString())
                        .param("to", LocalDate.now(ZoneOffset.UTC).plusDays(1).toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.days[0].hours[14].state").value("RESERVED"));
    }

    @Test
    void second_overlap_returns_409() throws Exception {
        seedSpace();
        String token = registerAndGetToken("overlap@test.local", "overlap");

        mvc.perform(post("/api/spaces/lab-4b/reservation-requests")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonBody(2)))
                .andExpect(status().isCreated());

        // Second booking that overlaps the first hour.
        Instant start = todayPlus(1, 14);
        Instant end = todayPlus(1, 15);
        mvc.perform(post("/api/spaces/lab-4b/reservation-requests")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(rangeBody(start, end)))
                .andExpect(status().isConflict());
    }

    @Test
    void window_longer_than_8_hours_is_400() throws Exception {
        seedSpace();
        String token = registerAndGetToken("longwin@test.local", "longwin");
        Instant start = todayPlus(1, 8);
        Instant end = start.plus(Duration.ofHours(9));
        mvc.perform(post("/api/spaces/lab-4b/reservation-requests")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(rangeBody(start, end)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void past_booking_is_400() throws Exception {
        seedSpace();
        String token = registerAndGetToken("past@test.local", "past");
        Instant start = Instant.now().minus(Duration.ofHours(1));
        Instant end = Instant.now().plus(Duration.ofMinutes(30));
        mvc.perform(post("/api/spaces/lab-4b/reservation-requests")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(rangeBody(start, end)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void weekly_recurring_creates_12_occurrences() throws Exception {
        seedSpace();
        String token = registerAndGetToken("week@test.local", "week");

        Instant start = todayPlus(1, 10);
        Instant end = todayPlus(1, 11);
        mvc.perform(post("/api/spaces/lab-4b/reservation-requests")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(rangeBodyRecurring(start, end, "weekly")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.recurring").value("weekly"))
                .andExpect(jsonPath("$.seriesId").exists())
                .andExpect(jsonPath("$.seriesOccurrences").value(12));

        // Verify 12 reservation_requests rows exist.
        if (requestRepository.count() != 12) {
            throw new AssertionError("Expected 12 requests, got " + requestRepository.count());
        }
    }

    @Test
    void closure_overlap_returns_409() throws Exception {
        Space space = seedSpace();
        String token = registerAndGetToken("closure@test.local", "closure");

        Instant clStart = todayPlus(1, 14);
        Instant clEnd = todayPlus(1, 16);
        SpaceClosure closure = SpaceClosure.builder()
                .space(space)
                .startsAt(clStart)
                .endsAt(clEnd)
                .reason("Maintenance")
                .createdBy(space.getOwnerId() != null
                        ? userRepository.findById(space.getOwnerId()).orElseThrow()
                        : null)
                .build();
        closureRepository.save(closure);

        mvc.perform(post("/api/spaces/lab-4b/reservation-requests")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(rangeBody(todayPlus(1, 13), todayPlus(1, 15))))
                .andExpect(status().isConflict());
    }

    @Test
    void default_tag_is_solo_focus() throws Exception {
        seedSpace();
        String token = registerAndGetToken("tag@test.local", "tag");

        MvcResult result = mvc.perform(post("/api/spaces/lab-4b/reservation-requests")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonBody(2)))
                .andExpect(status().isCreated())
                .andReturn();

        JsonNode body = json.readTree(result.getResponse().getContentAsByteArray());
        UUID requestId = UUID.fromString(body.get("requestId").asText());

        mvc.perform(get("/api/reservation-requests/" + requestId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tag").value("Solo focus"));
    }

    // ────────── helpers ──────────

    private Space seedSpace() {
        Building hsc = fixtures.building("Hawthorn Sciences", "HSC");
        User owner = fixtures.ownerUser("owner@test.local", "spaceowner");
        return fixtures.space("lab-4b", "Chemistry Lab 4B", SpaceType.LAB, hsc, owner,
                BigDecimal.valueOf(18), (short) 12, BigDecimal.valueOf(48));
    }

    private String registerAndGetToken(String email, String handle) throws Exception {
        RegisterRequest reg = new RegisterRequest(email, "secret-password-123", "Test " + handle, handle);
        MvcResult result = mvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsBytes(reg)))
                .andExpect(status().isCreated())
                .andReturn();
        JsonNode body = json.readTree(result.getResponse().getContentAsByteArray());
        return body.get("accessToken").asText();
    }

    /** Body covering hour {@code hourCount} hours starting at 14:00 tomorrow. */
    private byte[] jsonBody(int hourCount) throws Exception {
        Instant start = todayPlus(1, 14);
        Instant end = start.plus(Duration.ofHours(hourCount));
        return rangeBody(start, end);
    }

    private byte[] rangeBody(Instant start, Instant end) throws Exception {
        return ("{\"startsAt\":\"" + start + "\",\"endsAt\":\"" + end + "\"}").getBytes();
    }

    private byte[] rangeBodyRecurring(Instant start, Instant end, String recurring) throws Exception {
        return ("{\"startsAt\":\"" + start + "\",\"endsAt\":\"" + end + "\",\"recurring\":\"" + recurring + "\"}").getBytes();
    }

    private static Instant todayPlus(int days, int hour) {
        LocalDate target = LocalDate.now(ZoneOffset.UTC).plusDays(days);
        return LocalDateTime.of(target, java.time.LocalTime.of(hour, 0)).toInstant(ZoneOffset.UTC).truncatedTo(ChronoUnit.SECONDS);
    }
}
