package dev.tmmc.reservity.spaces;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.tmmc.reservity.reservations.repository.ReservationRepository;
import dev.tmmc.reservity.reservations.repository.ReservationRequestRepository;
import dev.tmmc.reservity.reservations.repository.ReservationSeriesRepository;
import dev.tmmc.reservity.reservations.repository.SavedPassRepository;
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
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SpaceCatalogIntegrationTest {

    @Autowired private MockMvc mvc;
    @Autowired private SpaceTestFixtures fixtures;
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
    private final ObjectMapper json = new ObjectMapper();

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
        userRepository.deleteAll();
    }

    @Test
    void vibes_endpoint_returns_seeded_vibes() throws Exception {
        // R__seed_vibes is a repeatable migration baked into the test DB —
        // 8 canonical vibes should always be present.
        mvc.perform(get("/api/vibes"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(8))
                .andExpect(jsonPath("$[0].id").exists())
                .andExpect(jsonPath("$[?(@.id == 'focus')].label").value("Focus mode"))
                .andExpect(jsonPath("$[?(@.id == 'lab-only')].icon").value("⌬"));
    }

    @Test
    void buildings_endpoint_returns_what_was_seeded() throws Exception {
        Building hsc = fixtures.building("Hawthorn Sciences", "HSC");
        Building lhl = fixtures.building("Linden Hall", "LHL");
        fixtures.building("Magnolia Arts", "MGA");

        mvc.perform(get("/api/buildings"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(3));

        mvc.perform(get("/api/buildings/" + hsc.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Hawthorn Sciences"))
                .andExpect(jsonPath("$.shortCode").value("HSC"));

        // Short code lookup also resolves.
        mvc.perform(get("/api/buildings/LHL"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(lhl.getId().toString()));
    }

    @Test
    void spaces_endpoint_lists_filters_and_serves_detail() throws Exception {
        Building hsc = fixtures.building("Hawthorn Sciences", "HSC");
        Building lhl = fixtures.building("Linden Hall", "LHL");
        User owner = fixtures.ownerUser("owner@test.local", "owner");

        fixtures.space("lab-4b", "Chemistry Lab 4B", SpaceType.LAB, hsc, owner,
                BigDecimal.valueOf(18), (short) 12, BigDecimal.valueOf(48),
                "focus", "group", "natural-light", "lab-only");
        fixtures.space("pod-3", "Coding Pod 3", SpaceType.POD, lhl, owner,
                BigDecimal.valueOf(4), (short) 1, BigDecimal.valueOf(6),
                "focus", "solo", "after-hours");
        fixtures.space("atrium", "The Glass Atrium", SpaceType.OPEN, hsc, owner,
                BigDecimal.ZERO, (short) 24, BigDecimal.valueOf(120),
                "loud", "group");

        // Unfiltered list returns all 3.
        mvc.perform(get("/api/spaces"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(3))
                .andExpect(jsonPath("$.content.length()").value(3));

        // type=lab filters to one.
        mvc.perform(get("/api/spaces").param("type", "lab"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].id").value("lab-4b"));

        // isFree=true filters to atrium.
        mvc.perform(get("/api/spaces").param("isFree", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].id").value("atrium"))
                .andExpect(jsonPath("$.content[0].currency").value("free"));

        // vibes=focus,group matches lab-4b (has both) and atrium (has group).
        // Default semantics is any-match, so both should appear.
        mvc.perform(get("/api/spaces").param("vibes", "focus,group"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(3));

        // vibes=lab-only matches only lab-4b.
        mvc.perform(get("/api/spaces").param("vibes", "lab-only"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].id").value("lab-4b"));

        // Detail view by slug returns full SpaceResponse with vibes + building name.
        mvc.perform(get("/api/spaces/lab-4b"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("lab-4b"))
                .andExpect(jsonPath("$.name").value("Chemistry Lab 4B"))
                .andExpect(jsonPath("$.type").value("lab"))
                .andExpect(jsonPath("$.building").value("Hawthorn Sciences"))
                .andExpect(jsonPath("$.vibes.length()").value(4))
                .andExpect(jsonPath("$.pin.x").exists())
                .andExpect(jsonPath("$.pin.y").exists())
                .andExpect(jsonPath("$.amenities").isArray())
                .andExpect(jsonPath("$.rules").isArray())
                .andExpect(jsonPath("$.booked").isArray())
                .andExpect(jsonPath("$.events").isArray());

        // Detail by UUID also works.
        var lab = spaceRepository.findBySlug("lab-4b").orElseThrow();
        mvc.perform(get("/api/spaces/" + lab.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("lab-4b"));

        // Unknown slug -> 404.
        mvc.perform(get("/api/spaces/does-not-exist"))
                .andExpect(status().isNotFound());
    }

    @Test
    void unfiltered_spaces_list_excludes_unpublished() throws Exception {
        Building b = fixtures.building("Hawthorn Sciences", "HSC");
        User owner = fixtures.ownerUser("owner2@test.local", "owner2");
        var published = fixtures.space("published-space", "Published", SpaceType.LAB, b, owner,
                BigDecimal.valueOf(10), (short) 5, BigDecimal.valueOf(20));

        // Manually demote a second space to DRAFT and verify it doesn't show.
        var draft = fixtures.space("draft-space", "Draft", SpaceType.LAB, b, owner,
                BigDecimal.valueOf(10), (short) 5, BigDecimal.valueOf(20));
        draft.setStatus(dev.tmmc.reservity.spaces.entity.SpaceStatus.DRAFT);
        spaceRepository.save(draft);

        mvc.perform(get("/api/spaces"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].id").value("published-space"));
    }
}
