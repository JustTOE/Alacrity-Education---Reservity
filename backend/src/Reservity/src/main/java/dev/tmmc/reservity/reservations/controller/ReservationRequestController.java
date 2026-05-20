package dev.tmmc.reservity.reservations.controller;

import dev.tmmc.reservity.common.exception.EntityNotFoundException;
import dev.tmmc.reservity.common.pagination.PageResponse;
import dev.tmmc.reservity.common.security.SecurityUser;
import dev.tmmc.reservity.reservations.dto.CancelRequestBody;
import dev.tmmc.reservity.reservations.dto.CreateReservationRequest;
import dev.tmmc.reservity.reservations.dto.DenyRequestBody;
import dev.tmmc.reservity.reservations.dto.ReservationRequestResponse;
import dev.tmmc.reservity.reservations.dto.ReservationResponse;
import dev.tmmc.reservity.reservations.entity.ReservationStatus;
import dev.tmmc.reservity.reservations.service.ReservationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class ReservationRequestController {

    private final ReservationService service;

    @PostMapping("/api/spaces/{slugOrId}/reservation-requests")
    public ResponseEntity<ReservationResponse> submit(
            @AuthenticationPrincipal SecurityUser principal,
            @PathVariable String slugOrId,
            @Valid @RequestBody CreateReservationRequest body) {
        ReservationResponse resp = service.submit(slugOrId, body, requireUserId(principal));
        return ResponseEntity.status(HttpStatus.CREATED).body(resp);
    }

    @GetMapping("/api/reservation-requests/mine")
    public PageResponse<ReservationRequestResponse> mine(
            @AuthenticationPrincipal SecurityUser principal,
            @RequestParam(required = false) ReservationStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return service.listMineRequests(requireUserId(principal), status, page, size);
    }

    @GetMapping("/api/reservation-requests/received")
    public PageResponse<ReservationRequestResponse> received(
            @AuthenticationPrincipal SecurityUser principal,
            @RequestParam(required = false) ReservationStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return service.listReceivedRequests(requireUserId(principal), status, page, size);
    }

    @GetMapping("/api/reservation-requests/{id}")
    public ReservationRequestResponse get(
            @AuthenticationPrincipal SecurityUser principal,
            @PathVariable UUID id) {
        return service.getRequest(id, requireUserId(principal));
    }

    @PostMapping("/api/reservation-requests/{id}/approve")
    public ReservationResponse approve(
            @AuthenticationPrincipal SecurityUser principal,
            @PathVariable UUID id) {
        return service.approveRequest(id, requireUserId(principal));
    }

    @PostMapping("/api/reservation-requests/{id}/deny")
    public ReservationRequestResponse deny(
            @AuthenticationPrincipal SecurityUser principal,
            @PathVariable UUID id,
            @Valid @RequestBody DenyRequestBody body) {
        return service.denyRequest(id, requireUserId(principal), body.reason());
    }

    @PostMapping("/api/reservation-requests/{id}/cancel")
    public ReservationRequestResponse cancel(
            @AuthenticationPrincipal SecurityUser principal,
            @PathVariable UUID id,
            @RequestBody(required = false) CancelRequestBody body) {
        String reason = body != null ? body.reason() : null;
        return service.cancelRequest(id, requireUserId(principal), reason);
    }

    private static UUID requireUserId(SecurityUser principal) {
        if (principal == null) throw new EntityNotFoundException("Not authenticated");
        return principal.getUserId();
    }
}
