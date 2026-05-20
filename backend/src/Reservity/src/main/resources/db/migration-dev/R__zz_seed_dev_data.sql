-- ============================================================================
-- R__zz_seed_dev_data (DEV PROFILE ONLY) — seed the 10 fixture spaces from the
-- frontend so `/api/spaces` returns the same demo data the frontend currently
-- renders.
--
-- Repeatable migration deliberately named with a "zz_" prefix so it sorts
-- last among all R__ files (vibes, buildings, etc.) and runs only after
-- those reference rows exist. The seed is idempotent — re-running it
-- updates existing rows in place via ON CONFLICT.
--
-- IDs (spaces) are auto-generated; the seed references them by slug in any
-- join inserts (space_vibes), so we don't have to pin UUIDs across runs.
--
-- This file lives under db/migration-dev/ — a sibling of db/migration/. It
-- is only on the Flyway location list when the `dev` profile is active
-- (configured in application-dev.yml). Test and prod profiles use the base
-- locations only and never see this file.
-- ============================================================================

-- ----------------------------------------------------------------------------
-- Self-sufficient buildings + vibes inserts.
-- R__seed_buildings and R__seed_vibes own these in the canonical sense, but
-- Flyway only re-runs R__ scripts when their checksum changes — so a wipe by
-- integration tests can leave the table empty even when Flyway thinks the
-- seed already ran. Re-asserting them here keeps the dev seed idempotent.
-- ----------------------------------------------------------------------------
INSERT INTO buildings (name, short_code, campus_name, pin_x, pin_y, description) VALUES
    ('Hawthorn Sciences', 'HSC', 'Main Campus', 0.6000, 0.4100,
     'Sciences faculty: wet labs, biology / chemistry teaching space, rooftop greenhouse.'),
    ('Linden Hall',       'LHL', 'Main Campus', 0.4200, 0.5000,
     'Quiet study tower. Coding pods, recording studios, and after-hours floors.'),
    ('Pavilion North',    'PVN', 'Main Campus', 0.2800, 0.3200,
     'Open-plan glass commons; cafe-adjacent, loud-friendly, big tables.'),
    ('Magnolia Arts',     'MGA', 'Main Campus', 0.7100, 0.6200,
     'Arts faculty: print studio, photo darkroom, crit rooms, drawing labs.')
ON CONFLICT (name) DO NOTHING;

INSERT INTO vibes (id, label, icon, description, display_order, active) VALUES
    ('focus',         'Focus mode',      '◐', 'Heads-down work, low conversation.',                  1, TRUE),
    ('loud',          'Loud and proud',  '◉', 'Talk freely; no shushing.',                           2, TRUE),
    ('window',        'Window seat',     '▢', 'A view to look up at.',                               3, TRUE),
    ('solo',          'Solo',            '·', 'One person, one chair.',                              4, TRUE),
    ('group',         'Group of 4+',     '◊', 'Built for small groups working together.',            5, TRUE),
    ('natural-light', 'Natural light',   '☼', 'Sunlit during the day.',                              6, TRUE),
    ('after-hours',   'After hours',     '◑', 'Open after the rest of campus closes.',               7, TRUE),
    ('lab-only',      'Lab certified',   '⌬', 'Restricted to users with the relevant lab cert.',     8, TRUE)
ON CONFLICT (id) DO NOTHING;

-- ----------------------------------------------------------------------------
-- Dev admin user — owner of all seeded spaces.
-- BCrypt hash below corresponds to password 'dev-admin-pass-123' (BCrypt-12).
-- The id is pinned because owner_id is referenced in every spaces row.
-- ----------------------------------------------------------------------------
INSERT INTO users (
    id, email, password_hash, handle, display_name, initials, account_type,
    verified_student, bio, member_since
) VALUES (
    '00000000-0000-0000-0000-000000000001',
    'admin@reservity.local',
    '$2a$12$Y8Jl3JpZ/j8Mq2GJVk6w8.3YKqx4NfPqUzm3Cq8UtZ8nWxPEnAvUu',
    'reservity_admin',
    'Reservity Admin',
    'RA',
    'ADMIN',
    TRUE,
    'Demo administrator. Owns all seeded spaces in dev/staging.',
    DATE '2026-01-01'
) ON CONFLICT (email) DO NOTHING;

