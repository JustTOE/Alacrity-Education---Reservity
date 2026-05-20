package dev.tmmc.reservity.reservations.controller;

import dev.tmmc.reservity.common.exception.EntityNotFoundException;
import dev.tmmc.reservity.common.pagination.PageResponse;
import dev.tmmc.reservity.common.security.SecurityUser;
import dev.tmmc.reservity.reservations.dto.PassResponse;
import dev.tmmc.reservity.reservations.dto.ReservationResponse;
import dev.tmmc.reservity.reservations.service.ReservationService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/reservations")
@RequiredArgsConstructor
public class ReservationController {

    private final ReservationService service;

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public PageResponse<ReservationResponse> listAll(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return service.listAllReservations(page, size);
    }

    @GetMapping("/mine")
    public PageResponse<ReservationResponse> mine(
            @AuthenticationPrincipal SecurityUser principal,
            @RequestParam(name = "upcoming", defaultValue = "false") boolean upcoming,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return service.listMineReservations(requireUserId(principal), upcoming, page, size);
    }

    @GetMapping("/{id}")
    public ReservationResponse get(
            @AuthenticationPrincipal SecurityUser principal,
            @PathVariable UUID id) {
        return service.getReservation(id, requireUserId(principal));
    }

    @GetMapping("/{id}/pass")
    public PassResponse pass(
            @AuthenticationPrincipal SecurityUser principal,
            @PathVariable UUID id) {
        return service.getPass(id, requireUserId(principal));
    }

    private static UUID requireUserId(SecurityUser principal) {
        if (principal == null) throw new EntityNotFoundException("Not authenticated");
        return principal.getUserId();
    }
}

