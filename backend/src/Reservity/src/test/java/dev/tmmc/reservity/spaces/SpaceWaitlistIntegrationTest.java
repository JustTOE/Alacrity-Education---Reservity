package dev.tmmc.reservity.spaces;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import dev.tmmc.reservity.auth.dto.RegisterRequest;
import dev.tmmc.reservity.auth.repository.RefreshTokenRepository;
import dev.tmmc.reservity.reservations.repository.ReservationRepository;
import dev.tmmc.reservity.reservations.repository.ReservationRequestRepository;
import dev.tmmc.reservity.reservations.repository.ReservationSeriesRepository;
import dev.tmmc.reservity.reservations.repository.SavedPassRepository;
import dev.tmmc.reservity.spaces.dto.WaitlistRequest;
import dev.tmmc.reservity.spaces.entity.Building;
import dev.tmmc.reservity.spaces.entity.SpaceType;
import dev.tmmc.reservity.spaces.repository.BuildingRepository;
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
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SpaceWaitlistIntegrationTest {

    @Autowired private MockMvc mvc;
    @Autowired private SpaceTestFixtures fixtures;
    @Autowired private RefreshTokenRepository refreshTokenRepository;
    @Autowired private SavedPassRepository savedPassRepository;
    @Autowired private ReservationRepository reservationRepository;
    @Autowired private ReservationRequestRepository requestRepository;
    @Autowired private ReservationSeriesRepository seriesRepository;
    @Autowired private SpaceFavoriteRepository favoriteRepository;
    @Autowired private SpaceWaitlistRepository waitlistRepository;
    @Autowired private SpaceVibeRepository spaceVibeRepository;
    @Autowired private SpaceImageRepository spaceImageRepository;
    @Autowired private SpaceRepository spaceRepository;
    @Autowired private BuildingRepository buildingRepository;
    @Autowired private UserRepository userRepository;
    private final ObjectMapper json = new ObjectMapper().registerModule(new JavaTimeModule());

    @BeforeEach
    void cleanState() {
        savedPassRepository.deleteAll();
        reservationRepository.deleteAll();
        requestRepository.deleteAll();
        seriesRepository.deleteAll();
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
    void waitlist_lifecycle_create_list_delete() throws Exception {
        Building hsc = fixtures.building("Hawthorn Sciences", "HSC");
        User owner = fixtures.ownerUser("owner@test.local", "owner");
        fixtures.space("lab-2a", "Bio Lab 2A", SpaceType.LAB, hsc, owner,
                BigDecimal.valueOf(16), (short) 10, BigDecimal.valueOf(40));

        String token = registerAndExtractToken();

        // POST creates a waitlist row.
        Instant from = Instant.now().truncatedTo(ChronoUnit.HOURS).plus(1, ChronoUnit.DAYS);
        Instant to   = from.plus(2, ChronoUnit.HOURS);
        WaitlistRequest req = new WaitlistRequest(from, to);

        MvcResult created = mvc.perform(post("/api/spaces/lab-2a/waitlist")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsBytes(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.spaceSlug").value("lab-2a"))
                .andExpect(jsonPath("$.spaceName").value("Bio Lab 2A"))
                .andReturn();

        JsonNode body = json.readTree(created.getResponse().getContentAsByteArray());
        String entryId = body.get("id").asText();

        // GET /me/waitlist returns 1.
        mvc.perform(get("/api/users/me/waitlist")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));

        // DELETE removes it.
        mvc.perform(delete("/api/spaces/lab-2a/waitlist/" + entryId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNoContent());

        mvc.perform(get("/api/users/me/waitlist")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void waitlist_unauth_post_returns_403() throws Exception {
        Building hsc = fixtures.building("Hawthorn Sciences", "HSC");
        User owner = fixtures.ownerUser("owner@test.local", "owner");
        fixtures.space("lab-2a", "Bio Lab 2A", SpaceType.LAB, hsc, owner,
                BigDecimal.valueOf(16), (short) 10, BigDecimal.valueOf(40));

        mvc.perform(post("/api/spaces/lab-2a/waitlist")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isForbidden());
    }

    private String registerAndExtractToken() throws Exception {
        RegisterRequest reg = new RegisterRequest(
                "wl@test.local", "secret-password-123", "Waitlist Tester", "wl_tester"
        );
        MvcResult result = mvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsBytes(reg)))
                .andExpect(status().isCreated())
                .andReturn();

        JsonNode body = json.readTree(result.getResponse().getContentAsByteArray());
        return body.get("accessToken").asText();
    }
}