INSERT INTO user_settings (user_id) VALUES ('00000000-0000-0000-0000-000000000001')
    ON CONFLICT (user_id) DO NOTHING;

-- ----------------------------------------------------------------------------
-- Spaces — 1:1 ports of frontend/web/src/data/fixtures.ts SPACES[].
-- Use slug as the natural key. UUIDs are auto-generated; we reference them
-- by slug in the space_vibes block below.
-- ----------------------------------------------------------------------------
INSERT INTO spaces (
    slug, owner_id, owner_type, name, type,
    building_id, floor, room, seats, area_sqm, price_per_hour, currency,
    blurb, description, amenities, rules, pin_x, pin_y, surprise, drop_in
) VALUES
('lab-4b',
 '00000000-0000-0000-0000-000000000001', 'USER',
 'Chemistry Lab 4B', 'lab',
 (SELECT id FROM buildings WHERE name = 'Hawthorn Sciences'),
 4, '4B', 12, 48, 18, 'USD',
 'Fume hoods, safety gear, precision instruments.',
 'A 48 m² wet lab on the 4th floor of Hawthorn Sciences, certified for organic and inorganic synthesis. Six fume hoods, two analytical balances, a centrifuge, and a small instrument bay (UV-vis, IR, GC). Required: lab certification + closed-toe shoes.',
 '["6 fume hoods","2 analytical balances","Centrifuge","UV-vis spectrometer","IR + GC","Eyewash station","Whiteboard wall","Wi-Fi"]'::JSONB,
 '["Lab cert required","Closed-toe shoes","Goggles provided","No food"]'::JSONB,
 0.62, 0.41, FALSE, FALSE),

('pod-3',
 '00000000-0000-0000-0000-000000000001', 'USER',
 'Coding Pod 3', 'pod',
 (SELECT id FROM buildings WHERE name = 'Linden Hall'),
 2, '2-08', 1, 6, 4, 'USD',
 'Soundproof, dual monitor, ergonomic chair.',
 'A 6 m² soundproof pod on the 2nd floor of Linden Hall. Dual 27" monitors, mechanical keyboard (or bring your own), Aeron-style chair, dimmable warm lighting. Lock the door, put on headphones, vanish for three hours.',
 '["Dual 27\" monitors","Soundproofed","Aeron chair","Dimmable lighting","Standing desk","USB-C dock","Wi-Fi 6"]'::JSONB,
 '["1 person max","Max 4-hour booking","Quiet floor — phone calls outside"]'::JSONB,
 0.42, 0.55, FALSE, FALSE),

('atrium',
 '00000000-0000-0000-0000-000000000001', 'USER',
 'The Glass Atrium', 'open',
 (SELECT id FROM buildings WHERE name = 'Pavilion North'),
 1, 'A1', 24, 120, 0, 'USD',
 'South-facing glass, plants, big tables.',
 'A 120 m² south-facing atrium with floor-to-ceiling windows, hanging plants, and four large communal tables seating six each. The acoustic floor of Pavilion North — bring a coffee, sit somewhere, talk to a stranger.',
 '["South-facing glass","4 large tables","Plants everywhere","Standing piano","Cafe adjacent","Power at every seat","Free Wi-Fi"]'::JSONB,
 '["Open to all","Loud-talk friendly","No reservations after 6pm"]'::JSONB,
 0.28, 0.32, FALSE, TRUE),

('studio-c',
 '00000000-0000-0000-0000-000000000001', 'USER',
 'Print Studio C', 'studio',
 (SELECT id FROM buildings WHERE name = 'Magnolia Arts'),
 0, 'B-12', 6, 32, 8, 'USD',
 'Letterpress, screens, drying racks. Messy welcome.',
 '32 m² print studio in the basement of Magnolia Arts. Vandercook letterpress, two screen-print stations, drying racks, and a Risograph. Aprons provided. Things will get on your hands.',
 '["Letterpress","Screen print x2","Risograph","Drying racks","Aprons + gloves","Wash station","Type drawer library"]'::JSONB,
 '["Studio induction required","Clean as you go","No food near presses"]'::JSONB,
 0.71, 0.62, FALSE, FALSE),

