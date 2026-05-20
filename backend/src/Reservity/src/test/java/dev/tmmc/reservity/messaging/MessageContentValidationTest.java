package dev.tmmc.reservity.messaging;

import dev.tmmc.reservity.messaging.dto.SendMessageRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Pure-JUnit bean validation tests for {@link SendMessageRequest}. The DB
 * CHECK {@code char_length(trim(content)) > 0} is the safety net — these
 * tests cover the {@code @NotBlank}/{@code @Size} layer that catches bad
 * payloads before they reach the persistence layer.
 */
class MessageContentValidationTest {

    private static ValidatorFactory factory;
    private static Validator validator;

    @BeforeAll
    static void setUp() {
        factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @AfterAll
    static void tearDown() {
        if (factory != null) factory.close();
    }

    @Test
    void valid_content_passes() {
        Set<ConstraintViolation<SendMessageRequest>> v =
                validator.validate(new SendMessageRequest("Hello there"));
        assertTrue(v.isEmpty());
    }

    @Test
    void null_content_fails_NotBlank() {
        Set<ConstraintViolation<SendMessageRequest>> v =
                validator.validate(new SendMessageRequest(null));
        assertFalse(v.isEmpty());
    }

    @Test
    void empty_content_fails_NotBlank() {
        Set<ConstraintViolation<SendMessageRequest>> v =
                validator.validate(new SendMessageRequest(""));
        assertFalse(v.isEmpty());
    }

    @Test
    void whitespace_only_content_fails_NotBlank() {
        Set<ConstraintViolation<SendMessageRequest>> v =
                validator.validate(new SendMessageRequest("   \t\n  "));
        assertFalse(v.isEmpty());
    }

    @Test
    void content_at_4000_chars_passes() {
        String s = "a".repeat(4000);
        Set<ConstraintViolation<SendMessageRequest>> v =
                validator.validate(new SendMessageRequest(s));
        assertTrue(v.isEmpty());
    }

    @Test
    void content_over_4000_chars_fails_Size() {
        String s = "a".repeat(4001);
        Set<ConstraintViolation<SendMessageRequest>> v =
                validator.validate(new SendMessageRequest(s));
        assertFalse(v.isEmpty());
    }
}
