package dev.tmmc.reservity.reservations.service;

import dev.tmmc.reservity.reservations.entity.Reservation;
import dev.tmmc.reservity.reservations.repository.ReservationRepository;
import dev.tmmc.reservity.spaces.availability.EventBlock;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Bridges {@code reservations} → {@link EventBlock} entries so the M4
 * {@code AvailabilityCalculator} can layer RESERVED hours onto the grid.
 *
 * <p>This bean lives in the {@code reservations} package — {@code spaces}
 * remains free of any direct knowledge of the reservation entities. The
 * existing {@code AvailabilityService} simply asks for an event block list
 * for a (space, day) and treats the result opaquely.</p>
 */
@Component
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReservationLookup {

    private static final int HOURS_PER_DAY = 24;

    private final ReservationRepository reservationRepository;

    /**
     * Event blocks for a single space on a single day, clipped to the day's
     * 24-hour grid.
     */
    public List<EventBlock> eventBlocksFor(UUID spaceId, LocalDate day, ZoneId zone) {
        Instant dayStart = day.atStartOfDay(zone).toInstant();
        Instant dayEnd = day.plusDays(1).atStartOfDay(zone).toInstant();
        List<Reservation> reservations = reservationRepository.findOverlapping(spaceId, dayStart, dayEnd);
        return toBlocks(reservations, day, zone);
    }

    /**
     * Same window across multiple spaces in one query. Returns a map keyed by
     * space id; spaces with no reservations are absent from the map.
     */
    public Map<UUID, List<EventBlock>> eventBlocksForSpaces(Collection<UUID> spaceIds, LocalDate day, ZoneId zone) {
        if (spaceIds == null || spaceIds.isEmpty()) return Map.of();
        Instant dayStart = day.atStartOfDay(zone).toInstant();
        Instant dayEnd = day.plusDays(1).atStartOfDay(zone).toInstant();
        List<Reservation> all = reservationRepository.findOverlappingForSpaces(spaceIds, dayStart, dayEnd);

        Map<UUID, List<Reservation>> grouped = new HashMap<>();
        for (Reservation r : all) {
            grouped.computeIfAbsent(r.getSpace().getId(), k -> new ArrayList<>()).add(r);
        }

        Map<UUID, List<EventBlock>> result = new HashMap<>();
        for (Map.Entry<UUID, List<Reservation>> entry : grouped.entrySet()) {
            result.put(entry.getKey(), toBlocks(entry.getValue(), day, zone));
        }
        return result;
    }

    private static List<EventBlock> toBlocks(List<Reservation> reservations, LocalDate day, ZoneId zone) {
        if (reservations.isEmpty()) return List.of();
        Instant dayStart = day.atStartOfDay(zone).toInstant();
        Instant dayEnd = day.plusDays(1).atStartOfDay(zone).toInstant();
        List<EventBlock> blocks = new ArrayList<>(reservations.size());
        for (Reservation r : reservations) {
            Instant rs = r.getStartsAt().isBefore(dayStart) ? dayStart : r.getStartsAt();
            Instant re = r.getEndsAt().isAfter(dayEnd) ? dayEnd : r.getEndsAt();
            if (!re.isAfter(rs)) continue;

            int startH = ZonedDateTime.ofInstant(rs, zone).getHour();
            int endH;
            if (re.equals(dayEnd)) {
                endH = HOURS_PER_DAY;
            } else {
                ZonedDateTime endZdt = ZonedDateTime.ofInstant(re, zone);
                if (endZdt.getMinute() == 0 && endZdt.getSecond() == 0 && endZdt.getNano() == 0) {
                    endH = endZdt.getHour();
                } else {
                    endH = Math.min(HOURS_PER_DAY, endZdt.getHour() + 1);
                }
            }
            if (endH <= startH) continue;

            String host = r.getUser() != null ? r.getUser().getDisplayName() : "Reserved";
            blocks.add(new EventBlock(
                    EventBlock.Source.RESERVATION,
                    r.getId(),
                    startH,
                    endH,
                    "Reserved",
                    host
            ));
        }
        return blocks;
    }
}