('darkroom',
 '00000000-0000-0000-0000-000000000001', 'USER',
 'Photo Darkroom 1', 'studio',
 (SELECT id FROM buildings WHERE name = 'Magnolia Arts'),
 -1, 'D-1', 4, 18, 6, 'USD',
 'Wet bench, enlargers, red safelights.',
 'Wet darkroom for B&W silver gelatin printing. Four enlarger stations, archival wash, red safelights only. Bring your negatives and your patience.',
 '["4 enlargers","Archival wash","Red safelights","Drying lines","Loupe + grain focus","Chemistry refresh daily"]'::JSONB,
 '["Photo cert required","Knock before entering","No phones with screens lit"]'::JSONB,
 0.74, 0.66, TRUE, FALSE),

('studio-grand',
 '00000000-0000-0000-0000-000000000001', 'USER',
 'Recording Studio A', 'studio',
 (SELECT id FROM buildings WHERE name = 'Linden Hall'),
 1, '1-30', 5, 28, 22, 'USD',
 'Treated room, condenser mics, MIDI rig.',
 'Acoustically-treated 28 m² recording room with isolation booth. Neumann + SM7B mics, MOTU interface, MIDI rig, full DAW workstation. Engineer optional.',
 '["Treated room","Iso booth","Neumann + SM7B","MOTU interface","MIDI rig","Logic + Ableton","Drum kit available"]'::JSONB,
 '["Audio cert for solo use","No food or liquids","Engineer fee if requested"]'::JSONB,
 0.45, 0.49, FALSE, FALSE),

('pod-7',
 '00000000-0000-0000-0000-000000000001', 'USER',
 'Coding Pod 7', 'pod',
 (SELECT id FROM buildings WHERE name = 'Linden Hall'),
 3, '3-12', 1, 5, 4, 'USD',
 'Window seat, single desk, soft acoustic panels.',
 'Single-occupant pod with a south-facing window seat on the 3rd floor of Linden Hall. Soft acoustic panels, single desk, plant on the sill. Quietest spot we have.',
 '["Window seat","Acoustic panels","Single desk","Plant","Wi-Fi 6","Power at desk"]'::JSONB,
 '["1 person max","Quiet floor","Plant lives, do not move"]'::JSONB,
 0.4, 0.46, FALSE, FALSE),

('crit-room',
 '00000000-0000-0000-0000-000000000001', 'USER',
 'Crit Room East', 'open',
 (SELECT id FROM buildings WHERE name = 'Magnolia Arts'),
 2, '2-04', 16, 56, 0, 'USD',
 'White walls, movable easels, projector.',
 '56 m² white-walled crit room with movable easels, a ceiling-mounted projector, and pinnable felt walls along two sides. Designed for show-and-tell.',
 '["White walls","Movable easels x8","Projector + screen","Pin walls","Track lighting","Bluetooth speaker"]'::JSONB,
 '["Open to all","Easels stay in room","Lights off when leaving"]'::JSONB,
 0.69, 0.58, FALSE, FALSE),

('lab-2a',
 '00000000-0000-0000-0000-000000000001', 'USER',
 'Bio Lab 2A', 'lab',
 (SELECT id FROM buildings WHERE name = 'Hawthorn Sciences'),
 2, '2A', 10, 40, 16, 'USD',
 'Microscopes, centrifuges, biosafety cabinet.',
 '40 m² biology lab with biosafety cabinet, twelve dissecting microscopes, and a centrifuge bank. BSL-1 work only. Currently fully booked through tomorrow — notify to be next.',
 '["Biosafety cabinet","12 microscopes","Centrifuge bank","Autoclave access","Cold storage","Wash bay"]'::JSONB,
 '["Bio cert required","Lab coat + gloves","No solo after-hours"]'::JSONB,
 0.6, 0.44, FALSE, FALSE),

('rooftop',
 '00000000-0000-0000-0000-000000000001', 'USER',
 'Rooftop Greenhouse', 'open',
 (SELECT id FROM buildings WHERE name = 'Hawthorn Sciences'),
 5, 'R', 8, 36, 4, 'USD',
 'Tomatoes, herbs, occasional bee. Bring a hat.',
 '36 m² rooftop greenhouse on the top of Hawthorn Sciences. Tomatoes, herbs, edible flowers, occasional bees. Spectacular at golden hour. Bring a hat in summer.',
 '["Working garden","Hand tools","Watering system","Wi-Fi (weak)","Sunshade","Compost bin"]'::JSONB,
 '["No pets","No picking without permission","Close all vents when leaving"]'::JSONB,
 0.58, 0.38, FALSE, FALSE)

