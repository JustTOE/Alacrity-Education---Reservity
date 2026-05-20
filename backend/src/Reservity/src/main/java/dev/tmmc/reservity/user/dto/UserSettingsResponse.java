package dev.tmmc.reservity.user.dto;

import dev.tmmc.reservity.user.entity.Accent;
import dev.tmmc.reservity.user.entity.Density;
import dev.tmmc.reservity.user.entity.Theme;

import java.time.LocalTime;

public record UserSettingsResponse(
        Theme theme,
        Accent accent,
        Density density,
        boolean ornamentEnabled,
        boolean reduceMotion,
        boolean highContrast,
        boolean emailOnRequestDecided,
        boolean emailOnNewMessage,
        boolean emailOnEventReminder,
        boolean emailOnSecurityAlert,
        boolean pushEnabled,
        LocalTime quietHoursStart,
        LocalTime quietHoursEnd,
        String locale,
        String timezone
) {}
