package dev.tmmc.reservity.notifications.service;

import dev.tmmc.reservity.reservations.entity.Reservation;
import dev.tmmc.reservity.reservations.entity.ReservationRequest;
import dev.tmmc.reservity.reservations.repository.ReservationRepository;
import dev.tmmc.reservity.reservations.repository.ReservationRequestRepository;
import dev.tmmc.reservity.spaces.entity.Space;
import dev.tmmc.reservity.spaces.repository.SpaceRepository;
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
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.UUID;

/**
 * Async email sender. Lives in a separate bean from {@code ReservationNotificationListener}
 * so the {@code @Async} proxy boundary is real (calling an {@code @Async} method
 * from inside the same bean bypasses the proxy).
 *
 * <p>Each method:
 * <ol>
 *   <li>Loads {@code User}, {@code UserSettings}, and the source entity.
 *   <li>Returns early if {@code !settings.emailOnRequestDecided}.
 *   <li>Returns early if {@code QuietHoursChecker} reports inside quiet hours.
 *   <li>Renders Thymeleaf and dispatches via {@link JavaMailSender}.
 *   <li>Logs WARN on failure — never throws (would have nowhere to land in the
 *       async pool's uncaught-exception path).
 * </ol>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private final JavaMailSender mailSender;
    private final SpringTemplateEngine templateEngine;
    private final UserRepository userRepository;
    private final UserSettingsRepository settingsRepository;
    private final ReservationRepository reservationRepository;
    private final ReservationRequestRepository requestRepository;
    private final SpaceRepository spaceRepository;
    private final QuietHoursChecker quietHoursChecker;

    @Value("${reservity.mail.from:noreply@reservity.local}")
    private String fromAddress;

    @Async("notificationExecutor")
    @Transactional(readOnly = true)
    public void sendApprovalEmail(UUID userId, UUID reservationId, UUID spaceId) {
        try {
            User user = userRepository.findById(userId).orElse(null);
            if (user == null) { log.warn("Approval email skipped: user {} not found", userId); return; }
            UserSettings settings = settingsRepository.findById(userId).orElse(null);
            if (settings == null) { log.warn("Approval email skipped: settings missing for user {}", userId); return; }
            if (!settings.isEmailOnRequestDecided()) return;
            if (quietHoursChecker.inQuietHours(settings, Instant.now())) return;

            Reservation r = reservationRepository.findById(reservationId).orElse(null);
            if (r == null) { log.warn("Approval email skipped: reservation {} not found", reservationId); return; }
            Space space = spaceRepository.findById(spaceId).orElse(null);
            if (space == null) { log.warn("Approval email skipped: space {} not found", spaceId); return; }

            ReservationRequest req = r.getRequest();
            String code = req.getReservationCode();
            String startsAtLocal = formatLocal(r.getStartsAt(), settings);

            Context ctx = new Context();
            ctx.setVariable("displayName", user.getDisplayName());
            ctx.setVariable("spaceName", space.getName());
            ctx.setVariable("reservationCode", code);
            ctx.setVariable("startsAtLocal", startsAtLocal);
            ctx.setVariable("spaceUrl", "/spaces/" + space.getSlug());

            String html = templateEngine.process("email/reservation-approved", ctx);
            send(user.getEmail(), "Your booking is confirmed", html);
        } catch (Exception e) {
            log.warn("sendApprovalEmail failed for user {}: {}", userId, e.getMessage());
        }
    }

    @Async("notificationExecutor")
    @Transactional(readOnly = true)
    public void sendDenialEmail(UUID userId, UUID requestId, UUID spaceId, String reason) {
        try {
            User user = userRepository.findById(userId).orElse(null);
            if (user == null) { log.warn("Denial email skipped: user {} not found", userId); return; }
            UserSettings settings = settingsRepository.findById(userId).orElse(null);
            if (settings == null) { log.warn("Denial email skipped: settings missing for user {}", userId); return; }
            if (!settings.isEmailOnRequestDecided()) return;
            if (quietHoursChecker.inQuietHours(settings, Instant.now())) return;

            ReservationRequest req = requestRepository.findById(requestId).orElse(null);
            if (req == null) { log.warn("Denial email skipped: request {} not found", requestId); return; }
            Space space = spaceRepository.findById(spaceId).orElse(null);
            if (space == null) { log.warn("Denial email skipped: space {} not found", spaceId); return; }

            String startsAtLocal = formatLocal(req.getStartsAt(), settings);

            Context ctx = new Context();
            ctx.setVariable("displayName", user.getDisplayName());
            ctx.setVariable("spaceName", space.getName());
            ctx.setVariable("reservationCode", req.getReservationCode());
            ctx.setVariable("startsAtLocal", startsAtLocal);
            ctx.setVariable("reason", reason);
            ctx.setVariable("spaceUrl", "/spaces/" + space.getSlug());

            String html = templateEngine.process("email/reservation-denied", ctx);
            send(user.getEmail(), "Your booking request was declined", html);
        } catch (Exception e) {
            log.warn("sendDenialEmail failed for user {}: {}", userId, e.getMessage());
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

    private static String formatLocal(Instant instant, UserSettings settings) {
        ZoneId zone = ZoneId.of(settings.getTimezone() == null ? "UTC" : settings.getTimezone());
        Locale locale = Locale.forLanguageTag(settings.getLocale() == null ? "en" : settings.getLocale());
        return DateTimeFormatter
                .ofPattern("yyyy-MM-dd HH:mm '('zzz')'", locale)
                .format(instant.atZone(zone));
    }
}
