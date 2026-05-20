package dev.tmmc.reservity.events;

import dev.tmmc.reservity.events.repository.EventRepository;
import dev.tmmc.reservity.events.service.EventSlugGenerator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class EventSlugGeneratorTest {

    private EventRepository repo;
    private EventSlugGenerator gen;

    @BeforeEach
    void setUp() {
        repo = Mockito.mock(EventRepository.class);
        gen = new EventSlugGenerator(repo);
    }

    @Test
    void produces_kebab_case_with_date_suffix() {
        when(repo.existsBySlug(anyString())).thenReturn(false);
        String slug = gen.generate("Open-mic poetry, no rules", Instant.parse("2026-05-02T18:00:00Z"));
        assertEquals("ev-open-mic-poetry-no-rules-2026-05-02", slug);
    }

    @Test
    void appends_2_on_first_collision() {
        String base = "ev-poetry-2026-05-02";
        when(repo.existsBySlug(base)).thenReturn(true);
        when(repo.existsBySlug(base + "-2")).thenReturn(false);
        String slug = gen.generate("Poetry", Instant.parse("2026-05-02T18:00:00Z"));
        assertEquals(base + "-2", slug);
    }

    @Test
    void ascii_folds_diacritics() {
        when(repo.existsBySlug(anyString())).thenReturn(false);
        String slug = gen.generate("Café meeting", Instant.parse("2026-05-02T18:00:00Z"));
        assertEquals("ev-cafe-meeting-2026-05-02", slug);
    }

    @Test
    void truncates_long_titles_under_80_chars() {
        when(repo.existsBySlug(anyString())).thenReturn(false);
        String long_title = "This is an extremely long event title that should be truncated for slug purposes hopefully";
        String slug = gen.generate(long_title, Instant.parse("2026-05-02T18:00:00Z"));
        assertTrue(slug.length() <= 80, "slug exceeds 80 chars: " + slug);
        assertTrue(slug.startsWith("ev-"));
    }

    @Test
    void blank_title_falls_back_to_untitled() {
        when(repo.existsBySlug(anyString())).thenReturn(false);
        String slug = gen.generate("   ", Instant.parse("2026-05-02T18:00:00Z"));
        assertEquals("ev-untitled-2026-05-02", slug);
    }

    @Test
    void hex_fallback_after_ninety_nine_collisions() {
        // Every -2..-99 collides; fallback is 6-hex random.
        when(repo.existsBySlug(anyString())).thenAnswer(inv -> {
            String s = inv.getArgument(0);
            // Hex suffix is "-XXXXXX" — 7 chars at the end. Treat that as available.
            if (s.length() >= 7 && s.charAt(s.length() - 7) == '-') {
                String tail = s.substring(s.length() - 6);
                if (tail.matches("[0-9a-f]{6}")) return false;
            }
            return true;
        });
        String slug = gen.generate("X", Instant.parse("2026-05-02T18:00:00Z"));
        assertTrue(slug.matches(".*-[0-9a-f]{6}$"), "expected hex suffix in: " + slug);
    }
}