ON CONFLICT (slug) DO UPDATE SET
    name           = EXCLUDED.name,
    type           = EXCLUDED.type,
    building_id    = EXCLUDED.building_id,
    floor          = EXCLUDED.floor,
    room           = EXCLUDED.room,
    seats          = EXCLUDED.seats,
    area_sqm       = EXCLUDED.area_sqm,
    price_per_hour = EXCLUDED.price_per_hour,
    blurb          = EXCLUDED.blurb,
    description    = EXCLUDED.description,
    amenities      = EXCLUDED.amenities,
    rules          = EXCLUDED.rules,
    pin_x          = EXCLUDED.pin_x,
    pin_y          = EXCLUDED.pin_y,
    surprise       = EXCLUDED.surprise,
    drop_in        = EXCLUDED.drop_in,
    updated_at     = NOW();

-- ----------------------------------------------------------------------------
-- Vibe joins. Wipe and re-insert by slug — this way the seed always reflects
-- the current vibe assignment in fixtures.ts even if vibes were edited.
-- ----------------------------------------------------------------------------
DELETE FROM space_vibes
 WHERE space_id IN (
     SELECT id FROM spaces
     WHERE slug IN ('lab-4b','pod-3','atrium','studio-c','darkroom',
                    'studio-grand','pod-7','crit-room','lab-2a','rooftop')
 );

INSERT INTO space_vibes (space_id, vibe_id)
SELECT s.id, v.vibe_id
FROM spaces s
JOIN (VALUES
    -- lab-4b
    ('lab-4b', 'focus'),
    ('lab-4b', 'group'),
    ('lab-4b', 'natural-light'),
    ('lab-4b', 'lab-only'),
    -- pod-3
    ('pod-3', 'focus'),
    ('pod-3', 'solo'),
    ('pod-3', 'after-hours'),
    -- atrium
    ('atrium', 'loud'),
    ('atrium', 'group'),
    ('atrium', 'natural-light'),
    ('atrium', 'window'),
    -- studio-c
    ('studio-c', 'focus'),
    ('studio-c', 'group'),
    ('studio-c', 'after-hours'),
    -- darkroom
    ('darkroom', 'focus'),
    ('darkroom', 'solo'),
    ('darkroom', 'after-hours'),
    -- studio-grand
    ('studio-grand', 'focus'),
    ('studio-grand', 'group'),
    ('studio-grand', 'after-hours'),
    -- pod-7
    ('pod-7', 'focus'),
    ('pod-7', 'solo'),
    ('pod-7', 'window'),
    -- crit-room
    ('crit-room', 'loud'),
    ('crit-room', 'group'),
    ('crit-room', 'natural-light'),
    -- lab-2a
    ('lab-2a', 'focus'),
    ('lab-2a', 'group'),
    ('lab-2a', 'lab-only'),
    -- rooftop
    ('rooftop', 'natural-light'),
    ('rooftop', 'window'),
    ('rooftop', 'group'),
    ('rooftop', 'after-hours')
) AS v(slug, vibe_id) ON s.slug = v.slug;

-- ----------------------------------------------------------------------------
-- M4 demo: weekly operating-hours overrides on a couple of spaces so the
-- calendar endpoint visibly differs from "always open" for /spaces/lab-4b
-- (closed weekends) and /spaces/pod-7 (closed Sun, late-open Sat).
-- ----------------------------------------------------------------------------
UPDATE spaces SET operating_hours = '{
  "mode": "weekly",
  "schedule": {
    "MON": [{"open":"08:00","close":"22:00"}],
    "TUE": [{"open":"08:00","close":"22:00"}],
    "WED": [{"open":"08:00","close":"22:00"}],
    "THU": [{"open":"08:00","close":"22:00"}],
    "FRI": [{"open":"08:00","close":"22:00"}],
    "SAT": [],
    "SUN": []
  },
  "exceptions": []
}'::JSONB
WHERE slug = 'lab-4b';

