package dev.tmmc.reservity.spaces.service;

import dev.tmmc.reservity.common.exception.EntityNotFoundException;
import dev.tmmc.reservity.spaces.dto.BuildingResponse;
import dev.tmmc.reservity.spaces.dto.SpaceSummary;
import dev.tmmc.reservity.spaces.entity.Building;
import dev.tmmc.reservity.spaces.mapper.BuildingMapper;
import dev.tmmc.reservity.spaces.mapper.SpaceMapper;
import dev.tmmc.reservity.spaces.repository.BuildingRepository;
import dev.tmmc.reservity.spaces.repository.SpaceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BuildingService {

    private final BuildingRepository buildingRepository;
    private final SpaceRepository spaceRepository;
    private final BuildingMapper buildingMapper;
    private final SpaceMapper spaceMapper;

    public List<BuildingResponse> listAll() {
        return buildingRepository.findAll(Sort.by("name").ascending()).stream()
                .map(buildingMapper::toResponse)
                .toList();
    }

    public BuildingResponse getBySlugOrId(String slugOrId) {
        return buildingMapper.toResponse(resolve(slugOrId));
    }

    public List<SpaceSummary> spacesIn(UUID buildingId) {
        // Reuse the published filter from SpaceQuerySpec so unpublished spaces don't leak.
        Specification<dev.tmmc.reservity.spaces.entity.Space> spec = SpaceQuerySpec.all(
                SpaceQuerySpec.published(),
                SpaceQuerySpec.byBuilding(buildingId)
        );
        return spaceRepository.findAll(spec, Sort.by("name").ascending()).stream()
                .map(spaceMapper::toSummary)
                .toList();
    }

    private Building resolve(String slugOrId) {
        // Buildings don't have a slug column today — accept either UUID or short_code.
        try {
            UUID id = UUID.fromString(slugOrId);
            return buildingRepository.findById(id)
                    .orElseThrow(() -> new EntityNotFoundException("Building", slugOrId));
        } catch (IllegalArgumentException notUuid) {
            return buildingRepository.findByShortCodeIgnoreCase(slugOrId)
                    .orElseThrow(() -> new EntityNotFoundException("Building", slugOrId));
        }
    }
}
