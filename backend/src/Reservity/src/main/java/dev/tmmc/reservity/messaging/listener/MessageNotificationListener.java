package dev.tmmc.reservity.messaging.listener;

import dev.tmmc.reservity.messaging.event.MessageSentEvent;
import dev.tmmc.reservity.messaging.service.MessageEmailService;
import dev.tmmc.reservity.notifications.entity.enums.NotificationType;
import dev.tmmc.reservity.notifications.service.NotificationService;
import dev.tmmc.reservity.user.entity.User;
import dev.tmmc.reservity.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Wires {@link MessageSentEvent} to in-app notifications + email. Mirrors the
 * M6 {@code ReservationNotificationListener} pattern verbatim:
 * {@code AFTER_COMMIT} + {@code REQUIRES_NEW} for the in-app row,
 * {@code @Async} for email so SMTP cannot back-pressure the listener.
 *
 * <p>The recipient is exactly the OTHER participant in the conversation —
 * the publisher pre-resolves it. The sender does NOT get a notification
 * about their own message.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class MessageNotificationListener {

    private final NotificationService notificationService;
    private final MessageEmailService messageEmailService;
    private final UserRepository userRepository;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void onMessageSent(MessageSentEvent ev) {
        User sender = userRepository.findById(ev.senderId()).orElse(null);
        if (sender == null) {
            log.warn("Sender {} vanished before message notification", ev.senderId());
            return;
        }

        notificationService.createInApp(
                ev.recipientId(),
                NotificationType.MESSAGE_RECEIVED,
                "New message from " + sender.getDisplayName(),
                ev.contentPreview(),
                "/messages/" + ev.conversationId(),
                "CONVERSATION",
                ev.conversationId());

        messageEmailService.sendMessageReceivedEmail(
                ev.recipientId(),
                ev.conversationId(),
                sender.getDisplayName(),
                ev.contentPreview());
    }
}
