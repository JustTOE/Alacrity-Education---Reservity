package dev.tmmc.reservity.user.service;

import dev.tmmc.reservity.common.exception.EntityNotFoundException;
import dev.tmmc.reservity.user.dto.UserSettingsResponse;
import dev.tmmc.reservity.user.dto.UserSettingsUpdateRequest;
import dev.tmmc.reservity.user.entity.User;
import dev.tmmc.reservity.user.entity.UserSettings;
import dev.tmmc.reservity.user.repository.UserRepository;
import dev.tmmc.reservity.user.repository.UserSettingsRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserSettingsService {

    private final UserSettingsRepository userSettingsRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public UserSettingsResponse get(UUID userId) {
        return toResponse(loadOrCreate(userId));
    }

    @Transactional
    public UserSettingsResponse update(UUID userId, UserSettingsUpdateRequest req) {
        UserSettings s = loadOrCreate(userId);
        if (req.theme() != null) s.setTheme(req.theme());
        if (req.accent() != null) s.setAccent(req.accent());
        if (req.density() != null) s.setDensity(req.density());
        if (req.ornamentEnabled() != null) s.setOrnamentEnabled(req.ornamentEnabled());
        if (req.reduceMotion() != null) s.setReduceMotion(req.reduceMotion());
        if (req.highContrast() != null) s.setHighContrast(req.highContrast());
        if (req.emailOnRequestDecided() != null) s.setEmailOnRequestDecided(req.emailOnRequestDecided());
        if (req.emailOnNewMessage() != null) s.setEmailOnNewMessage(req.emailOnNewMessage());
        if (req.emailOnEventReminder() != null) s.setEmailOnEventReminder(req.emailOnEventReminder());
        if (req.emailOnSecurityAlert() != null) s.setEmailOnSecurityAlert(req.emailOnSecurityAlert());
        if (req.pushEnabled() != null) s.setPushEnabled(req.pushEnabled());
        if (req.quietHoursStart() != null) s.setQuietHoursStart(req.quietHoursStart());
        if (req.quietHoursEnd() != null) s.setQuietHoursEnd(req.quietHoursEnd());
        if (req.locale() != null) s.setLocale(req.locale());
        if (req.timezone() != null) s.setTimezone(req.timezone());
        return toResponse(userSettingsRepository.save(s));
    }

    private UserSettings loadOrCreate(UUID userId) {
        return userSettingsRepository.findById(userId).orElseGet(() -> {
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new EntityNotFoundException("User not found"));
            return userSettingsRepository.save(UserSettings.defaultsFor(user));
        });
    }

    private UserSettingsResponse toResponse(UserSettings s) {
        return new UserSettingsResponse(
                s.getTheme(), s.getAccent(), s.getDensity(),
                s.isOrnamentEnabled(), s.isReduceMotion(), s.isHighContrast(),
                s.isEmailOnRequestDecided(), s.isEmailOnNewMessage(),
                s.isEmailOnEventReminder(), s.isEmailOnSecurityAlert(),
                s.isPushEnabled(), s.getQuietHoursStart(), s.getQuietHoursEnd(),
                s.getLocale(), s.getTimezone()
        );
    }
}
