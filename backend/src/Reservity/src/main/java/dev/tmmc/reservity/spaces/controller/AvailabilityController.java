package dev.tmmc.reservity.spaces.controller;

import dev.tmmc.reservity.spaces.dto.AvailabilityResponse;
import dev.tmmc.reservity.spaces.dto.BatchAvailabilityRequest;
import dev.tmmc.reservity.spaces.dto.BatchAvailabilityResponse;
import dev.tmmc.reservity.spaces.dto.CalendarDayResponse;
import dev.tmmc.reservity.spaces.service.AvailabilityService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

/**
 * Read-side calendar endpoints. Public — no auth header required.
 *
 * <ul>
 *   <li>{@code GET /api/spaces/{slugOrId}/availability?from=&to=} — multi-day grid for the schedule view.</li>
 *   <li>{@code GET /api/spaces/{slugOrId}/calendar?date=} — single-day richer payload.</li>
 *   <li>{@code POST /api/spaces/availability:batch} — one round-trip for ListingsRoute when the date pill changes.</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/spaces")
@RequiredArgsConstructor
public class AvailabilityController {

    private final AvailabilityService availabilityService;

    @GetMapping("/{slugOrId}/availability")
    public AvailabilityResponse availability(
            @PathVariable String slugOrId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to
    ) {
        return availabilityService.availabilityRange(slugOrId, from, to);
    }

    @GetMapping("/{slugOrId}/calendar")
    public CalendarDayResponse calendar(
            @PathVariable String slugOrId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date
    ) {
        return availabilityService.calendarDay(slugOrId, date);
    }

    @PostMapping("/availability:batch")
    public BatchAvailabilityResponse batch(@Valid @RequestBody BatchAvailabilityRequest req) {
        return availabilityService.batch(req);
    }
}
