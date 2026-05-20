package dev.tmmc.reservity.spaces.service;

import dev.tmmc.reservity.common.exception.EntityNotFoundException;
import dev.tmmc.reservity.reservations.service.ReservationLookup;
import dev.tmmc.reservity.spaces.availability.AvailabilityCalculator;
import dev.tmmc.reservity.spaces.availability.DayAvailability;
import dev.tmmc.reservity.spaces.availability.EventBlock;
import dev.tmmc.reservity.spaces.availability.OperatingHours;
import dev.tmmc.reservity.spaces.availability.OperatingHoursParser;
import dev.tmmc.reservity.spaces.dto.AvailabilityResponse;
import dev.tmmc.reservity.spaces.dto.BatchAvailabilityRequest;
import dev.tmmc.reservity.spaces.dto.BatchAvailabilityResponse;
import dev.tmmc.reservity.spaces.dto.CalendarDayResponse;
import dev.tmmc.reservity.spaces.entity.Space;
import dev.tmmc.reservity.spaces.entity.SpaceClosure;
import dev.tmmc.reservity.spaces.repository.SpaceClosureRepository;
import dev.tmmc.reservity.spaces.repository.SpaceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AvailabilityService {

    public static final int MAX_RANGE_DAYS = 60;
    public static final int MAX_BATCH_SIZE = 50;

    private static final ZoneId DEFAULT_ZONE = ZoneId.of("UTC");

    private final SpaceService spaceService;
    private final SpaceRepository spaceRepository;
    private final SpaceClosureRepository closureRepository;
    private final OperatingHoursParser hoursParser;
    private final AvailabilityCalculator calculator;
    private final ReservationLookup reservationLookup;

    public AvailabilityResponse availabilityRange(String slugOrId, LocalDate from, LocalDate to) {
        validateRange(from, to);
        Space space = resolve(slugOrId);
        ZoneId zone = DEFAULT_ZONE;

        Instant rangeStart = from.atStartOfDay(zone).toInstant();
        Instant rangeEnd = to.plusDays(1).atStartOfDay(zone).toInstant();
        List<SpaceClosure> closures = closureRepository.findOverlapping(space.getId(), rangeStart, rangeEnd);

        OperatingHours hours = hoursParser.parse(space.getOperatingHours());

        List<DayAvailability> days = new ArrayList<>();
        for (LocalDate d = from; !d.isAfter(to); d = d.plusDays(1)) {
            List<SpaceClosure> dayClosures = filterClosuresForDay(closures, d, zone);
            List<EventBlock> events = reservationLookup.eventBlocksFor(space.getId(), d, zone);
            days.add(calculator.calculate(d, zone, hours, dayClosures, events));
        }

        return new AvailabilityResponse(
                space.getId(),
                space.getSlug(),
                space.getName(),
                zone.getId(),
                days
        );
    }

    public CalendarDayResponse calendarDay(String slugOrId, LocalDate date) {
        if (date == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "date is required");
        }
        Space space = resolve(slugOrId);
        ZoneId zone = DEFAULT_ZONE;

        Instant dayStart = date.atStartOfDay(zone).toInstant();
        Instant dayEnd = date.plusDays(1).atStartOfDay(zone).toInstant();
        List<SpaceClosure> closures = closureRepository.findOverlapping(space.getId(), dayStart, dayEnd);
        OperatingHours hours = hoursParser.parse(space.getOperatingHours());

        List<EventBlock> events = reservationLookup.eventBlocksFor(space.getId(), date, zone);
        DayAvailability day = calculator.calculate(date, zone, hours, closures, events);
        return new CalendarDayResponse(
                space.getId(),
                space.getSlug(),
                space.getName(),
                zone.getId(),
                day
        );
    }

    public BatchAvailabilityResponse batch(BatchAvailabilityRequest req) {
        if (req == null || req.date() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "date is required");
        }
        boolean hasIds = req.spaceIds() != null && !req.spaceIds().isEmpty();
        boolean hasSlugs = req.slugs() != null && !req.slugs().isEmpty();
        if (hasIds == hasSlugs) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Provide exactly one of spaceIds or slugs");
        }
        int size = hasIds ? req.spaceIds().size() : req.slugs().size();
        if (size > MAX_BATCH_SIZE) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Too many spaces in batch — max " + MAX_BATCH_SIZE);
        }

        List<Space> spaces = hasIds ? loadByIds(req.spaceIds()) : loadBySlugs(req.slugs());
        ZoneId zone = DEFAULT_ZONE;
        LocalDate date = req.date();
        Instant dayStart = date.atStartOfDay(zone).toInstant();
        Instant dayEnd = date.plusDays(1).atStartOfDay(zone).toInstant();

        List<UUID> ids = spaces.stream().map(Space::getId).toList();
        List<SpaceClosure> allClosures = ids.isEmpty()
                ? List.of()
                : closureRepository.findOverlappingForSpaces(ids, dayStart, dayEnd);
        Map<UUID, List<SpaceClosure>> closuresByspace = new HashMap<>();
        for (SpaceClosure c : allClosures) {
            closuresByspace.computeIfAbsent(c.getSpace().getId(), k -> new ArrayList<>()).add(c);
        }

        Map<UUID, List<EventBlock>> eventsByspace = reservationLookup.eventBlocksForSpaces(ids, date, zone);
        Map<String, DayAvailability> result = new LinkedHashMap<>();
        for (Space s : spaces) {
            OperatingHours hours = hoursParser.parse(s.getOperatingHours());
            List<SpaceClosure> dayClosures = closuresByspace.getOrDefault(s.getId(), List.of());
            List<EventBlock> events = eventsByspace.getOrDefault(s.getId(), List.of());
            DayAvailability day = calculator.calculate(date, zone, hours, dayClosures, events);
            result.put(s.getSlug(), day);
        }
        return new BatchAvailabilityResponse(date, zone.getId(), result);
    }

    private void validateRange(LocalDate from, LocalDate to) {
        if (from == null || to == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "from and to are required");
        }
        if (from.isAfter(to)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "from must be on or before to");
        }
        long days = ChronoUnit.DAYS.between(from, to) + 1;
        if (days > MAX_RANGE_DAYS) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Range too long — max " + MAX_RANGE_DAYS + " days");
        }
    }

    private Space resolve(String slugOrId) {
        try {
            UUID id = UUID.fromString(slugOrId);
            return spaceService.requireById(id);
        } catch (IllegalArgumentException notUuid) {
            return spaceService.requireBySlug(slugOrId);
        }
    }

    private List<Space> loadByIds(Collection<UUID> ids) {
        Set<UUID> wanted = new HashSet<>(ids);
        List<Space> found = spaceRepository.findAllById(wanted).stream()
                .filter(s -> !s.isDeleted())
                .toList();
        Set<UUID> foundIds = new HashSet<>();
        for (Space s : found) foundIds.add(s.getId());
        for (UUID id : wanted) {
            if (!foundIds.contains(id)) {
                throw new EntityNotFoundException("Space", id.toString());
            }
        }
        return found;
    }

    private List<Space> loadBySlugs(Collection<String> slugs) {
        List<Space> out = new ArrayList<>(slugs.size());
        for (String slug : slugs) {
            out.add(spaceService.requireBySlug(slug));
        }
        return out;
    }

    private static List<SpaceClosure> filterClosuresForDay(List<SpaceClosure> closures, LocalDate day, ZoneId zone) {
        if (closures.isEmpty()) return List.of();
        Instant dayStart = day.atStartOfDay(zone).toInstant();
        Instant dayEnd = day.plusDays(1).atStartOfDay(zone).toInstant();
        List<SpaceClosure> out = new ArrayList<>();
        for (SpaceClosure c : closures) {
            if (c.getStartsAt().isBefore(dayEnd) && c.getEndsAt().isAfter(dayStart)) {
                out.add(c);
            }
        }
        return out;
    }
}
