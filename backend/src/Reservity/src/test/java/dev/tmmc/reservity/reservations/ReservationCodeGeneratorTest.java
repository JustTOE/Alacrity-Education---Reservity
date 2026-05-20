package dev.tmmc.reservity.reservations;

import dev.tmmc.reservity.reservations.service.ReservationCodeGenerator;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class ReservationCodeGeneratorTest {

    private final ReservationCodeGenerator gen = new ReservationCodeGenerator();

    @Test
    void format_is_RV_dash_slugUpperNoHyphens_dash_6digits() {
        for (int i = 0; i < 50; i++) {
            String code = gen.generate("lab-4b");
            assertTrue(code.matches("^RV-LAB4B-\\d{6}$"), code);
        }
    }

    @Test
    void slug_is_uppercased_and_hyphens_stripped() {
        String code = gen.generate("pod-3");
        assertTrue(code.startsWith("RV-POD3-"), code);
    }

    @Test
    void generated_codes_are_distinct_across_thousands() {
        Set<String> codes = new HashSet<>();
        for (int i = 0; i < 10_000; i++) {
            codes.add(gen.generate("lab-4b"));
        }
        // 1M code space, 10k draws — collision probability is tiny but not zero.
        // Allow a handful of collisions (the service retries via UNIQUE).
        assertTrue(codes.size() > 9_900, "expected >9900 unique, got " + codes.size());
    }

    @Test
    void blank_slug_throws() {
        assertThrows(IllegalArgumentException.class, () -> gen.generate(""));
        assertThrows(IllegalArgumentException.class, () -> gen.generate(null));
    }

    @Test
    void long_slug_truncates_to_fit_20_char_column() {
        String code = gen.generate("very-very-very-very-very-long-slug-name-that-exceeds");
        assertTrue(code.length() <= 20, "code = " + code + " (len " + code.length() + ")");
        assertTrue(code.startsWith("RV-"));
    }
}
