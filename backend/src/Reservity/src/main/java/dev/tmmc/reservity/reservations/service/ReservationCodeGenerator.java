package dev.tmmc.reservity.reservations.service;

import org.springframework.stereotype.Component;

import java.security.SecureRandom;

/**
 * Pure helper that produces "RV-{slug-upper-no-hyphens}-{6-digit-secure-random}".
 * <p>Example: slug "lab-4b" → {@code "RV-LAB4B-839271"}. The 6-digit random
 * suffix gives 1 000 000 codes per slug; collisions are caught at the DB
 * UNIQUE constraint and the caller retries.</p>
 */
@Component
public class ReservationCodeGenerator {

    private static final SecureRandom RNG = new SecureRandom();
    private static final int RANDOM_DIGITS = 6;
    private static final int CODE_MAX_LEN = 20;

    public String generate(String spaceSlug) {
        if (spaceSlug == null || spaceSlug.isBlank()) {
            throw new IllegalArgumentException("spaceSlug must not be blank");
        }
        String compact = spaceSlug.replace("-", "").toUpperCase();
        int suffix = 100_000 + RNG.nextInt(900_000);  // always 6 digits
        String code = "RV-" + compact + "-" + suffix;
        if (code.length() > CODE_MAX_LEN) {
            // Truncate the slug-upper component to fit the column. Keep the
            // "RV-" prefix and "-{6 digits}" suffix intact.
            int slugBudget = CODE_MAX_LEN - 3 - 1 - RANDOM_DIGITS;  // "RV-" + "-" + 6 digits
            String truncated = compact.substring(0, slugBudget);
            code = "RV-" + truncated + "-" + suffix;
        }
        return code;
    }
}
