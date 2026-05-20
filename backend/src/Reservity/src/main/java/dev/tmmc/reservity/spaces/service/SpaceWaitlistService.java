package dev.tmmc.reservity.spaces.service;

import dev.tmmc.reservity.common.exception.EntityNotFoundException;
import dev.tmmc.reservity.common.exception.ForbiddenOperationException;
import dev.tmmc.reservity.spaces.dto.WaitlistRequest;
import dev.tmmc.reservity.spaces.dto.WaitlistResponse;
import dev.tmmc.reservity.spaces.entity.Space;
import dev.tmmc.reservity.spaces.entity.SpaceWaitlistEntry;
import dev.tmmc.reservity.spaces.repository.SpaceWaitlistRepository;
import dev.tmmc.reservity.user.entity.User;
import dev.tmmc.reservity.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SpaceWaitlistService {

    private final SpaceWaitlistRepository waitlistRepository;
    private final SpaceService spaceService;
    private final UserRepository userRepository;

    @Transactional
    public WaitlistResponse add(UUID userId, String spaceSlugOrId, WaitlistRequest req) {
        Space space = resolveSpace(spaceSlugOrId);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User", userId));

        Instant from = req == null ? null : req.from();
        Instant to   = req == null ? null : req.to();
        if (from != null && to != null && !to.isAfter(from)) {
            throw new ForbiddenOperationException("Waitlist window 'to' must be after 'from'");
        }

        SpaceWaitlistEntry entry = SpaceWaitlistEntry.builder()
                .user(user)
                .space(space)
                .desiredFrom(from)
                .desiredTo(to)
                .expiresAt(Instant.now().plus(14, ChronoUnit.DAYS))
                .build();

        try {
            entry = waitlistRepository.save(entry);
        } catch (DataIntegrityViolationException dup) {
            // Unique (user, space, from, to) violated — already on the list for this window.
            throw new DataIntegrityViolationException("Already on the waitlist for that window", dup);
        }

        return toResponse(entry);
    }

    @Transactional
    public void remove(UUID userId, UUID entryId) {
        SpaceWaitlistEntry entry = waitlistRepository.findById(entryId)
                .orElseThrow(() -> new EntityNotFoundException("WaitlistEntry", entryId));
        if (!entry.getUser().getId().equals(userId)) {
            throw new ForbiddenOperationException("Cannot remove someone else's waitlist entry");
        }
        waitlistRepository.delete(entry);
    }

    @Transactional
    public void removeBySpace(UUID userId, String spaceSlugOrId) {
        Space space = resolveSpace(spaceSlugOrId);
        waitlistRepository.deleteByUserIdAndSpaceId(userId, space.getId());
    }


    @Transactional(readOnly = true)
    public List<WaitlistResponse> listForUser(UUID userId) {
        return waitlistRepository.findAllForUser(userId).stream()
                .map(this::toResponse)
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

    private WaitlistResponse toResponse(SpaceWaitlistEntry e) {
        return new WaitlistResponse(
                e.getId(),
                e.getSpace().getId(),
                e.getSpace().getSlug(),
                e.getSpace().getName(),
                e.getDesiredFrom(),
                e.getDesiredTo(),
                e.getExpiresAt(),
                e.getCreatedAt()
        );
    }
}
