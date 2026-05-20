package dev.tmmc.reservity.user.dto;

import dev.tmmc.reservity.user.entity.Accent;
import dev.tmmc.reservity.user.entity.Density;
import dev.tmmc.reservity.user.entity.Theme;
import jakarta.validation.constraints.Size;

import java.time.LocalTime;

public record UserSettingsUpdateRequest(
        Theme theme,
        Accent accent,
        Density density,
        Boolean ornamentEnabled,
        Boolean reduceMotion,
        Boolean highContrast,
        Boolean emailOnRequestDecided,
        Boolean emailOnEventReminder,
        Boolean emailOnSecurityAlert,
        Boolean pushEnabled,
        LocalTime quietHoursStart,
        LocalTime quietHoursEnd,
        @Size(max = 8) String locale,
        @Size(max = 64) String timezone
) {}
