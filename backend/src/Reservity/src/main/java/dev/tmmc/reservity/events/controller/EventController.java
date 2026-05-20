package dev.tmmc.reservity.events.controller;

import dev.tmmc.reservity.common.exception.EntityNotFoundException;
import dev.tmmc.reservity.common.pagination.PageResponse;
import dev.tmmc.reservity.common.security.SecurityUser;
import dev.tmmc.reservity.events.dto.EventCancelRequest;
import dev.tmmc.reservity.events.dto.EventCreateRequest;
import dev.tmmc.reservity.events.dto.EventResponse;
import dev.tmmc.reservity.events.dto.EventSummary;
import dev.tmmc.reservity.events.dto.EventUpdateRequest;
import dev.tmmc.reservity.events.entity.Event;
import dev.tmmc.reservity.events.entity.enums.EventCategory;
import dev.tmmc.reservity.events.entity.enums.EventDisplayStatus;
import dev.tmmc.reservity.events.entity.enums.EventHostType;
import dev.tmmc.reservity.events.mapper.EventMapper;
import dev.tmmc.reservity.events.service.EventService;
import dev.tmmc.reservity.events.service.EventService.EventListFilters;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/events")
public class EventController {

    private final EventService service;
    private final EventMapper mapper;

    @GetMapping
    public PageResponse<EventSummary> list(
            @AuthenticationPrincipal SecurityUser principal,
            @RequestParam(required = false) EventDisplayStatus status,
            @RequestParam(required = false) EventCategory category,
            @RequestParam(required = false) EventHostType hostType,
            @RequestParam(required = false) UUID hostId,
            @RequestParam(required = false) UUID spaceId,
            @RequestParam(required = false) UUID buildingId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        EventListFilters filters = new EventListFilters(
                status, category, hostType, hostId, spaceId, buildingId);
        UUID callerUserId = principal == null ? null : principal.getUserId();
        return service.list(filters, callerUserId, page, size);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public EventResponse create(
            @AuthenticationPrincipal SecurityUser principal,
            @Valid @RequestBody EventCreateRequest body) {
        UUID callerId = requireUserId(principal);
        Event event = service.create(body, callerId);
        return mapper.toResponse(event, null);
    }

    @GetMapping("/{slugOrId}")
    public EventResponse getBySlugOrId(
            @AuthenticationPrincipal SecurityUser principal,
            @PathVariable String slugOrId) {
        UUID callerUserId = principal == null ? null : principal.getUserId();
        return service.getBySlugOrId(slugOrId, callerUserId);
    }

    @PatchMapping("/{id}")
    public EventResponse update(
            @AuthenticationPrincipal SecurityUser principal,
            @PathVariable UUID id,
            @Valid @RequestBody EventUpdateRequest body) {
        UUID callerId = requireUserId(principal);
        Event event = service.update(id, body, callerId);
        return mapper.toResponse(event, null);
    }

    @PostMapping("/{id}/cancel")
    public EventResponse cancel(
            @AuthenticationPrincipal SecurityUser principal,
            @PathVariable UUID id,
            @RequestBody(required = false) EventCancelRequest body) {
        UUID callerId = requireUserId(principal);
        String reason = body == null ? null : body.reason();
        Event event = service.cancel(id, reason, callerId);
        return mapper.toResponse(event, null);
    }

    private static UUID requireUserId(SecurityUser principal) {
        if (principal == null) throw new EntityNotFoundException("Not authenticated");
        return principal.getUserId();
    }
}
