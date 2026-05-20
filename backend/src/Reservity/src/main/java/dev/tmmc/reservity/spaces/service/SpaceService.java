package dev.tmmc.reservity.spaces.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.tmmc.reservity.common.exception.EntityNotFoundException;
import dev.tmmc.reservity.common.pagination.PageResponse;
import dev.tmmc.reservity.organization.entity.OrgMembership;
import dev.tmmc.reservity.organization.repository.OrgMembershipRepository;
import dev.tmmc.reservity.spaces.dto.SpaceCreateRequest;
import dev.tmmc.reservity.spaces.dto.SpaceFilterParams;
import dev.tmmc.reservity.spaces.dto.SpaceResponse;
import dev.tmmc.reservity.spaces.dto.SpaceSummary;
import dev.tmmc.reservity.spaces.dto.SpaceUpdateRequest;
import dev.tmmc.reservity.spaces.entity.Building;
import dev.tmmc.reservity.spaces.entity.Space;
import dev.tmmc.reservity.spaces.entity.SpaceImage;
import dev.tmmc.reservity.spaces.entity.SpaceOwnerType;
import dev.tmmc.reservity.spaces.entity.SpaceStatus;
import dev.tmmc.reservity.spaces.entity.SpaceVibe;
import dev.tmmc.reservity.spaces.entity.SpaceVibeId;
import dev.tmmc.reservity.spaces.entity.Vibe;
import dev.tmmc.reservity.spaces.mapper.SpaceMapper;
import dev.tmmc.reservity.spaces.repository.BuildingRepository;
import dev.tmmc.reservity.spaces.repository.SpaceImageRepository;
import dev.tmmc.reservity.spaces.repository.SpaceRepository;
import dev.tmmc.reservity.spaces.repository.VibeRepository;
import dev.tmmc.reservity.user.entity.User;
import dev.tmmc.reservity.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SpaceService {

    private final SpaceRepository spaceRepository;
    private final SpaceImageRepository spaceImageRepository;
    private final BuildingRepository buildingRepository;
    private final VibeRepository vibeRepository;
    private final UserRepository userRepository;
    private final SpaceMapper spaceMapper;
    private final OrgMembershipRepository orgMembershipRepository;
    private final ObjectMapper objectMapper;

    public PageResponse<SpaceSummary> search(SpaceFilterParams f, int page, int size, String sortKey) {
        Specification<Space> spec = SpaceQuerySpec.all(
                SpaceQuerySpec.published(),
                SpaceQuerySpec.byTextLike(f.q()),
                SpaceQuerySpec.byType(f.type()),
                SpaceQuerySpec.byMinSeats(f.minSeats()),
                SpaceQuerySpec.byMaxPrice(f.maxPrice()),
                SpaceQuerySpec.freeOnly(f.isFree()),
                SpaceQuerySpec.byVibes(f.vibes()),
                SpaceQuerySpec.byBuilding(f.buildingId())
        );

        Pageable pageable = PageRequest.of(
                Math.max(page, 0),
                Math.min(Math.max(size, 1), 100),
                resolveSort(sortKey)
        );

        Page<Space> rows = spaceRepository.findAll(spec, pageable);
        Map<UUID, String> primaryByspace = primaryImageUrls(rows.getContent());
        Page<SpaceSummary> mapped = rows.map(s -> spaceMapper.toSummary(s, primaryByspace.get(s.getId())));
        return PageResponse.of(mapped);
    }

    private Map<UUID, String> primaryImageUrls(List<Space> spaces) {
        if (spaces.isEmpty()) return Map.of();
        List<UUID> ids = spaces.stream().map(Space::getId).toList();
        List<SpaceImage> primaries = spaceImageRepository.findBySpaceIdInAndPrimaryTrue(ids);
        Map<UUID, String> out = new HashMap<>(primaries.size());
        for (SpaceImage img : primaries) {
            out.put(img.getSpace().getId(), img.getUrl());
        }
        return out;
    }

    public SpaceResponse getBySlugOrId(String slugOrId) {
        Space s = resolve(slugOrId);
        return spaceMapper.toResponse(s);
    }

    public Space requireById(UUID id) {
        return spaceRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new EntityNotFoundException("Space", id.toString()));
    }

    public Space requireBySlug(String slug) {
        return spaceRepository.findBySlug(slug)
                .filter(s -> !s.isDeleted())
                .orElseThrow(() -> new EntityNotFoundException("Space", slug));
    }

    private Space resolve(String slugOrId) {
        try {
            UUID id = UUID.fromString(slugOrId);
            return requireById(id);
        } catch (IllegalArgumentException notUuid) {
            return requireBySlug(slugOrId);
        }
    }

    @Transactional
    public SpaceResponse create(SpaceCreateRequest req, UUID actorId) {
        Building b = resolveBuilding(req.buildingId(), req.building());

        Space s = Space.builder()
                .slug(generateSlug(req.name()))
                .ownerId(actorId)
                .ownerType(SpaceOwnerType.USER) // Default to USER for now
                .name(req.name())
                .type(req.type())
                .building(b)
                .floor(req.floor())
                .room(req.room())
                .seats(req.seats())
                .areaSqm(req.area())
                .pricePerHour(req.price())
                .currency("USD")
                .blurb(req.blurb())
                .description(req.description())
                .amenities(objectMapper.valueToTree(req.amenities() != null ? req.amenities() : List.of()))
                .rules(objectMapper.valueToTree(req.rules() != null ? req.rules() : List.of()))
                .operatingHours(objectMapper.valueToTree(req.operatingHours() != null ? req.operatingHours() : Map.of("mode", "24/7")))
                .pinX(req.pinX())
                .pinY(req.pinY())
                .status(SpaceStatus.PUBLISHED)
                .surprise(req.surprise())
                .dropIn(req.dropIn())
                .instantBook(req.instantBook())
                .build();

        s = spaceRepository.save(s);
        updateVibes(s, req.vibes());
        return spaceMapper.toResponse(s);
    }

    @Transactional
    public SpaceResponse update(UUID id, SpaceUpdateRequest req, UUID actorId) {
        Space s = requireById(id);

        if (req.name() != null) {
            s.setName(req.name());
            s.setSlug(generateSlug(req.name()));
        }
        if (req.type() != null) s.setType(req.type());
        if (req.buildingId() != null || req.building() != null) {
            s.setBuilding(resolveBuilding(req.buildingId(), req.building()));
        }
        if (req.floor() != null) s.setFloor(req.floor());
        if (req.room() != null) s.setRoom(req.room());
        if (req.seats() != null) s.setSeats(req.seats());
        if (req.area() != null) s.setAreaSqm(req.area());
        if (req.price() != null) s.setPricePerHour(req.price());
        if (req.blurb() != null) s.setBlurb(req.blurb());
        if (req.description() != null) s.setDescription(req.description());
        if (req.amenities() != null) s.setAmenities(objectMapper.valueToTree(req.amenities()));
        if (req.rules() != null) s.setRules(objectMapper.valueToTree(req.rules()));
        if (req.operatingHours() != null) s.setOperatingHours(objectMapper.valueToTree(req.operatingHours()));
        if (req.pinX() != null) s.setPinX(req.pinX());
        if (req.pinY() != null) s.setPinY(req.pinY());
        if (req.surprise() != null) s.setSurprise(req.surprise());
        if (req.dropIn() != null) s.setDropIn(req.dropIn());
        if (req.instantBook() != null) s.setInstantBook(req.instantBook());

        if (req.vibes() != null) {
            updateVibes(s, req.vibes());
        }

        return spaceMapper.toResponse(s);
    }

    private Building resolveBuilding(UUID id, String name) {
        if (id != null) {
            return buildingRepository.findById(id)
                    .orElseThrow(() -> new EntityNotFoundException("Building", id.toString()));
        }
        if (name != null) {
            return buildingRepository.findByName(name)
                    .orElseThrow(() -> new EntityNotFoundException("Building", name));
        }
        throw new IllegalArgumentException("Building ID or name must be provided");
    }


    @Transactional
    public void delete(UUID id, UUID actorId) {
        Space s = requireById(id);
        s.setDeletedAt(Instant.now());
        spaceRepository.save(s);
    }

    private void updateVibes(Space s, List<String> vibeIds) {
        s.getSpaceVibes().clear();
        if (vibeIds != null) {
            for (String vibeId : vibeIds) {
                Vibe v = vibeRepository.findById(vibeId)
                        .orElseThrow(() -> new EntityNotFoundException("Vibe", vibeId));
                SpaceVibe sv = SpaceVibe.builder()
                        .id(new SpaceVibeId(s.getId(), v.getId()))
                        .space(s)
                        .vibe(v)
                        .build();
                s.getSpaceVibes().add(sv);
            }
        }
    }

    private String generateSlug(String name) {
        String base = name.toLowerCase()
                .replaceAll("[^a-z0-9\\s-]", "")
                .trim()
                .replaceAll("\\s+", "-")
                .replaceAll("-+", "-");
        
        // Remove leading/trailing dashes safely
        while (base.startsWith("-")) base = base.substring(1);
        while (base.endsWith("-")) base = base.substring(0, base.length() - 1);
        
        // Database check constraint requires min 2 chars. 
        // If name is too short after stripping, use a prefix.
        if (base.length() < 2) base = "sp-" + base;
        
        // Simple deduplication
        String candidate = base;
        int attempt = 1;
        while (spaceRepository.findBySlug(candidate).isPresent()) {
            candidate = base + "-" + attempt++;
        }
        return candidate;
    }


    /**
     * IDs of every space the user can manage (owns directly, or is a member
     * of an owning org). Used by the reservation owner-queue endpoint.
     */
    public List<UUID> findManageableSpaceIds(User actor) {
        return findManageableSpaceIds(actor.getId());
    }

    public List<UUID> findManageableSpaceIds(UUID actorId) {
        Set<UUID> ids = new HashSet<>(spaceRepository.findIdsOwnedByUser(actorId, SpaceOwnerType.USER));
        List<UUID> orgIds = new ArrayList<>();
        User actor = userRepository.findById(actorId).orElseThrow(() -> new EntityNotFoundException("User", actorId.toString()));
        for (OrgMembership m : orgMembershipRepository.findByUser(actor)) {
            orgIds.add(m.getOrganization().getId());
        }
        if (!orgIds.isEmpty()) {
            ids.addAll(spaceRepository.findIdsOwnedByOrgs(orgIds, SpaceOwnerType.ORGANIZATION));
        }
        return List.copyOf(ids);
    }

    private static Sort resolveSort(String key) {
        // Whitelist sortable columns to avoid arbitrary-property injection.
        if (key == null || key.isBlank()) return Sort.by(Sort.Direction.ASC, "name");
        return switch (key) {
            case "newest"      -> Sort.by(Sort.Direction.DESC, "createdAt");
            case "priceAsc"    -> Sort.by(Sort.Direction.ASC, "pricePerHour");
            case "priceDesc"   -> Sort.by(Sort.Direction.DESC, "pricePerHour");
            case "seatsDesc"   -> Sort.by(Sort.Direction.DESC, "seats");
            case "name"        -> Sort.by(Sort.Direction.ASC, "name");
            default            -> Sort.by(Sort.Direction.ASC, "name");
        };
    }
}

