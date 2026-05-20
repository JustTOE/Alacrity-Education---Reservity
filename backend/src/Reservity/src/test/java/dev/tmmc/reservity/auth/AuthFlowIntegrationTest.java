package dev.tmmc.reservity.auth;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.tmmc.reservity.auth.dto.LoginRequest;
import dev.tmmc.reservity.auth.dto.RefreshRequest;
import dev.tmmc.reservity.auth.dto.RegisterRequest;
import dev.tmmc.reservity.auth.repository.RefreshTokenRepository;
import dev.tmmc.reservity.notifications.repository.NotificationRepository;
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
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthFlowIntegrationTest {

    @Autowired private MockMvc mvc;
    @Autowired private UserRepository userRepository;
    @Autowired private RefreshTokenRepository refreshTokenRepository;
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
    @Autowired private dev.tmmc.reservity.events.repository.EventRsvpRepository eventRsvpRepository;
    @Autowired private dev.tmmc.reservity.events.repository.EventRepository eventRepository;
    private final ObjectMapper json = new ObjectMapper();

    @BeforeEach
    void cleanState() {
        // Reverse-FK order — leakage from other tests blocks user delete
        // because reservations.user_id has ON DELETE RESTRICT.
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
    void register_login_refresh_me_logout_flow_works_end_to_end() throws Exception {
        // ----- Register -----
        RegisterRequest reg = new RegisterRequest(
                "maya@university.edu", "secret-password-123", "Maya Reyes", "maya_r"
        );
        MvcResult registered = mvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsBytes(reg)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.refreshToken").isNotEmpty())
                .andExpect(jsonPath("$.user.email").value("maya@university.edu"))
                .andExpect(jsonPath("$.user.handle").value("maya_r"))
                .andExpect(jsonPath("$.user.displayName").value("Maya Reyes"))
                .andExpect(jsonPath("$.user.initials").value("MR"))
                .andExpect(jsonPath("$.user.accountType").value("REQUESTER"))
                .andReturn();

        JsonNode body = json.readTree(registered.getResponse().getContentAsByteArray());
        String accessToken = body.get("accessToken").asText();
        String refreshToken = body.get("refreshToken").asText();

        // ----- /me requires bearer -----
        // Spring Security 4 returns 403 (not 401) by default when there's no auth.
        // The semantic distinction is that we never *prompt* for a credential — JWT is presented or it isn't.
        mvc.perform(get("/api/auth/me"))
                .andExpect(status().isForbidden());

        mvc.perform(get("/api/auth/me").header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("maya@university.edu"));

        // ----- Login again -----
        LoginRequest login = new LoginRequest("maya@university.edu", "secret-password-123");
        mvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsBytes(login)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.user.email").value("maya@university.edu"));

        // ----- Wrong password -----
        LoginRequest bad = new LoginRequest("maya@university.edu", "wrong-password");
        mvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsBytes(bad)))
                .andExpect(status().isUnauthorized());

        // ----- Refresh rotates tokens -----
        RefreshRequest refreshReq = new RefreshRequest(refreshToken);
        MvcResult refreshed = mvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsBytes(refreshReq)))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode refreshBody = json.readTree(refreshed.getResponse().getContentAsByteArray());
        String newAccess = refreshBody.get("accessToken").asText();
        String newRefresh = refreshBody.get("refreshToken").asText();
        assertThat(newRefresh).isNotEqualTo(refreshToken);

        // The OLD refresh token must now be rejected (and trigger reuse-detection).
        mvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsBytes(refreshReq)))
                .andExpect(status().isUnauthorized());

        // After reuse detection, the *new* refresh should also be revoked (defense in depth).
        mvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsBytes(new RefreshRequest(newRefresh))))
                .andExpect(status().isUnauthorized());

        // The new access token still works until it expires.
        mvc.perform(get("/api/auth/me").header("Authorization", "Bearer " + newAccess))
                .andExpect(status().isOk());
    }

    @Test
    void register_with_duplicate_email_returns_409() throws Exception {
        RegisterRequest reg = new RegisterRequest(
                "dup@university.edu", "secret-password-123", "First User", "first_user"
        );
        mvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsBytes(reg)))
                .andExpect(status().isCreated());

        RegisterRequest dup = new RegisterRequest(
                "dup@university.edu", "another-pass-456", "Second User", "second_user"
        );
        mvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsBytes(dup)))
                .andExpect(status().isConflict());
    }

    @Test
    void register_with_invalid_handle_returns_400() throws Exception {
        RegisterRequest reg = new RegisterRequest(
                "bad@university.edu", "secret-password-123", "Bad Handle", "Has SPACES"
        );
        mvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsBytes(reg)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void unauthenticated_protected_endpoint_returns_403() throws Exception {
        // Spring Security 4 defaults to 403 (Access Denied) when no credentials are presented
        // on a stateless API. We could customize the AuthenticationEntryPoint to emit 401 if we
        // wanted to match the OAuth-style "challenge" semantics, but for the current API style
        // (bearer-or-nothing, no realm prompts) 403 is fine.
        mvc.perform(get("/api/users/me"))
                .andExpect(status().isForbidden());
    }
}
