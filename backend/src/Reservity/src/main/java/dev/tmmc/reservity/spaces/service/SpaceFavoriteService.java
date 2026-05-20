package dev.tmmc.reservity.spaces.service;

import dev.tmmc.reservity.common.exception.EntityNotFoundException;
import dev.tmmc.reservity.spaces.dto.SpaceSummary;
import dev.tmmc.reservity.spaces.entity.Space;
import dev.tmmc.reservity.spaces.entity.SpaceFavorite;
import dev.tmmc.reservity.spaces.entity.SpaceFavoriteId;
import dev.tmmc.reservity.spaces.mapper.SpaceMapper;
import dev.tmmc.reservity.spaces.repository.SpaceFavoriteRepository;
import dev.tmmc.reservity.user.entity.User;
import dev.tmmc.reservity.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SpaceFavoriteService {

    private final SpaceFavoriteRepository favoriteRepository;
    private final SpaceService spaceService;
    private final UserRepository userRepository;
    private final SpaceMapper spaceMapper;

    @Transactional
    public void favorite(UUID userId, String spaceSlugOrId) {
        Space space = resolveSpace(spaceSlugOrId);
        SpaceFavoriteId id = new SpaceFavoriteId(userId, space.getId());
        if (favoriteRepository.existsByIdUserIdAndIdSpaceId(userId, space.getId())) {
            return; // idempotent
        }
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User", userId));
        favoriteRepository.save(SpaceFavorite.builder()
                .id(id)
                .user(user)
                .space(space)
                .build());
    }

    @Transactional
    public void unfavorite(UUID userId, String spaceSlugOrId) {
        Space space = resolveSpace(spaceSlugOrId);
        favoriteRepository.deleteByIdUserIdAndIdSpaceId(userId, space.getId());
    }

    @Transactional(readOnly = true)
    public List<SpaceSummary> listForUser(UUID userId) {
        return favoriteRepository.findAllForUser(userId).stream()
                .map(SpaceFavorite::getSpace)
                .filter(s -> !s.isDeleted())
                .map(spaceMapper::toSummary)
                .toList();
    }

    private Space resolveSpace(String slugOrId) {
        try {
            UUID id = UUID.fromString(slugOrId);
            return spaceService.requireById(id);
        } catch (IllegalArgumentException notUuid) {
            return spaceService.requireBySlug(slugOrId);
        }
    }
}
