package dev.tmmc.reservity.spaces;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import dev.tmmc.reservity.reservations.repository.ReservationRepository;
import dev.tmmc.reservity.reservations.repository.ReservationRequestRepository;
import dev.tmmc.reservity.reservations.repository.ReservationSeriesRepository;
import dev.tmmc.reservity.reservations.repository.SavedPassRepository;
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

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AvailabilityIntegrationTest {

    @Autowired private MockMvc mvc;
    @Autowired private SpaceTestFixtures fixtures;
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

    private static final ZoneId UTC = ZoneId.of("UTC");

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
        userRepository.deleteAll();
    }

    @Test
    void availability_alwaysOpen_returns_24_open_slots_per_day() throws Exception {
        Building b = fixtures.building("Hawthorn Sciences", "HSC");
        User owner = fixtures.ownerUser("owner1@test.local", "owner1");
        fixtures.space("lab-4b", "Chemistry Lab 4B", SpaceType.LAB, b, owner,
                BigDecimal.valueOf(18), (short) 12, BigDecimal.valueOf(48), "focus");

        mvc.perform(get("/api/spaces/lab-4b/availability")
                        .param("from", "2026-05-07")
                        .param("to", "2026-05-09"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.slug").value("lab-4b"))
                .andExpect(jsonPath("$.zone").value("UTC"))
                .andExpect(jsonPath("$.days.length()").value(3))
                .andExpect(jsonPath("$.days[0].date").value("2026-05-07"))
                .andExpect(jsonPath("$.days[0].hours.length()").value(24))
                .andExpect(jsonPath("$.days[0].hours[0].state").value("OPEN"))
                .andExpect(jsonPath("$.days[0].hours[12].state").value("OPEN"))
                .andExpect(jsonPath("$.days[0].closedAllDay").value(false))
                .andExpect(jsonPath("$.days[0].fullyBooked").value(false));
    }

    @Test
    void availability_reflects_a_closure_inserted_on_the_space() throws Exception {
        Building b = fixtures.building("Hawthorn Sciences", "HSC");
        User owner = fixtures.ownerUser("owner2@test.local", "owner2");
        Space space = fixtures.space("lab-4b", "Chemistry Lab 4B", SpaceType.LAB, b, owner,
                BigDecimal.valueOf(18), (short) 12, BigDecimal.valueOf(48), "focus");

        SpaceClosure closure = SpaceClosure.builder()
                .space(space)
                .startsAt(LocalDate.of(2026, 5, 8).atTime(14, 0).atZone(UTC).toInstant())
                .endsAt(LocalDate.of(2026, 5, 8).atTime(17, 0).atZone(UTC).toInstant())
                .reason("Maintenance")
                .createdBy(owner)
                .build();
        closureRepository.save(closure);

        mvc.perform(get("/api/spaces/lab-4b/availability")
                        .param("from", "2026-05-08")
                        .param("to", "2026-05-08"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.days[0].hours[13].state").value("OPEN"))
                .andExpect(jsonPath("$.days[0].hours[14].state").value("CLOSED"))
                .andExpect(jsonPath("$.days[0].hours[15].state").value("CLOSED"))
                .andExpect(jsonPath("$.days[0].hours[16].state").value("CLOSED"))
                .andExpect(jsonPath("$.days[0].hours[17].state").value("OPEN"));
    }

    @Test
    void availability_returns_400_when_from_is_after_to() throws Exception {
        Building b = fixtures.building("Hawthorn Sciences", "HSC");
        User owner = fixtures.ownerUser("owner3@test.local", "owner3");
        fixtures.space("lab-4b", "Chemistry Lab 4B", SpaceType.LAB, b, owner,
                BigDecimal.valueOf(18), (short) 12, BigDecimal.valueOf(48), "focus");

        mvc.perform(get("/api/spaces/lab-4b/availability")
                        .param("from", "2026-05-10")
                        .param("to", "2026-05-07"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void availability_returns_400_when_range_exceeds_60_days() throws Exception {
        Building b = fixtures.building("Hawthorn Sciences", "HSC");
        User owner = fixtures.ownerUser("owner4@test.local", "owner4");
        fixtures.space("lab-4b", "Chemistry Lab 4B", SpaceType.LAB, b, owner,
                BigDecimal.valueOf(18), (short) 12, BigDecimal.valueOf(48), "focus");

        mvc.perform(get("/api/spaces/lab-4b/availability")
                        .param("from", "2026-05-01")
                        .param("to", "2026-08-01"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void availability_returns_404_for_unknown_slug() throws Exception {
        mvc.perform(get("/api/spaces/does-not-exist/availability")
                        .param("from", "2026-05-07")
                        .param("to", "2026-05-08"))
                .andExpect(status().isNotFound());
    }

    @Test
    void availability_honors_weekly_operating_hours() throws Exception {
        Building b = fixtures.building("Hawthorn Sciences", "HSC");
        User owner = fixtures.ownerUser("owner5@test.local", "owner5");
        Space space = fixtures.space("lab-4b", "Chemistry Lab 4B", SpaceType.LAB, b, owner,
                BigDecimal.valueOf(18), (short) 12, BigDecimal.valueOf(48), "focus");

        // Mon-Fri 09-17, weekend closed.
        ObjectNode hours = json.createObjectNode();
        hours.put("mode", "weekly");
        ObjectNode schedule = hours.putObject("schedule");
        for (String day : new String[]{"MON", "TUE", "WED", "THU", "FRI"}) {
            ObjectNode w = json.createObjectNode().put("open", "09:00").put("close", "17:00");
            schedule.putArray(day).add(w);
        }
        space.setOperatingHours(hours);
        spaceRepository.saveAndFlush(space);

        // 2026-05-07 is Thursday → 09–16 OPEN.
        mvc.perform(get("/api/spaces/lab-4b/availability")
                        .param("from", "2026-05-07")
                        .param("to", "2026-05-07"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.days[0].hours[8].state").value("CLOSED"))
                .andExpect(jsonPath("$.days[0].hours[9].state").value("OPEN"))
                .andExpect(jsonPath("$.days[0].hours[16].state").value("OPEN"))
                .andExpect(jsonPath("$.days[0].hours[17].state").value("CLOSED"));

        // 2026-05-09 is Saturday → all CLOSED.
        mvc.perform(get("/api/spaces/lab-4b/availability")
                        .param("from", "2026-05-09")
                        .param("to", "2026-05-09"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.days[0].closedAllDay").value(true));
    }

    @Test
    void calendar_returns_single_day_payload() throws Exception {
        Building b = fixtures.building("Hawthorn Sciences", "HSC");
        User owner = fixtures.ownerUser("owner6@test.local", "owner6");
        fixtures.space("lab-4b", "Chemistry Lab 4B", SpaceType.LAB, b, owner,
                BigDecimal.valueOf(18), (short) 12, BigDecimal.valueOf(48), "focus");

        mvc.perform(get("/api/spaces/lab-4b/calendar").param("date", "2026-05-07"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.slug").value("lab-4b"))
                .andExpect(jsonPath("$.day.date").value("2026-05-07"))
                .andExpect(jsonPath("$.day.hours.length()").value(24));
    }

    @Test
    void batch_endpoint_returns_map_keyed_by_slug() throws Exception {
        Building b = fixtures.building("Hawthorn Sciences", "HSC");
        User owner = fixtures.ownerUser("owner7@test.local", "owner7");
        fixtures.space("lab-4b", "Chemistry Lab 4B", SpaceType.LAB, b, owner,
                BigDecimal.valueOf(18), (short) 12, BigDecimal.valueOf(48), "focus");
        fixtures.space("atrium", "The Glass Atrium", SpaceType.OPEN, b, owner,
                BigDecimal.ZERO, (short) 24, BigDecimal.valueOf(120), "loud");

        String body = """
            {"slugs":["lab-4b","atrium"],"date":"2026-05-07"}
            """;
        mvc.perform(post("/api/spaces/availability:batch")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.date").value("2026-05-07"))
                .andExpect(jsonPath("$.spaces.lab-4b.hours.length()").value(24))
                .andExpect(jsonPath("$.spaces.atrium.hours.length()").value(24));
    }

    @Test
    void batch_endpoint_rejects_when_both_ids_and_slugs_supplied() throws Exception {
        String body = """
            {"slugs":["lab-4b"],"spaceIds":["00000000-0000-0000-0000-000000000001"],"date":"2026-05-07"}
            """;
        mvc.perform(post("/api/spaces/availability:batch")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    void batch_endpoint_rejects_more_than_50_entries() throws Exception {
        ObjectNode body = json.createObjectNode();
        body.put("date", "2026-05-07");
        var slugs = body.putArray("slugs");
        for (int i = 0; i < 51; i++) slugs.add("slug-" + i);

        mvc.perform(post("/api/spaces/availability:batch")
                        .contentType(MediaType.APPLICATION_JSON).content(body.toString()))
                .andExpect(status().isBadRequest());
    }

    @Test
    void availability_endpoints_are_accessible_without_auth() throws Exception {
        Building b = fixtures.building("Hawthorn Sciences", "HSC");
        User owner = fixtures.ownerUser("owner8@test.local", "owner8");
        fixtures.space("lab-4b", "Chemistry Lab 4B", SpaceType.LAB, b, owner,
                BigDecimal.valueOf(18), (short) 12, BigDecimal.valueOf(48), "focus");

        // No Authorization header on any of the three endpoints.
        mvc.perform(get("/api/spaces/lab-4b/availability")
                        .param("from", "2026-05-07").param("to", "2026-05-07"))
                .andExpect(status().isOk());
        mvc.perform(get("/api/spaces/lab-4b/calendar").param("date", "2026-05-07"))
                .andExpect(status().isOk());
        mvc.perform(post("/api/spaces/availability:batch")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"slugs\":[\"lab-4b\"],\"date\":\"2026-05-07\"}"))
                .andExpect(status().isOk());
    }

    @Test
    void availability_resolves_by_uuid_too() throws Exception {
        Building b = fixtures.building("Hawthorn Sciences", "HSC");
        User owner = fixtures.ownerUser("owner9@test.local", "owner9");
        Space space = fixtures.space("lab-4b", "Chemistry Lab 4B", SpaceType.LAB, b, owner,
                BigDecimal.valueOf(18), (short) 12, BigDecimal.valueOf(48), "focus");

        UUID id = space.getId();
        mvc.perform(get("/api/spaces/" + id + "/availability")
                        .param("from", "2026-05-07").param("to", "2026-05-07"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.spaceId").value(id.toString()));
    }
}
