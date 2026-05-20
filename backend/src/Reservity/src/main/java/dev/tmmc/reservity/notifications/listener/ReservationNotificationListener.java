package dev.tmmc.reservity.notifications.listener;

import dev.tmmc.reservity.notifications.entity.enums.NotificationType;
import dev.tmmc.reservity.notifications.service.EmailService;
import dev.tmmc.reservity.notifications.service.NotificationService;
import dev.tmmc.reservity.reservations.entity.Reservation;
import dev.tmmc.reservity.reservations.entity.ReservationRequest;
import dev.tmmc.reservity.reservations.event.ReservationApprovedEvent;
import dev.tmmc.reservity.reservations.event.ReservationDeniedEvent;
import dev.tmmc.reservity.reservations.repository.ReservationRepository;
import dev.tmmc.reservity.reservations.repository.ReservationRequestRepository;
import dev.tmmc.reservity.spaces.entity.Space;
import dev.tmmc.reservity.spaces.repository.SpaceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Wires reservation lifecycle events to in-app notifications + email.
 *
 * <p>Uses {@code @TransactionalEventListener(AFTER_COMMIT)} so the listener
 * only fires after the booking transaction commits. The listener opens its
 * own {@code REQUIRES_NEW} transaction for the in-app row insert (the
 * publishing transaction is already gone by AFTER_COMMIT). The email send
 * is fire-and-forget on the {@code notificationExecutor} pool so a slow
 * SMTP server cannot back-pressure the listener thread.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ReservationNotificationListener {

    private final NotificationService notificationService;
    private final EmailService emailService;
    private final ReservationRepository reservationRepository;
    private final ReservationRequestRepository requestRepository;
    private final SpaceRepository spaceRepository;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void onApproved(ReservationApprovedEvent ev) {
        Reservation r = reservationRepository.findById(ev.reservationId()).orElse(null);
        if (r == null) {
            log.warn("Reservation {} vanished before notification fired", ev.reservationId());
            return;
        }
        Space space = spaceRepository.findById(ev.spaceId()).orElse(null);
        if (space == null) {
            log.warn("Space {} vanished before approval notification", ev.spaceId());
            return;
        }
        String code = r.getRequest().getReservationCode();

        notificationService.createInApp(
                ev.requesterId(),
                NotificationType.RESERVATION_APPROVED,
                "Booking confirmed: " + space.getName(),
                "Your reservation " + code + " is locked in.",
                "/spaces/" + space.getSlug(),
                "RESERVATION",
                r.getId());

        emailService.sendApprovalEmail(ev.requesterId(), r.getId(), space.getId());
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void onDenied(ReservationDeniedEvent ev) {
        ReservationRequest req = requestRepository.findById(ev.requestId()).orElse(null);
        if (req == null) {
            log.warn("Request {} vanished before denial notification", ev.requestId());
            return;
        }
        Space space = spaceRepository.findById(ev.spaceId()).orElse(null);
        if (space == null) {
            log.warn("Space {} vanished before denial notification", ev.spaceId());
            return;
        }

        String body = ev.reason() == null
                ? "Your request was declined."
                : "Reason: " + ev.reason();

        notificationService.createInApp(
                ev.requesterId(),
                NotificationType.RESERVATION_DENIED,
                "Booking declined: " + space.getName(),
                body,
                "/spaces/" + space.getSlug(),
                "RESERVATION_REQUEST",
                req.getId());

        emailService.sendDenialEmail(ev.requesterId(), req.getId(), space.getId(), ev.reason());
    }
}