UPDATE spaces SET operating_hours = '{
  "mode": "weekly",
  "schedule": {
    "MON": [{"open":"07:00","close":"23:00"}],
    "TUE": [{"open":"07:00","close":"23:00"}],
    "WED": [{"open":"07:00","close":"23:00"}],
    "THU": [{"open":"07:00","close":"23:00"}],
    "FRI": [{"open":"07:00","close":"23:00"}],
    "SAT": [{"open":"10:00","close":"18:00"}],
    "SUN": []
  },
  "exceptions": []
}'::JSONB
WHERE slug = 'pod-7';

-- ----------------------------------------------------------------------------
-- M4 demo closures: a handful of one-off greyed-out windows so /availability
-- shows non-trivial CLOSED segments out of the box. Idempotent — re-deleting
-- the demo rows lets the seed re-insert with up-to-date "today + N" anchors
-- whenever the script re-runs (checksum changes whenever this file changes).
--
-- Anchored to NOW() so the closures fall inside the frontend's 14-day window.
-- ----------------------------------------------------------------------------
DELETE FROM space_closures WHERE reason LIKE '[dev-seed] %';

INSERT INTO space_closures (space_id, starts_at, ends_at, reason, created_by)
SELECT s.id,
       (date_trunc('day', NOW()) + INTERVAL '2 days' + TIME '14:00')::TIMESTAMPTZ,
       (date_trunc('day', NOW()) + INTERVAL '2 days' + TIME '17:00')::TIMESTAMPTZ,
       '[dev-seed] Maintenance — chemical hood inspection',
       '00000000-0000-0000-0000-000000000001'
FROM spaces s WHERE s.slug = 'lab-4b';

INSERT INTO space_closures (space_id, starts_at, ends_at, reason, created_by)
SELECT s.id,
       (date_trunc('day', NOW()) + INTERVAL '1 day' + TIME '02:00')::TIMESTAMPTZ,
       (date_trunc('day', NOW()) + INTERVAL '1 day' + TIME '06:00')::TIMESTAMPTZ,
       '[dev-seed] Cleaning crew',
       '00000000-0000-0000-0000-000000000001'
FROM spaces s WHERE s.slug = 'atrium';

INSERT INTO space_closures (space_id, starts_at, ends_at, reason, created_by)
SELECT s.id,
       (date_trunc('day', NOW()) + INTERVAL '4 days' + TIME '00:00')::TIMESTAMPTZ,
       (date_trunc('day', NOW()) + INTERVAL '5 days' + TIME '00:00')::TIMESTAMPTZ,
       '[dev-seed] Faculty event — closed all day',
       '00000000-0000-0000-0000-000000000001'
FROM spaces s WHERE s.slug = 'studio-grand';

INSERT INTO space_closures (space_id, starts_at, ends_at, reason, created_by)
SELECT s.id,
       (date_trunc('day', NOW()) + INTERVAL '3 days' + TIME '12:00')::TIMESTAMPTZ,
       (date_trunc('day', NOW()) + INTERVAL '3 days' + TIME '18:00')::TIMESTAMPTZ,
       '[dev-seed] Reserved by Studio Faculty',
       '00000000-0000-0000-0000-000000000001'
FROM spaces s WHERE s.slug = 'crit-room';

INSERT INTO space_closures (space_id, starts_at, ends_at, reason, created_by)
SELECT s.id,
       (date_trunc('day', NOW()) + INTERVAL '6 days' + TIME '20:00')::TIMESTAMPTZ,
       (date_trunc('day', NOW()) + INTERVAL '7 days' + TIME '08:00')::TIMESTAMPTZ,
       '[dev-seed] Overnight HVAC service',
       '00000000-0000-0000-0000-000000000001'
FROM spaces s WHERE s.slug = 'rooftop';

-- ----------------------------------------------------------------------------
-- M5 — instant_book defaults to TRUE; explicit re-assert so any space row that
-- predates V6 (in case a prior dev run created it before the column existed)
-- still gets the modern default. Idempotent.
-- ----------------------------------------------------------------------------
UPDATE spaces SET instant_book = TRUE WHERE instant_book IS NULL OR instant_book = FALSE;

