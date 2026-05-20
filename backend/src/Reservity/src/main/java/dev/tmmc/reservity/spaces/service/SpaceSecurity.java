package dev.tmmc.reservity.spaces.service;

import dev.tmmc.reservity.common.security.SecurityUser;
import dev.tmmc.reservity.spaces.entity.Space;
import dev.tmmc.reservity.spaces.entity.SpaceOwnerType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component("spaceSecurity")
@RequiredArgsConstructor
public class SpaceSecurity {

    private final SpaceService spaceService;

    public boolean isOwner(String slugOrId, SecurityUser principal) {
        if (principal == null) return false;
        try {
            Space s = spaceService.requireBySlug(slugOrId);
            return isOwner(s, principal.getUserId());
        } catch (Exception e) {
            try {
                Space s = spaceService.requireById(UUID.fromString(slugOrId));
                return isOwner(s, principal.getUserId());
            } catch (Exception e2) {
                return false;
            }
        }
    }

    public boolean isOwner(UUID spaceId, SecurityUser principal) {
        if (principal == null) return false;
        try {
            Space s = spaceService.requireById(spaceId);
            return isOwner(s, principal.getUserId());
        } catch (Exception e) {
            return false;
        }
    }

    private boolean isOwner(Space s, UUID userId) {
        if (s.getOwnerType() == SpaceOwnerType.USER) {
            return s.getOwnerId().equals(userId);
        }
        return spaceService.findManageableSpaceIds(userId).contains(s.getId());
    }
}
