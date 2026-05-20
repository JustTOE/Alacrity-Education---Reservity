package dev.tmmc.reservity.reservations.service;

import dev.tmmc.reservity.common.exception.EntityNotFoundException;
import dev.tmmc.reservity.common.exception.ForbiddenOperationException;
import dev.tmmc.reservity.common.exception.ReservationConflictException;
import dev.tmmc.reservity.common.pagination.PageResponse;
import dev.tmmc.reservity.organization.entity.Organization;
import dev.tmmc.reservity.organization.repository.OrganizationRepository;
import dev.tmmc.reservity.organization.repository.OrgMembershipRepository;
import dev.tmmc.reservity.reservations.dto.CreateReservationRequest;
import dev.tmmc.reservity.reservations.dto.ReservationRequestResponse;
import dev.tmmc.reservity.reservations.dto.ReservationResponse;
import dev.tmmc.reservity.reservations.entity.RecurrencePattern;
import dev.tmmc.reservity.reservations.entity.Reservation;
import dev.tmmc.reservity.reservations.entity.ReservationRequest;
import dev.tmmc.reservity.reservations.entity.ReservationSeries;
import dev.tmmc.reservity.reservations.entity.ReservationStatus;
import dev.tmmc.reservity.reservations.entity.ReservationTag;
import dev.tmmc.reservity.reservations.entity.SavedPass;
import dev.tmmc.reservity.reservations.entity.SavedPassId;
import dev.tmmc.reservity.reservations.event.ReservationApprovedEvent;
import dev.tmmc.reservity.reservations.event.ReservationDeniedEvent;
import dev.tmmc.reservity.reservations.mapper.ReservationMapper;
import dev.tmmc.reservity.reservations.repository.ReservationRepository;
import dev.tmmc.reservity.reservations.repository.ReservationRequestRepository;
import dev.tmmc.reservity.reservations.repository.ReservationSeriesRepository;
import dev.tmmc.reservity.reservations.repository.SavedPassRepository;
import dev.tmmc.reservity.spaces.availability.DayWindow;
import dev.tmmc.reservity.spaces.availability.OperatingHours;
import dev.tmmc.reservity.spaces.availability.OperatingHoursParser;
import dev.tmmc.reservity.spaces.entity.Space;
import dev.tmmc.reservity.spaces.entity.SpaceClosure;
import dev.tmmc.reservity.spaces.entity.SpaceOwnerType;
import dev.tmmc.reservity.spaces.repository.SpaceClosureRepository;
import dev.tmmc.reservity.spaces.service.SpaceService;
import dev.tmmc.reservity.user.entity.AccountType;
import dev.tmmc.reservity.user.entity.User;
import dev.tmmc.reservity.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * The booking-lifecycle orchestrator. See plan §M5-NOW for the full algorithm.
 *
 * <p>Hot path:
 * <ol>
 *   <li>Resolve space + validate window vs operating-hours/closures/conflicts.</li>
 *   <li>Create one (or 12 weekly) {@link ReservationRequest} rows.</li>
 *   <li>If {@link Space#isInstantBook()} (default), flip to APPROVED + create
 *       {@link Reservation} + auto-pin {@link SavedPass}, publishing
 *       {@link ReservationApprovedEvent}.</li>
 *   <li>Return the seed (and, for weekly, count of generated occurrences).</li>
 * </ol>
 *
 * <p>Owner-side approve/deny re-runs the conflict pre-flight, mirrors the
 * authz check used by {@code SpaceImageService}.</p>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ReservationService {

    public static final long MAX_DURATION_HOURS = 8L;
    public static final long MAX_HORIZON_DAYS = 60L;
    public static final long PAST_GRACE_MINUTES = 5L;

    private static final ZoneId DEFAULT_ZONE = ZoneId.of("UTC");

    private final SpaceService spaceService;
    private final UserRepository userRepository;
    private final OrganizationRepository organizationRepository;
    private final OrgMembershipRepository orgMembershipRepository;

    private final SpaceClosureRepository closureRepository;
    private final ReservationRepository reservationRepository;
    private final ReservationRequestRepository requestRepository;
    private final ReservationSeriesRepository seriesRepository;
    private final SavedPassRepository savedPassRepository;

    private final ReservationCodeGenerator codeGenerator;
    private final PassTokenGenerator tokenGenerator;
    private final ReservationStateMachine stateMachine;
    private final RecurrenceExpander expander;
    private final ReservationMapper mapper;
    private final OperatingHoursParser hoursParser;
    private final ApplicationEventPublisher events;

    // ────────── submission ──────────

    @Transactional
    public ReservationResponse submit(String slugOrId, CreateReservationRequest body, UUID currentUserId) {
        validateBasicWindow(body.startsAt(), body.endsAt());

        Space space = resolve(slugOrId);
        User requester = requireUser(currentUserId);
        boolean weekly = "weekly".equalsIgnoreCase(body.recurring());

        List<RecurrenceExpander.Window> windows = weekly
                ? expander.expandWeekly(body.startsAt(), body.endsAt())
                : List.of(new RecurrenceExpander.Window(0, body.startsAt(), body.endsAt()));

        // Validate every occurrence — succeed-or-rollback as a unit.
        OperatingHours hours = hoursParser.parse(space.getOperatingHours());
        for (RecurrenceExpander.Window w : windows) {
            validateAgainstOperatingHours(hours, w.startsAt(), w.endsAt());
            validateAgainstClosures(space.getId(), w.startsAt(), w.endsAt());
            validateAgainstReservations(space.getId(), w.startsAt(), w.endsAt());
        }

        ReservationSeries series = null;
        if (weekly) {
            series = ReservationSeries.builder()
                    .space(space)
                    .requester(requester)
                    .pattern(RecurrencePattern.WEEKLY)
                    .startsOnDate(toLocalDate(body.startsAt()))
                    .endsOnDate(toLocalDate(windows.get(windows.size() - 1).endsAt()))
                    .startTime(toLocalTime(body.startsAt()))
                    .endTime(toLocalTime(body.endsAt()))
                    .build();
            series = seriesRepository.save(series);
        }

        ReservationTag tag = body.tag() != null ? ReservationTag.fromValue(body.tag()) : ReservationTag.SOLO_FOCUS;
        short attendees = body.attendeesCount() != null ? body.attendeesCount().shortValue() : (short) 1;

        ReservationRequest seed = null;
        for (RecurrenceExpander.Window w : windows) {
            ReservationRequest req = ReservationRequest.builder()
                    .reservationCode(generateUniqueCode(space.getSlug()))
                    .space(space)
                    .requester(requester)
                    .startsAt(w.startsAt())
                    .endsAt(w.endsAt())
                    .series(series)
                    .seriesOccurrenceIdx(weekly ? w.index() : null)
                    .purpose(body.purpose())
                    .attendeesCount(attendees)
                    .contactPhone(body.contactPhone())
                    .tag(tag)
                    .status(ReservationStatus.PENDING)
                    .autoApproved(false)
                    .build();
            req = requestRepository.save(req);

            if (space.isInstantBook()) {
                Reservation reservation = approveAndCreateReservation(req, requester);
                if (w.index() == 0) seed = req;
                events.publishEvent(new ReservationApprovedEvent(reservation.getId(), requester.getId(), space.getId()));
            } else if (w.index() == 0) {
                seed = req;
            }
        }

        // Reload seed with relationships set after the in-flight transaction (status was just set).
        UUID seedId = seed.getId();
        ReservationRequest finalSeed = requestRepository.findById(seedId)
                .orElseThrow(() -> new EntityNotFoundException("ReservationRequest", seedId));
        Reservation reservation = reservationRepository.findByRequestId(finalSeed.getId()).orElse(null);
        Integer occurrences = weekly ? RecurrenceExpander.WEEKLY_OCCURRENCES : null;
        return mapper.toResponse(finalSeed, reservation, true, occurrences);
    }

    /** Flips PENDING → APPROVED, creates {@code reservations} row, auto-pins saved pass. */
    private Reservation approveAndCreateReservation(ReservationRequest req, User decidedBy) {
        req.setStatus(ReservationStatus.APPROVED);
        req.setAutoApproved(true);
        req.setDecidedAt(Instant.now());
        req.setDecidedBy(decidedBy);

        Reservation reservation = Reservation.builder()
                .request(req)
                .space(req.getSpace())
                .user(req.getRequester())
                .startsAt(req.getStartsAt())
                .endsAt(req.getEndsAt())
                .passToken(tokenGenerator.generate())
                .build();
        try {
            reservation = reservationRepository.saveAndFlush(reservation);
        } catch (DataIntegrityViolationException dup) {
            throw new ReservationConflictException(
                    "Slot was taken by another booking — try a different time");
        }

        savedPassRepository.save(SavedPass.builder()
                .id(new SavedPassId(req.getRequester().getId(), reservation.getId()))
                .user(req.getRequester())
                .reservation(reservation)
                .build());
        return reservation;
    }

    // ────────── owner approve / deny ──────────

    @Transactional
    public ReservationResponse approveRequest(UUID requestId, UUID actorId) {
        ReservationRequest req = requireRequest(requestId);
        User actor = requireUser(actorId);
        requireSpaceWriteAccess(req.getSpace(), actor);

        stateMachine.requireTransition(req.getStatus(), ReservationStateMachine.Action.APPROVE);

        // Re-run conflict pre-flight (a competing booking may have landed).
        validateAgainstReservations(req.getSpace().getId(), req.getStartsAt(), req.getEndsAt());
        validateAgainstClosures(req.getSpace().getId(), req.getStartsAt(), req.getEndsAt());

        Reservation reservation = approveAndCreateReservation(req, actor);
        events.publishEvent(new ReservationApprovedEvent(reservation.getId(), req.getRequester().getId(), req.getSpace().getId()));
        return mapper.toResponse(req, reservation, false, null);
    }

    @Transactional
    public ReservationRequestResponse denyRequest(UUID requestId, UUID actorId, String reason) {
        ReservationRequest req = requireRequest(requestId);
        User actor = requireUser(actorId);
        requireSpaceWriteAccess(req.getSpace(), actor);

        stateMachine.requireTransition(req.getStatus(), ReservationStateMachine.Action.DENY);

        req.setStatus(ReservationStatus.DENIED);
        req.setDecidedAt(Instant.now());
        req.setDecidedBy(actor);
        req.setRejectionReason(reason);
        events.publishEvent(new ReservationDeniedEvent(req.getId(), req.getRequester().getId(), req.getSpace().getId(), reason));
        return mapper.toRequestResponse(req);
    }

    // ────────── cancel ──────────

    @Transactional
    public ReservationRequestResponse cancelRequest(UUID requestId, UUID actorId, String reason) {
        ReservationRequest req = requireRequest(requestId);
        User actor = requireUser(actorId);
        requireRequestVisibility(req, actor);

        stateMachine.requireTransition(req.getStatus(), ReservationStateMachine.Action.CANCEL);

        if (req.getStatus() == ReservationStatus.APPROVED) {
            // Drop the blocking row + its saved pass so the slot reopens.
            Optional<Reservation> existing = reservationRepository.findByRequestId(req.getId());
            existing.ifPresent(r -> {
                savedPassRepository.deleteByReservationId(r.getId());
                reservationRepository.delete(r);
            });
        }

        req.setStatus(ReservationStatus.CANCELLED);
        req.setCancelledAt(Instant.now());
        req.setCancelledBy(actor);
        req.setCancellationReason(reason);
        return mapper.toRequestResponse(req);
    }

    // ────────── reads ──────────

    @Transactional(readOnly = true)
    public PageResponse<ReservationRequestResponse> listMineRequests(UUID userId, ReservationStatus status, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<ReservationRequest> p = (status == null)
                ? requestRepository.findByRequesterIdOrderByCreatedAtDesc(userId, pageable)
                : requestRepository.findByRequesterIdAndStatusOrderByCreatedAtDesc(userId, status, pageable);
        return PageResponse.of(p.map(mapper::toRequestResponse));
    }

    @Transactional(readOnly = true)
    public PageResponse<ReservationRequestResponse> listReceivedRequests(UUID ownerUserId, ReservationStatus status, int page, int size) {
        User actor = requireUser(ownerUserId);
        List<UUID> spaceIds = collectOwnedSpaceIds(actor);
        Pageable pageable = PageRequest.of(page, size);
        if (spaceIds.isEmpty()) return PageResponse.of(Page.empty(pageable));
        Page<ReservationRequest> p = (status == null)
                ? requestRepository.findReceivedForSpaces(spaceIds, pageable)
                : requestRepository.findReceivedForSpacesByStatus(spaceIds, status, pageable);
        return PageResponse.of(p.map(mapper::toRequestResponse));
    }

    @Transactional(readOnly = true)
    public ReservationRequestResponse getRequest(UUID requestId, UUID actorId) {
        ReservationRequest req = requireRequest(requestId);
        requireRequestVisibility(req, requireUser(actorId));
        return mapper.toRequestResponse(req);
    }

    @Transactional(readOnly = true)
    public PageResponse<ReservationResponse> listAllReservations(int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "startsAt"));
        Page<Reservation> p = reservationRepository.findAllDetailed(pageable);
        return PageResponse.of(p.map(r -> mapper.toResponse(r.getRequest(), r, false, null)));
    }


    @Transactional(readOnly = true)
    public PageResponse<ReservationResponse> listMineReservations(UUID userId, boolean upcomingOnly, int page, int size) {

        Pageable pageable = PageRequest.of(page, size);
        Page<Reservation> p = upcomingOnly
                ? reservationRepository.findByUserIdAndStartsAtGreaterThanEqualOrderByStartsAtAsc(userId, Instant.now(), pageable)
                : reservationRepository.findByUserIdOrderByStartsAtDesc(userId, pageable);
        return PageResponse.of(p.map(r -> mapper.toResponse(r.getRequest(), r, true, null)));
    }

    @Transactional(readOnly = true)
    public ReservationResponse getReservation(UUID reservationId, UUID actorId) {
        Reservation r = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new EntityNotFoundException("Reservation", reservationId));
        User actor = requireUser(actorId);
        boolean isOwner = r.getUser().getId().equals(actor.getId());
        boolean isAdmin = actor.getAccountType() == AccountType.ADMIN;
        if (!isOwner && !isAdmin && !canManageSpace(r.getSpace(), actor)) {
            throw new ForbiddenOperationException("You can't view this reservation");
        }
        return mapper.toResponse(r.getRequest(), r, isOwner || isAdmin, null);
    }

    @Transactional(readOnly = true)
    public dev.tmmc.reservity.reservations.dto.PassResponse getPass(UUID reservationId, UUID actorId) {
        Reservation r = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new EntityNotFoundException("Reservation", reservationId));
        User actor = requireUser(actorId);
        boolean isOwner = r.getUser().getId().equals(actor.getId());
        boolean isAdmin = actor.getAccountType() == AccountType.ADMIN;
        if (!isOwner && !isAdmin) {
            throw new ForbiddenOperationException("Pass tokens are only visible to the booking holder");
        }
        if (r.isPassRevoked()) {
            throw new ResponseStatusException(HttpStatus.GONE, "Pass has been revoked");
        }
        return mapper.toPassResponse(r);
    }

    // ────────── validation helpers ──────────

    private void validateBasicWindow(Instant startsAt, Instant endsAt) {
        if (startsAt == null || endsAt == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "startsAt and endsAt are required");
        }
        if (!endsAt.isAfter(startsAt)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "endsAt must be after startsAt");
        }
        Duration d = Duration.between(startsAt, endsAt);
        if (d.toHours() > MAX_DURATION_HOURS || (d.toHours() == MAX_DURATION_HOURS && d.toMinutesPart() > 0)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Window too long — max " + MAX_DURATION_HOURS + " hours");
        }
        Instant now = Instant.now();
        if (startsAt.isBefore(now.minus(Duration.ofMinutes(PAST_GRACE_MINUTES)))) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "startsAt is in the past");
        }
        if (startsAt.isAfter(now.plus(Duration.ofDays(MAX_HORIZON_DAYS)))) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "startsAt is more than " + MAX_HORIZON_DAYS + " days ahead");
        }
    }

    private void validateAgainstOperatingHours(OperatingHours hours, Instant startsAt, Instant endsAt) {
        ZonedDateTime startZdt = ZonedDateTime.ofInstant(startsAt, DEFAULT_ZONE);
        ZonedDateTime endZdt = ZonedDateTime.ofInstant(endsAt, DEFAULT_ZONE);
        // M5 keeps it simple: require the window to fall within a single operating-hours window
        // on the start date. (Cross-midnight bookings would require multi-day window logic — out of scope.)
        if (!startZdt.toLocalDate().equals(endZdt.toLocalDate())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Booking must not cross midnight");
        }
        LocalDate day = startZdt.toLocalDate();
        LocalTime startT = startZdt.toLocalTime();
        LocalTime endT = endZdt.toLocalTime();
        for (DayWindow w : hours.windowsFor(day)) {
            LocalTime open = w.open();
            LocalTime close = w.close().equals(LocalTime.MIDNIGHT) ? LocalTime.MAX : w.close();
            if (!startT.isBefore(open) && !endT.isAfter(close)) {
                return;
            }
        }
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                "Outside operating hours");
    }

    private void validateAgainstClosures(UUID spaceId, Instant startsAt, Instant endsAt) {
        List<SpaceClosure> closures = closureRepository.findOverlapping(spaceId, startsAt, endsAt);
        if (!closures.isEmpty()) {
            throw new ReservationConflictException("Space is closed for this window");
        }
    }

    private void validateAgainstReservations(UUID spaceId, Instant startsAt, Instant endsAt) {
        List<Reservation> overlapping = reservationRepository.findOverlapping(spaceId, startsAt, endsAt);
        if (!overlapping.isEmpty()) {
            throw new ReservationConflictException("Slot already taken");
        }
    }

    /** Loop until reservation_code is unique (DB UNIQUE catches collisions; loop bounded). */
    private String generateUniqueCode(String slug) {
        for (int attempt = 0; attempt < 5; attempt++) {
            String code = codeGenerator.generate(slug);
            if (!requestRepository.existsByReservationCode(code)) return code;
        }
        // Should be effectively impossible (1M codes per slug, 5 attempts → ~10^-30 chance of collision).
        throw new ReservationConflictException("Could not generate a unique reservation code");
    }

    // ────────── authz helpers (mirror SpaceImageService) ──────────

    private void requireSpaceWriteAccess(Space space, User actor) {
        if (canManageSpace(space, actor)) return;
        throw new ForbiddenOperationException("Only the space owner can do this");
    }

    private boolean canManageSpace(Space space, User actor) {
        if (actor.getAccountType() == AccountType.ADMIN) return true;
        if (space.getOwnerType() == SpaceOwnerType.USER) {
            return space.getOwnerId().equals(actor.getId());
        }
        if (space.getOwnerType() == SpaceOwnerType.ORGANIZATION) {
            Optional<Organization> org = organizationRepository.findById(space.getOwnerId());
            return org.isPresent() && orgMembershipRepository.existsByUserAndOrganization(actor, org.get());
        }
        return false;
    }

    private void requireRequestVisibility(ReservationRequest req, User actor) {
        if (actor.getAccountType() == AccountType.ADMIN) return;
        if (req.getRequester().getId().equals(actor.getId())) return;
        if (canManageSpace(req.getSpace(), actor)) return;
        throw new ForbiddenOperationException("You can't view or modify this request");
    }

    private List<UUID> collectOwnedSpaceIds(User actor) {
        // Lightweight: resolve via SpaceService rather than yet another query; it already
        // honors @EntityGraph for `building` etc.
        // For owner-side queue we just need the id list of every space this user can manage.
        // Spaces owned directly by the user:
        // (No dedicated repo method exists yet; use a simple JPQL via SpaceService.requireOwnedBy)
        return spaceService.findManageableSpaceIds(actor);
    }

    // ────────── tiny utilities ──────────

    private Space resolve(String slugOrId) {
        try {
            UUID id = UUID.fromString(slugOrId);
            return spaceService.requireById(id);
        } catch (IllegalArgumentException notUuid) {
            return spaceService.requireBySlug(slugOrId);
        }
    }

    private User requireUser(UUID userId) {
        if (userId == null) throw new EntityNotFoundException("Not authenticated");
        return userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User", userId));
    }

    private ReservationRequest requireRequest(UUID id) {
        return requestRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("ReservationRequest", id));
    }

    private static LocalDate toLocalDate(Instant i) {
        return ZonedDateTime.ofInstant(i, DEFAULT_ZONE).toLocalDate();
    }

    private static LocalTime toLocalTime(Instant i) {
        return ZonedDateTime.ofInstant(i, DEFAULT_ZONE).toLocalTime();
    }
}