-- ----------------------------------------------------------------------------
-- M5 demo reservations — pre-approved bookings on a few spaces so the schedule
-- grid actually shows RESERVED hours out of the box. Anchored to NOW() so the
-- rows always fall inside the 14-day visible window.
--
-- Idempotent via the [dev-seed] purpose tag: we DELETE rows from BOTH
-- reservations and reservation_requests where the request carries that tag,
-- then re-INSERT.
--
-- We pin a deterministic dev requester (different from the admin) so cancel
-- tests can target it. Two-step insert: requests first, then reservations
-- linked back via reservation_code lookups.
-- ----------------------------------------------------------------------------
INSERT INTO users (
    id, email, password_hash, handle, display_name, initials, account_type,
    verified_student, member_since
) VALUES (
    '00000000-0000-0000-0000-000000000010',
    'demo@reservity.local',
    '$2a$12$Y8Jl3JpZ/j8Mq2GJVk6w8.3YKqx4NfPqUzm3Cq8UtZ8nWxPEnAvUu',
    'demo_user',
    'Maya Reyes',
    'MR',
    'REQUESTER',
    TRUE,
    DATE '2026-04-01'
) ON CONFLICT (email) DO NOTHING;

INSERT INTO user_settings (user_id) VALUES ('00000000-0000-0000-0000-000000000010')
    ON CONFLICT (user_id) DO NOTHING;

-- Drop existing dev-seeded reservations so we can re-insert with fresh
-- relative-to-NOW() timestamps. Reservation rows go first (FK to requests).
DELETE FROM saved_passes
 WHERE reservation_id IN (
     SELECT r.id FROM reservations r
       JOIN reservation_requests rq ON rq.id = r.request_id
      WHERE rq.purpose LIKE '[dev-seed] %'
 );
DELETE FROM reservations
 WHERE request_id IN (
     SELECT id FROM reservation_requests WHERE purpose LIKE '[dev-seed] %'
 );
DELETE FROM reservation_requests WHERE purpose LIKE '[dev-seed] %';

-- 1) Insert four pre-approved requests. Reservation codes are deterministic so
--    the join in step 2 stays simple. auto_approved=TRUE / decided_by=admin.
INSERT INTO reservation_requests (
    space_id, requester_id,
    reservation_code,
    starts_at, ends_at,
    purpose, attendees_count, tag,
    status, auto_approved, decided_at, decided_by
)
SELECT s.id, '00000000-0000-0000-0000-000000000010',
       'RV-DEMO-001',
       (date_trunc('day', NOW()) + INTERVAL '1 day' + TIME '14:00')::TIMESTAMPTZ,
       (date_trunc('day', NOW()) + INTERVAL '1 day' + TIME '16:00')::TIMESTAMPTZ,
       '[dev-seed] Cell-culture training session',
       4, 'Group session',
       'APPROVED', TRUE, NOW(), '00000000-0000-0000-0000-000000000001'
FROM spaces s WHERE s.slug = 'lab-4b';

INSERT INTO reservation_requests (
    space_id, requester_id,
    reservation_code,
    starts_at, ends_at,
    purpose, attendees_count, tag,
    status, auto_approved, decided_at, decided_by
)
SELECT s.id, '00000000-0000-0000-0000-000000000010',
       'RV-DEMO-002',
       (date_trunc('day', NOW()) + INTERVAL '2 days' + TIME '10:00')::TIMESTAMPTZ,
       (date_trunc('day', NOW()) + INTERVAL '2 days' + TIME '12:00')::TIMESTAMPTZ,
       '[dev-seed] Solo focus block',
       1, 'Solo focus',
       'APPROVED', TRUE, NOW(), '00000000-0000-0000-0000-000000000001'
FROM spaces s WHERE s.slug = 'pod-7';

INSERT INTO reservation_requests (
    space_id, requester_id,
    reservation_code,
    starts_at, ends_at,
    purpose, attendees_count, tag,
    status, auto_approved, decided_at, decided_by
)
SELECT s.id, '00000000-0000-0000-0000-000000000010',
       'RV-DEMO-003',
       (date_trunc('day', NOW()) + INTERVAL '5 days' + TIME '13:00')::TIMESTAMPTZ,
       (date_trunc('day', NOW()) + INTERVAL '5 days' + TIME '15:00')::TIMESTAMPTZ,
       '[dev-seed] Sketch crit + critique panel',
       12, 'Group session',
       'APPROVED', TRUE, NOW(), '00000000-0000-0000-0000-000000000001'
FROM spaces s WHERE s.slug = 'crit-room';

