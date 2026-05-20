package dev.tmmc.reservity.reservations.mapper;

import dev.tmmc.reservity.reservations.dto.PassResponse;
import dev.tmmc.reservity.reservations.dto.ReservationRequestResponse;
import dev.tmmc.reservity.reservations.dto.ReservationResponse;
import dev.tmmc.reservity.reservations.entity.Reservation;
import dev.tmmc.reservity.reservations.entity.ReservationRequest;
import dev.tmmc.reservity.spaces.dto.SpaceResponse;
import dev.tmmc.reservity.spaces.dto.SpaceSummary;
import dev.tmmc.reservity.spaces.entity.SpaceImage;
import dev.tmmc.reservity.spaces.mapper.SpaceMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Comparator;

@Component
@RequiredArgsConstructor
public class ReservationMapper {

    private final SpaceMapper spaceMapper;

    /**
     * Map a {@link ReservationRequest} → frontend {@link ReservationResponse}.
     * @param reservation the linked reservation if APPROVED, else null
     * @param includePassToken whether the caller is the requester (or admin) — controls whether passToken is exposed
     * @param seriesOccurrences when this is the seed of a weekly series, the total
     *                          generated count; otherwise null
     */
    public ReservationResponse toResponse(ReservationRequest req,
                                          Reservation reservation,
                                          boolean includePassToken,
                                          Integer seriesOccurrences) {
        return new ReservationResponse(
                req.getReservationCode(),
                req.getId(),
                reservation == null ? null : reservation.getId(),
                spaceMapper.toSummary(req.getSpace(), primaryUrl(req)),
                req.getStartsAt(),
                req.getEndsAt(),
                req.getStatus().name(),
                req.isAutoApproved(),
                (reservation != null && includePassToken) ? reservation.getPassToken() : null,
                req.getCreatedAt(),
                req.getDecidedAt(),
                req.getSeries() != null ? "weekly" : "none",
                req.getSeries() != null ? req.getSeries().getId() : null,
                seriesOccurrences
        );
    }

    public ReservationRequestResponse toRequestResponse(ReservationRequest req) {
        return new ReservationRequestResponse(
                req.getId(),
                req.getReservationCode(),
                spaceMapper.toSummary(req.getSpace(), primaryUrl(req)),
                req.getRequester() != null ? req.getRequester().getId() : null,
                req.getRequester() != null ? req.getRequester().getDisplayName() : null,
                req.getStartsAt(),
                req.getEndsAt(),
                req.getSeries() != null ? "weekly" : "none",
                req.getSeries() != null ? req.getSeries().getId() : null,
                req.getSeriesOccurrenceIdx(),
                req.getPurpose(),
                req.getAttendeesCount(),
                req.getContactPhone(),
                req.getTag() != null ? req.getTag().value() : null,
                req.getStatus().name(),
                req.isAutoApproved(),
                req.getDecidedAt(),
                req.getDecidedBy() != null ? req.getDecidedBy().getId() : null,
                req.getRejectionReason(),
                req.getCancelledAt(),
                req.getCancelledBy() != null ? req.getCancelledBy().getId() : null,
                req.getCancellationReason(),
                req.getCreatedAt(),
                req.getUpdatedAt()
        );
    }

    public PassResponse toPassResponse(Reservation r) {
        SpaceResponse spaceDto = spaceMapper.toResponse(r.getSpace());
        return new PassResponse(
                r.getId(),
                r.getRequest().getReservationCode(),
                spaceDto,
                r.getStartsAt(),
                r.getEndsAt(),
                r.getPassToken(),
                r.isPassRevoked(),
                r.getCheckinAt(),
                r.getCheckoutAt()
        );
    }

    private static String primaryUrl(ReservationRequest req) {
        if (req.getSpace() == null || req.getSpace().getImages() == null) return null;
        return req.getSpace().getImages().stream()
                .min(Comparator.comparingInt(SpaceImage::getDisplayOrder))
                .map(SpaceImage::getUrl)
                .orElse(null);
    }
}
