package dev.tmmc.reservity.events.service;

import dev.tmmc.reservity.events.repository.EventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.text.Normalizer;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.regex.Pattern;

/**
 * "Open-mic poetry, no rules" + 2026-05-02 → "ev-open-mic-poetry-no-rules-2026-05-02".
 * Collisions append "-2", "-3", … Falls back to a 6-hex random suffix at >99 collisions.
 *
 * <p>Slug constraint matches the V9 CHECK: {@code ^[a-z0-9-]{2,80}$}.
 */
@Service
@RequiredArgsConstructor
public class EventSlugGenerator {

    private static final int MAX_LEN = 80;
    private static final int TITLE_CAP = 50;
    private static final Pattern NON_SLUG_CHARS = Pattern.compile("[^a-z0-9]+");
    private static final Pattern DIACRITICS = Pattern.compile("\\p{InCombiningDiacriticalMarks}+");
    private static final SecureRandom RNG = new SecureRandom();

    private final EventRepository repo;

    public String generate(String title, Instant startsAt) {
        String slugTitle = slugifyTitle(title);
        String date = LocalDate.ofInstant(startsAt, ZoneOffset.UTC).toString(); // YYYY-MM-DD
        String base = ("ev-" + slugTitle + "-" + date);
        if (base.length() > MAX_LEN) base = base.substring(0, MAX_LEN);
        base = trimHyphens(base);

        if (!repo.existsBySlug(base)) return base;

        for (int n = 2; n <= 99; n++) {
            String suffix = "-" + n;
            String candidate = base.length() + suffix.length() > MAX_LEN
                    ? base.substring(0, MAX_LEN - suffix.length()) + suffix
                    : base + suffix;
            candidate = trimHyphens(candidate);
            if (!repo.existsBySlug(candidate)) return candidate;
        }

        // Defensive fallback: 6-hex random suffix.
        String hex = randomHex();
        String fallback = base.length() + 7 > MAX_LEN
                ? base.substring(0, MAX_LEN - 7) + "-" + hex
                : base + "-" + hex;
        return trimHyphens(fallback);
    }

    static String slugifyTitle(String title) {
        if (title == null || title.isBlank()) return "untitled";
        String normalized = Normalizer.normalize(title, Normalizer.Form.NFD);
        normalized = DIACRITICS.matcher(normalized).replaceAll("");
        normalized = normalized.toLowerCase();
        normalized = NON_SLUG_CHARS.matcher(normalized).replaceAll("-");
        normalized = trimHyphens(normalized);
        if (normalized.length() > TITLE_CAP) normalized = trimHyphens(normalized.substring(0, TITLE_CAP));
        return normalized.isEmpty() ? "untitled" : normalized;
    }

    private static String trimHyphens(String s) {
        int start = 0;
        int end = s.length();
        while (start < end && s.charAt(start) == '-') start++;
        while (end > start && s.charAt(end - 1) == '-') end--;
        return s.substring(start, end);
    }

    private static String randomHex() {
        byte[] bytes = new byte[3];
        RNG.nextBytes(bytes);
        StringBuilder hex = new StringBuilder(6);
        for (byte b : bytes) hex.append(String.format("%02x", b));
        return hex.toString();
    }
}
