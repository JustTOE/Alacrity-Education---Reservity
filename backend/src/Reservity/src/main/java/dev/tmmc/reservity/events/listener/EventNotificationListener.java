package dev.tmmc.reservity.events.listener;

import dev.tmmc.reservity.events.entity.Event;
import dev.tmmc.reservity.events.event.EventCancelledEvent;
import dev.tmmc.reservity.events.event.RsvpPromotedEvent;
import dev.tmmc.reservity.events.repository.EventRepository;
import dev.tmmc.reservity.notifications.entity.enums.NotificationType;
import dev.tmmc.reservity.notifications.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.UUID;

/**
 * Listens for committed event lifecycle changes and writes in-app
 * notifications. Pattern mirrors M6/M7:
 * {@code @TransactionalEventListener(AFTER_COMMIT) + @Transactional(REQUIRES_NEW)}
 * so a notification only exists after the originating tx commits.
 *
 * <p>M8 ships in-app only — no email path. The deferred reminder cron
 * will use the existing {@code email_on_event_reminder} preference.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class EventNotificationListener {

    private final NotificationService notificationService;
    private final EventRepository eventRepository;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void onEventCancelled(EventCancelledEvent ev) {
        Event event = eventRepository.findById(ev.eventId()).orElse(null);
        if (event == null) {
            log.warn("Event {} vanished before cancellation notification fired", ev.eventId());
            return;
        }
        String reason = event.getCancellationReason();
        String body = (reason == null || reason.isBlank())
                ? "The host cancelled this event."
                : "Reason: " + reason;
        for (UUID userId : ev.notifyUserIds()) {
            notificationService.createInApp(
                    userId,
                    NotificationType.EVENT_CANCELLED,
                    "Event cancelled: " + event.getTitle(),
                    body,
                    "/events/" + event.getSlug(),
                    "EVENT",
                    event.getId());
        }
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void onRsvpPromoted(RsvpPromotedEvent ev) {
        notificationService.createInApp(
                ev.promotedUserId(),
                NotificationType.EVENT_RSVP_PROMOTED,
                "You're in: " + ev.eventTitle(),
                "A spot opened up — you've been moved off the waitlist.",
                "/events/" + ev.eventId(),
                "EVENT",
                ev.eventId());
    }
}
