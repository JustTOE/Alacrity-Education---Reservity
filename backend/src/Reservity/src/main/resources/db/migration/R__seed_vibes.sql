-- ============================================================================
-- R__seed_vibes — canonical 8 vibes from frontend/web/src/data/fixtures.ts.
-- Repeatable migration: re-runs whenever this file's checksum changes, so
-- editing labels/icons here propagates automatically on the next deploy.
-- ============================================================================

INSERT INTO vibes (id, label, icon, description, display_order, active) VALUES
    ('focus',         'Focus mode',      '◐', 'Heads-down work, low conversation.',                  1, TRUE),
    ('loud',          'Loud and proud',  '◉', 'Talk freely; no shushing.',                           2, TRUE),
    ('window',        'Window seat',     '▢', 'A view to look up at.',                               3, TRUE),
    ('solo',          'Solo',            '·', 'One person, one chair.',                              4, TRUE),
    ('group',         'Group of 4+',     '◊', 'Built for small groups working together.',            5, TRUE),
    ('natural-light', 'Natural light',   '☼', 'Sunlit during the day.',                              6, TRUE),
    ('after-hours',   'After hours',     '◑', 'Open after the rest of campus closes.',               7, TRUE),
    ('lab-only',      'Lab certified',   '⌬', 'Restricted to users with the relevant lab cert.',     8, TRUE)
ON CONFLICT (id) DO UPDATE SET
    label         = EXCLUDED.label,
    icon          = EXCLUDED.icon,
    description   = EXCLUDED.description,
    display_order = EXCLUDED.display_order,
    active        = EXCLUDED.active;
