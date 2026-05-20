package dev.tmmc.reservity.user.entity;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;
import java.time.LocalTime;
import java.util.UUID;

@Entity
@Table(name = "user_settings")
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserSettings {

    @Id
    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @MapsId
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id")
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 8)
    private Theme theme;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 8)
    private Accent accent;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 12)
    private Density density;

    @Column(name = "ornament_enabled", nullable = false)
    private boolean ornamentEnabled;

    @Column(name = "reduce_motion", nullable = false)
    private boolean reduceMotion;

    @Column(name = "high_contrast", nullable = false)
    private boolean highContrast;

    @Column(name = "email_on_request_decided", nullable = false)
    private boolean emailOnRequestDecided;

    @Column(name = "email_on_new_message", nullable = false)
    private boolean emailOnNewMessage;

    @Column(name = "email_on_event_reminder", nullable = false)
    private boolean emailOnEventReminder;

    @Column(name = "email_on_security_alert", nullable = false)
    private boolean emailOnSecurityAlert;

    @Column(name = "push_enabled", nullable = false)
    private boolean pushEnabled;

    @Column(name = "quiet_hours_start")
    private LocalTime quietHoursStart;

    @Column(name = "quiet_hours_end")
    private LocalTime quietHoursEnd;

    @Column(nullable = false, length = 8)
    private String locale;

    @Column(nullable = false, length = 64)
    private String timezone;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public static UserSettings defaultsFor(User user) {
        return UserSettings.builder()
                .user(user)
                .theme(Theme.auto)
                .accent(Accent.indigo)
                .density(Density.comfortable)
                .ornamentEnabled(true)
                .reduceMotion(false)
                .highContrast(false)
                .emailOnRequestDecided(true)
                .emailOnNewMessage(false)
                .emailOnEventReminder(true)
                .emailOnSecurityAlert(true)
                .pushEnabled(false)
                .locale("en")
                .timezone("UTC")
                .build();
    }
}
