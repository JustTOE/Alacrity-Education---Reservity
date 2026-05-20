package dev.tmmc.reservity.spaces;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.tmmc.reservity.spaces.entity.*;
import dev.tmmc.reservity.spaces.repository.*;
import dev.tmmc.reservity.user.entity.User;
import dev.tmmc.reservity.user.repository.UserRepository;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Shared helpers for spaces-domain integration tests. Each test resets state
 * in {@code @BeforeEach}, then calls these factories to populate exactly the
 * fixtures it needs.
 */
@Component
public class SpaceTestFixtures {

    private final BuildingRepository buildingRepository;
    private final SpaceRepository spaceRepository;
    private final SpaceVibeRepository spaceVibeRepository;
    private final VibeRepository vibeRepository;
    private final UserRepository userRepository;
    private final ObjectMapper json;

    public SpaceTestFixtures(BuildingRepository buildingRepository,
                             SpaceRepository spaceRepository,
                             SpaceVibeRepository spaceVibeRepository,
                             VibeRepository vibeRepository,
                             UserRepository userRepository) {
        this.buildingRepository = buildingRepository;
        this.spaceRepository = spaceRepository;
        this.spaceVibeRepository = spaceVibeRepository;
        this.vibeRepository = vibeRepository;
        this.userRepository = userRepository;
        this.json = new ObjectMapper();
    }

    public Building building(String name, String code) {
        return buildingRepository.save(Building.builder()
                .name(name)
                .shortCode(code)
                .campusName("Main Campus")
                .pinX(BigDecimal.valueOf(0.5))
                .pinY(BigDecimal.valueOf(0.5))
                .build());
    }

    /** Default-bcrypt placeholder is fine — tests don't authenticate this user. */
    public User ownerUser(String email, String handle) {
        return userRepository.save(User.builder()
                .email(email)
                .passwordHash("$2a$12$0000000000000000000000000000000000000000000000000000")
                .handle(handle)
                .displayName("Test " + handle)
                .initials("T" + handle.toUpperCase().charAt(0))
                .memberSince(LocalDate.now())
                .build());
    }

    public Space space(String slug, String name, SpaceType type, Building building, User owner,
                       BigDecimal price, short seats, BigDecimal area, String... vibeIds) {
        JsonNode emptyArr = json.createArrayNode();
        JsonNode hours = json.createObjectNode().put("mode", "24/7");

        Space space = spaceRepository.save(Space.builder()
                .slug(slug)
                .name(name)
                .type(type)
                .ownerId(owner.getId())
                .ownerType(SpaceOwnerType.USER)
                .building(building)
                .floor((short) 1)
                .room("1")
                .seats(seats)
                .areaSqm(area)
                .pricePerHour(price)
                .currency("USD")
                .blurb(name + " — test fixture.")
                .description("Test fixture for " + name)
                .amenities(emptyArr)
                .rules(emptyArr)
                .operatingHours(hours)
                .pinX(BigDecimal.valueOf(0.5))
                .pinY(BigDecimal.valueOf(0.5))
                .status(SpaceStatus.PUBLISHED)
                .instantBook(true)
                .build());

        for (String vibeId : vibeIds) {
            Vibe vibe = vibeRepository.findById(vibeId).orElseThrow();
            SpaceVibeId id = new SpaceVibeId(space.getId(), vibe.getId());
            spaceVibeRepository.save(SpaceVibe.builder()
                    .id(id)
                    .space(space)
                    .vibe(vibe)
                    .build());
        }
        return space;
    }
}
