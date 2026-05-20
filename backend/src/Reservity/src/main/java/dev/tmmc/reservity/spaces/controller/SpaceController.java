package dev.tmmc.reservity.spaces.controller;

import dev.tmmc.reservity.common.exception.EntityNotFoundException;
import dev.tmmc.reservity.common.pagination.PageResponse;
import dev.tmmc.reservity.common.security.SecurityUser;
import dev.tmmc.reservity.spaces.dto.SpaceCreateRequest;
import dev.tmmc.reservity.spaces.dto.SpaceFilterParams;
import dev.tmmc.reservity.spaces.dto.SpaceResponse;
import dev.tmmc.reservity.spaces.dto.SpaceSummary;
import dev.tmmc.reservity.spaces.dto.SpaceUpdateRequest;
import dev.tmmc.reservity.spaces.service.SpaceService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/spaces")
@RequiredArgsConstructor
public class SpaceController {

    private final SpaceService spaceService;

    /**
     * Filterable paginated list. Filters that depend on bookings data
     * ({@code hideFull}) are accepted but ignored until M5 — the booked /
     * fully-booked computation is wired in once reservations land.
     */
    @GetMapping
    public PageResponse<SpaceSummary> list(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) Short minSeats,
            @RequestParam(required = false) BigDecimal maxPrice,
            @RequestParam(required = false) Boolean isFree,
            @RequestParam(required = false) String vibes,
            @RequestParam(required = false) UUID buildingId,
            @RequestParam(required = false) Boolean hideFull,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String sort
    ) {
        List<String> vibeList = parseCsv(vibes);
        SpaceFilterParams params = new SpaceFilterParams(
                q, type, minSeats, maxPrice, isFree, vibeList, buildingId, hideFull
        );
        return spaceService.search(params, page, size, sort);
    }

    @GetMapping("/{slugOrId}")
    public SpaceResponse get(@PathVariable String slugOrId) {
        return spaceService.getBySlugOrId(slugOrId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public SpaceResponse create(
            @AuthenticationPrincipal SecurityUser principal,
            @Valid @RequestBody SpaceCreateRequest req) {
        return spaceService.create(req, requireUserId(principal));
    }

    @PutMapping("/{id}")
    @PreAuthorize("@spaceSecurity.isOwner(#id, principal)")
    public SpaceResponse update(
            @AuthenticationPrincipal SecurityUser principal,
            @PathVariable UUID id,
            @Valid @RequestBody SpaceUpdateRequest req) {
        return spaceService.update(id, req, requireUserId(principal));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("@spaceSecurity.isOwner(#id, principal)")
    public void delete(
            @AuthenticationPrincipal SecurityUser principal,
            @PathVariable UUID id) {
        spaceService.delete(id, requireUserId(principal));
    }

    private static UUID requireUserId(SecurityUser principal) {
        if (principal == null) throw new EntityNotFoundException("Not authenticated");
        return principal.getUserId();
    }

    private static List<String> parseCsv(String raw) {
        if (raw == null || raw.isBlank()) return List.of();
        return Arrays.stream(raw.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();
    }
}

