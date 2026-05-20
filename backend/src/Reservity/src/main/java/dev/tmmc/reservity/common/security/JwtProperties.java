package dev.tmmc.reservity.common.security;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
@ConfigurationProperties(prefix = "reservity.security.jwt")
@Getter
@Setter
public class JwtProperties {

    /** HS256 secret. Must be at least 256 bits (32 bytes) of entropy. */
    private String secret;

    /** Access token lifetime. */
    private Duration accessTokenTtl = Duration.ofHours(1);

    /** Refresh token lifetime. */
    private Duration refreshTokenTtl = Duration.ofDays(7);

    /** Issuer claim. */
    private String issuer = "reservity";
}
