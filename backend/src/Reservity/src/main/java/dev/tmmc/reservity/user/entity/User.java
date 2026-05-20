package dev.tmmc.reservity.user.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "users")
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(updatable = false, nullable = false)
    private UUID id;

    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(nullable = false, unique = true, columnDefinition = "CITEXT")
    private String email;

    @Column(name = "email_verified_at")
    private Instant emailVerifiedAt;

    @JsonIgnore
    @Column(name = "password_hash", nullable = false, length = 60)
    private String passwordHash;

    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(nullable = false, unique = true, columnDefinition = "CITEXT")
    private String handle;

    @Column(name = "display_name", nullable = false, length = 80)
    private String displayName;

    @Column(name = "real_name", length = 120)
    private String realName;

    @Column(nullable = false, length = 4)
    private String initials;

    @Enumerated(EnumType.STRING)
    @Column(name = "account_type", nullable = false, length = 16)
    private AccountType accountType;

    @Column(name = "verified_student", nullable = false)
    private boolean verifiedStudent;

    @Column(length = 500)
    private String bio;

    @Column(name = "avatar_url")
    private String avatarUrl;

    @Column(name = "cover_gradient", nullable = false, length = 120)
    private String coverGradient;

    @Column(name = "member_since", nullable = false)
    private LocalDate memberSince;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    @Column(name = "failed_login_attempts", nullable = false)
    private int failedLoginAttempts;

    @Column(name = "locked_until")
    private Instant lockedUntil;

    @Column(name = "last_login_at")
    private Instant lastLoginAt;

    @Column(name = "two_fa_enabled", nullable = false)
    private boolean twoFaEnabled;

    @JsonIgnore
    @Column(name = "two_fa_secret", length = 64)
    private String twoFaSecret;

    @Column(name = "two_fa_backup_codes_left", nullable = false)
    private short twoFaBackupCodesLeft;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    void onCreate() {
        if (accountType == null) accountType = AccountType.REQUESTER;
        if (coverGradient == null) coverGradient = "linear-gradient(135deg,#4b52a7,#ff823c)";
        if (memberSince == null) memberSince = LocalDate.now();
    }

    public boolean isLocked() {
        return lockedUntil != null && lockedUntil.isAfter(Instant.now());
    }

    public boolean isDeleted() {
        return deletedAt != null;
    }
}