INSERT INTO reservation_requests (
    space_id, requester_id,
    reservation_code,
    starts_at, ends_at,
    purpose, attendees_count, tag,
    status, auto_approved, decided_at, decided_by
)
SELECT s.id, '00000000-0000-0000-0000-000000000001', -- admin's own booking
       'RV-DEMO-004',
       (date_trunc('day', NOW()) + INTERVAL '8 days' + TIME '09:00')::TIMESTAMPTZ,
       (date_trunc('day', NOW()) + INTERVAL '8 days' + TIME '11:00')::TIMESTAMPTZ,
       '[dev-seed] Microscopy lab session',
       8, 'Group session',
       'APPROVED', TRUE, NOW(), '00000000-0000-0000-0000-000000000001'
FROM spaces s WHERE s.slug = 'lab-2a';

-- 2) Mirror reservations for those approved requests, with deterministic
--    pass tokens (64 hex chars). Useful for QR-code mock-ups in dev.
INSERT INTO reservations (request_id, space_id, user_id, starts_at, ends_at, pass_token)
SELECT rq.id, rq.space_id, rq.requester_id, rq.starts_at, rq.ends_at,
       'aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa1'
  FROM reservation_requests rq WHERE rq.reservation_code = 'RV-DEMO-001';

INSERT INTO reservations (request_id, space_id, user_id, starts_at, ends_at, pass_token)
SELECT rq.id, rq.space_id, rq.requester_id, rq.starts_at, rq.ends_at,
       'aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa2'
  FROM reservation_requests rq WHERE rq.reservation_code = 'RV-DEMO-002';

INSERT INTO reservations (request_id, space_id, user_id, starts_at, ends_at, pass_token)
SELECT rq.id, rq.space_id, rq.requester_id, rq.starts_at, rq.ends_at,
       'aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa3'
  FROM reservation_requests rq WHERE rq.reservation_code = 'RV-DEMO-003';

INSERT INTO reservations (request_id, space_id, user_id, starts_at, ends_at, pass_token)
SELECT rq.id, rq.space_id, rq.requester_id, rq.starts_at, rq.ends_at,
       'aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa4'
  FROM reservation_requests rq WHERE rq.reservation_code = 'RV-DEMO-004';

-- 3) Auto-pin saved passes for the requesters.
INSERT INTO saved_passes (user_id, reservation_id)
SELECT r.user_id, r.id FROM reservations r
  JOIN reservation_requests rq ON rq.id = r.request_id
 WHERE rq.purpose LIKE '[dev-seed] %'
ON CONFLICT (user_id, reservation_id) DO NOTHING;

-- ============================================================================
-- M8 dev events. Anchored to NOW() so they always fall in the live/soon/
-- tomorrow/upcoming buckets. Idempotent via blurb LIKE '[dev-seed] %'.
-- ============================================================================
DELETE FROM event_rsvps WHERE event_id IN (
    SELECT id FROM events WHERE blurb LIKE '[dev-seed] %'
);
DELETE FROM events WHERE blurb LIKE '[dev-seed] %';

-- LIVE: started 20 min ago, ends in 90 min.
INSERT INTO events (slug, host_id, host_type, space_id, building_id,
                    starts_at, ends_at, title, blurb, description,
                    category, tag, capacity, attendees_count,
                    visibility, status)
SELECT 'ev-poetry-live',
       '00000000-0000-0000-0000-000000000010', 'USER',
       (SELECT id FROM spaces WHERE slug = 'atrium'),
       (SELECT b.id FROM buildings b
          JOIN spaces s ON s.building_id = b.id
         WHERE s.slug = 'atrium' LIMIT 1),
       NOW() - INTERVAL '20 minutes',
       NOW() + INTERVAL '90 minutes',
       'Open-mic poetry, no rules',
       '[dev-seed] Bring a notebook or just listen. Bring tea.',
       'Casual gathering, all welcome.',
       'PERFORMANCE', 'Open to all', 40, 0,
       'PUBLIC', 'PUBLISHED'
WHERE NOT EXISTS (SELECT 1 FROM events WHERE slug = 'ev-poetry-live');

-- SOON: starts in 30 min.
INSERT INTO events (slug, host_id, host_type, space_id, building_id,
                    starts_at, ends_at, title, blurb, description,
                    category, tag, capacity, attendees_count,
                    visibility, status)
