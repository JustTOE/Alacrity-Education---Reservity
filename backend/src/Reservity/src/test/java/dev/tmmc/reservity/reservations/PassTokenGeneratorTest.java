package dev.tmmc.reservity.reservations;

import dev.tmmc.reservity.reservations.service.PassTokenGenerator;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class PassTokenGeneratorTest {

    private final PassTokenGenerator gen = new PassTokenGenerator();

    @Test
    void token_is_64_lowercase_hex_chars() {
        for (int i = 0; i < 50; i++) {
            String token = gen.generate();
            assertEquals(64, token.length(), token);
            assertTrue(token.matches("^[0-9a-f]{64}$"), token);
        }
    }

    @Test
    void tokens_distinct_across_thousands() {
        Set<String> tokens = new HashSet<>();
        for (int i = 0; i < 10_000; i++) {
            tokens.add(gen.generate());
        }
        assertEquals(10_000, tokens.size(), "256-bit space — should never collide");
    }
}
