package dev.tmmc.reservity.spaces.service;

import dev.tmmc.reservity.common.exception.EntityNotFoundException;
import dev.tmmc.reservity.common.pagination.PageResponse;
import dev.tmmc.reservity.organization.entity.OrgMembership;
import dev.tmmc.reservity.organization.repository.OrgMembershipRepository;
import dev.tmmc.reservity.spaces.dto.SpaceFilterParams;
import dev.tmmc.reservity.spaces.dto.SpaceResponse;
import dev.tmmc.reservity.spaces.dto.SpaceSummary;
import dev.tmmc.reservity.spaces.entity.Space;
import dev.tmmc.reservity.spaces.entity.SpaceImage;
import dev.tmmc.reservity.spaces.entity.SpaceOwnerType;
import dev.tmmc.reservity.spaces.mapper.SpaceMapper;
import dev.tmmc.reservity.spaces.repository.SpaceImageRepository;
import dev.tmmc.reservity.spaces.repository.SpaceRepository;
import dev.tmmc.reservity.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
    private final SpaceMapper spaceMapper;
    private final OrgMembershipRepository orgMembershipRepository;

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
                // f.hideFull() — no-op until M5; document in SpaceController javadoc.
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

    /**
     * IDs of every space the user can manage (owns directly, or is a member
     * of an owning org). Used by the reservation owner-queue endpoint.
     */
    public List<UUID> findManageableSpaceIds(User actor) {
        Set<UUID> ids = new HashSet<>(spaceRepository.findIdsOwnedByUser(actor.getId(), SpaceOwnerType.USER));
        List<UUID> orgIds = new ArrayList<>();
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
