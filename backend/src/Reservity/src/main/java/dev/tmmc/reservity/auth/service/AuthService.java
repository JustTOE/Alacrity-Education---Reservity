package dev.tmmc.reservity.auth.service;

import dev.tmmc.reservity.auth.dto.AuthResponse;
import dev.tmmc.reservity.auth.dto.LoginRequest;
import dev.tmmc.reservity.auth.dto.RefreshRequest;
import dev.tmmc.reservity.auth.dto.RegisterRequest;
import dev.tmmc.reservity.auth.entity.RefreshToken;
import dev.tmmc.reservity.auth.repository.RefreshTokenRepository;
import dev.tmmc.reservity.common.exception.ForbiddenOperationException;
import dev.tmmc.reservity.common.security.JwtService;
import dev.tmmc.reservity.common.security.TokenHasher;
import dev.tmmc.reservity.user.dto.UserResponse;
import dev.tmmc.reservity.user.entity.AccountType;
import dev.tmmc.reservity.user.entity.User;
import dev.tmmc.reservity.user.entity.UserSettings;
import dev.tmmc.reservity.user.mapper.UserMapper;
import dev.tmmc.reservity.user.repository.UserRepository;
import dev.tmmc.reservity.user.repository.UserSettingsRepository;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final UserSettingsRepository userSettingsRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final TokenHasher tokenHasher;
    private final UserMapper userMapper;
    private final RefreshTokenRevoker tokenRevoker;

    @Transactional
    public AuthResponse register(RegisterRequest req, HttpServletRequest http) {
        if (userRepository.existsByEmail(req.email())) {
            throw new DataIntegrityViolationException("Email already registered");
        }

        String handle = (req.handle() == null || req.handle().isBlank())
                ? deriveHandleFromEmail(req.email())
                : req.handle();

        if (userRepository.existsByHandle(handle)) {
            handle = uniquifyHandle(handle);
        }

        User user = User.builder()
                .email(req.email().toLowerCase())
                .passwordHash(passwordEncoder.encode(req.password()))
                .handle(handle)
                .displayName(req.displayName())
                .initials(deriveInitials(req.displayName()))
                .accountType(AccountType.REQUESTER)
                .build();
        user = userRepository.save(user);

        UserSettings settings = UserSettings.defaultsFor(user);
        userSettingsRepository.save(settings);

        return issueTokens(user, http);
    }

    @Transactional
    public AuthResponse login(LoginRequest req, HttpServletRequest http) {
        User user = userRepository.findByEmail(req.email())
                .filter(u -> !u.isDeleted())
                .orElseThrow(() -> new BadCredentialsException("Invalid credentials"));

        if (user.isLocked()) {
            throw new ForbiddenOperationException("Account is temporarily locked. Try again later.");
        }

        if (!passwordEncoder.matches(req.password(), user.getPasswordHash())) {
            user.setFailedLoginAttempts(user.getFailedLoginAttempts() + 1);
            if (user.getFailedLoginAttempts() >= 10) {
                user.setLockedUntil(Instant.now().plusSeconds(15 * 60));
                user.setFailedLoginAttempts(0);
            }
            userRepository.save(user);
            throw new BadCredentialsException("Invalid credentials");
        }

        user.setFailedLoginAttempts(0);
        user.setLastLoginAt(Instant.now());
        userRepository.save(user);

        return issueTokens(user, http);
    }

    @Transactional
    public AuthResponse refresh(RefreshRequest req, HttpServletRequest http) {
        String hash = tokenHasher.hash(req.refreshToken());
        Optional<RefreshToken> stored = refreshTokenRepository.findByTokenHash(hash);

        if (stored.isEmpty()) {
            throw new BadCredentialsException("Invalid refresh token");
        }

        RefreshToken rt = stored.get();

        if (!rt.isActive()) {
            // Reuse-detection: presented token was already revoked or expired.
            // Best-practice: revoke ALL active refresh tokens for that user.
            // Run in REQUIRES_NEW so the revocation commits before we throw and roll back.
            tokenRevoker.revokeAllForUser(rt.getUser());
            log.warn("Refresh token reuse detected for user {} — all sessions revoked", rt.getUser().getId());
            throw new BadCredentialsException("Refresh token is no longer valid");
        }

        // Rotate: revoke this one, issue a new one.
        rt.setRevokedAt(Instant.now());
        refreshTokenRepository.save(rt);

        User user = rt.getUser();
        if (user.isDeleted()) {
            throw new BadCredentialsException("Account is no longer active");
        }

        return issueTokens(user, http);
    }

    @Transactional
    public void logout(String refreshToken) {
        if (refreshToken == null || refreshToken.isBlank()) return;
        String hash = tokenHasher.hash(refreshToken);
        refreshTokenRepository.findByTokenHash(hash).ifPresent(rt -> {
            if (rt.getRevokedAt() == null) {
                rt.setRevokedAt(Instant.now());
                refreshTokenRepository.save(rt);
            }
        });
    }

    private AuthResponse issueTokens(User user, HttpServletRequest http) {
        String accessToken = jwtService.issueAccessToken(user);
        String rawRefresh = tokenHasher.generateRandomToken();

        RefreshToken rt = RefreshToken.builder()
                .user(user)
                .tokenHash(tokenHasher.hash(rawRefresh))
                .deviceLabel(extractDeviceLabel(http))
                .ipAddress(extractIp(http))
                .userAgent(http != null ? http.getHeader("User-Agent") : null)
                .expiresAt(Instant.now().plus(jwtService.refreshTokenTtl()))
                .lastUsedAt(Instant.now())
                .build();
        refreshTokenRepository.save(rt);

        UserResponse userResp = userMapper.toResponse(user);
        return new AuthResponse(accessToken, rawRefresh, jwtService.accessTokenTtl().toSeconds(), userResp);
    }

    private static String extractIp(HttpServletRequest http) {
        if (http == null) return null;
        String xff = http.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isBlank()) {
            int comma = xff.indexOf(',');
            return (comma > 0 ? xff.substring(0, comma) : xff).trim();
        }
        return http.getRemoteAddr();
    }

    private static String extractDeviceLabel(HttpServletRequest http) {
        if (http == null) return null;
        String ua = http.getHeader("User-Agent");
        if (ua == null) return null;
        return ua.length() > 120 ? ua.substring(0, 120) : ua;
    }

    private String uniquifyHandle(String base) {
        for (int i = 1; i < 1000; i++) {
            String candidate = base + i;
            if (candidate.length() > 30) candidate = candidate.substring(0, 30);
            if (!userRepository.existsByHandle(candidate)) return candidate;
        }
        throw new DataIntegrityViolationException("Could not generate unique handle");
    }

    static String deriveHandleFromEmail(String email) {
        String local = email.split("@")[0].toLowerCase();
        String slug = local.replaceAll("[^a-z0-9_]", "_").replaceAll("_+", "_");
        if (slug.length() < 3) slug = slug + "user";
        if (slug.length() > 30) slug = slug.substring(0, 30);
        return slug;
    }

    static String deriveInitials(String displayName) {
        if (displayName == null || displayName.isBlank()) return "U";
        String[] parts = displayName.trim().split("\\s+");
        StringBuilder sb = new StringBuilder();
        sb.append(Character.toUpperCase(parts[0].charAt(0)));
        if (parts.length > 1) sb.append(Character.toUpperCase(parts[parts.length - 1].charAt(0)));
        return sb.toString();
    }
}