SELECT 'ev-design-crit-soon',
       '00000000-0000-0000-0000-000000000010', 'USER',
       (SELECT id FROM spaces WHERE slug = 'crit-room'),
       (SELECT b.id FROM buildings b
          JOIN spaces s ON s.building_id = b.id
         WHERE s.slug = 'crit-room' LIMIT 1),
       NOW() + INTERVAL '30 minutes',
       NOW() + INTERVAL '2 hours',
       'Studio crit, all programs welcome',
       '[dev-seed] Bring 3 boards. Honest feedback only.',
       'Drop-in critique session for studio work in progress.',
       'WORKSHOP', 'Bring 3 boards', 12, 0,
       'PUBLIC', 'PUBLISHED'
WHERE NOT EXISTS (SELECT 1 FROM events WHERE slug = 'ev-design-crit-soon');

-- TOMORROW: starts at 14:00 tomorrow UTC.
INSERT INTO events (slug, host_id, host_type, space_id, building_id,
                    starts_at, ends_at, title, blurb, description,
                    category, tag, capacity, attendees_count,
                    visibility, status)
SELECT 'ev-cell-culture-tomorrow',
       '00000000-0000-0000-0000-000000000010', 'USER',
       (SELECT id FROM spaces WHERE slug = 'lab-4b'),
       (SELECT b.id FROM buildings b
          JOIN spaces s ON s.building_id = b.id
         WHERE s.slug = 'lab-4b' LIMIT 1),
       (date_trunc('day', NOW()) + INTERVAL '1 day' + TIME '14:00')::TIMESTAMPTZ,
       (date_trunc('day', NOW()) + INTERVAL '1 day' + TIME '16:00')::TIMESTAMPTZ,
       'Cell-culture training session',
       '[dev-seed] Lab cert recommended. Goggles provided.',
       'Hands-on tutorial for graduate students.',
       'WORKSHOP', 'Lab-only', 8, 0,
       'PUBLIC', 'PUBLISHED'
WHERE NOT EXISTS (SELECT 1 FROM events WHERE slug = 'ev-cell-culture-tomorrow');

-- UPCOMING: 5 days out.
INSERT INTO events (slug, host_id, host_type, space_id, building_id,
                    starts_at, ends_at, title, blurb, description,
                    category, tag, capacity, attendees_count,
                    visibility, status)
SELECT 'ev-research-talk-upcoming',
       '00000000-0000-0000-0000-000000000010', 'USER',
       (SELECT id FROM spaces WHERE slug = 'pod-7'),
       (SELECT b.id FROM buildings b
          JOIN spaces s ON s.building_id = b.id
         WHERE s.slug = 'pod-7' LIMIT 1),
       NOW() + INTERVAL '5 days',
       NOW() + INTERVAL '5 days' + INTERVAL '90 minutes',
       'Research talk: spectroscopy in the wild',
       '[dev-seed] 30-minute talk + 60 minutes of Q&A.',
       'Visiting fellow shares fieldwork notes.',
       'TALK', 'Free', 30, 0,
       'PUBLIC', 'PUBLISHED'
WHERE NOT EXISTS (SELECT 1 FROM events WHERE slug = 'ev-research-talk-upcoming');

-- UPCOMING: 10 days out — student social.
INSERT INTO events (slug, host_id, host_type, space_id, building_id,
                    starts_at, ends_at, title, blurb, description,
                    category, tag, capacity, attendees_count,
                    visibility, status)
SELECT 'ev-board-games-upcoming',
       '00000000-0000-0000-0000-000000000010', 'USER',
       (SELECT id FROM spaces WHERE slug = 'studio-grand'),
       (SELECT b.id FROM buildings b
          JOIN spaces s ON s.building_id = b.id
         WHERE s.slug = 'studio-grand' LIMIT 1),
       NOW() + INTERVAL '10 days',
       NOW() + INTERVAL '10 days' + INTERVAL '4 hours',
       'Board games & cocoa',
       '[dev-seed] Bring a game or borrow one from the shelf.',
       'Casual social, all years welcome.',
       'SOCIAL', 'Bring snacks', 50, 0,
       'PUBLIC', 'PUBLISHED'
WHERE NOT EXISTS (SELECT 1 FROM events WHERE slug = 'ev-board-games-upcoming');
