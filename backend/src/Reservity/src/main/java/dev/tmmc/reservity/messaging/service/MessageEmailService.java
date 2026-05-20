package dev.tmmc.reservity.messaging.service;

import dev.tmmc.reservity.notifications.service.QuietHoursChecker;
import dev.tmmc.reservity.user.entity.User;
import dev.tmmc.reservity.user.entity.UserSettings;
import dev.tmmc.reservity.user.repository.UserRepository;
import dev.tmmc.reservity.user.repository.UserSettingsRepository;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

import java.time.Instant;
import java.util.UUID;

/**
 * Async dispatcher for "you have a new message" emails. Lives in a separate
 * bean from {@code MessageNotificationListener} so the {@code @Async} proxy
 * boundary is real (calling {@code @Async} from inside the same bean bypasses
 * the proxy).
 *
 * <p>Honors {@code userSettings.emailOnNewMessage} and quiet hours. SMTP
 * failures log WARN and never propagate — message send must succeed even if
 * mail is flaky.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class MessageEmailService {

    private final JavaMailSender mailSender;
    private final SpringTemplateEngine templateEngine;
    private final UserRepository userRepository;
    private final UserSettingsRepository settingsRepository;
    private final QuietHoursChecker quietHoursChecker;

    @Value("${reservity.mail.from:noreply@reservity.local}")
    private String fromAddress;

    @Async("notificationExecutor")
    @Transactional(readOnly = true)
    public void sendMessageReceivedEmail(UUID recipientId,
                                         UUID conversationId,
                                         String senderName,
                                         String contentPreview) {
        try {
            User recipient = userRepository.findById(recipientId).orElse(null);
            if (recipient == null) {
                log.warn("Message email skipped: user {} not found", recipientId);
                return;
            }
            UserSettings settings = settingsRepository.findById(recipientId).orElse(null);
            if (settings == null) {
                log.warn("Message email skipped: settings missing for user {}", recipientId);
                return;
            }
            if (!settings.isEmailOnNewMessage()) return;
            if (quietHoursChecker.inQuietHours(settings, Instant.now())) return;

            Context ctx = new Context();
            ctx.setVariable("senderName", senderName);
            ctx.setVariable("contentPreview", contentPreview);
            ctx.setVariable("conversationUrl", "/messages/" + conversationId);

            String html = templateEngine.process("email/message-received", ctx);
            send(recipient.getEmail(), "New message from " + senderName, html);
        } catch (Exception e) {
            log.warn("sendMessageReceivedEmail failed for user {}: {}", recipientId, e.getMessage());
        }
    }

    private void send(String toAddress, String subject, String html) {
        try {
            MimeMessage msg = mailSender.createMimeMessage();
            MimeMessageHelper h = new MimeMessageHelper(msg, "UTF-8");
            h.setFrom(fromAddress);
            h.setTo(toAddress);
            h.setSubject(subject);
            h.setText(html, true);
            mailSender.send(msg);
        } catch (MailException | MessagingException e) {
            log.warn("Mail send to {} failed: {}", toAddress, e.getMessage());
        }
    }
}
