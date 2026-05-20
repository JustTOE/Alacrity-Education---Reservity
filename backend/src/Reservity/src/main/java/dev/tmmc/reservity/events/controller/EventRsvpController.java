package dev.tmmc.reservity.events.controller;

import dev.tmmc.reservity.common.exception.EntityNotFoundException;
import dev.tmmc.reservity.common.pagination.PageResponse;
import dev.tmmc.reservity.common.security.SecurityUser;
import dev.tmmc.reservity.events.dto.EventAttendeeSummary;
import dev.tmmc.reservity.events.dto.EventRsvpResponse;
import dev.tmmc.reservity.events.dto.RsvpRequest;
import dev.tmmc.reservity.events.entity.EventRsvp;
import dev.tmmc.reservity.events.entity.enums.RsvpStatus;
import dev.tmmc.reservity.events.mapper.EventRsvpMapper;
import dev.tmmc.reservity.events.service.EventRsvpService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/events/{eventId}")
public class EventRsvpController {

    private final EventRsvpService service;
    private final EventRsvpMapper mapper;

    @GetMapping("/attendees")
    public PageResponse<EventAttendeeSummary> attendees(
            @PathVariable UUID eventId,
            @RequestParam(required = false) RsvpStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "30") int size) {
        return service.listAttendees(eventId, status, page, size);
    }

    @PostMapping("/rsvp")
    @ResponseStatus(HttpStatus.CREATED)
    public EventRsvpResponse rsvp(
            @AuthenticationPrincipal SecurityUser principal,
            @PathVariable UUID eventId,
            @Valid @RequestBody RsvpRequest body) {
        UUID callerId = requireUserId(principal);
        EventRsvp rsvp = service.rsvp(eventId, callerId, body.status());
        return mapper.toResponse(rsvp);
    }

    @PatchMapping("/rsvp")
    public EventRsvpResponse updateRsvp(
            @AuthenticationPrincipal SecurityUser principal,
            @PathVariable UUID eventId,
            @Valid @RequestBody RsvpRequest body) {
        UUID callerId = requireUserId(principal);
        EventRsvp rsvp = service.rsvp(eventId, callerId, body.status());
        return mapper.toResponse(rsvp);
    }

    @DeleteMapping("/rsvp")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void cancelRsvp(
            @AuthenticationPrincipal SecurityUser principal,
            @PathVariable UUID eventId) {
        UUID callerId = requireUserId(principal);
        service.cancel(eventId, callerId);
    }

    private static UUID requireUserId(SecurityUser principal) {
        if (principal == null) throw new EntityNotFoundException("Not authenticated");
        return principal.getUserId();
    }
}
