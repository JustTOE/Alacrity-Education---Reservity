-- ============================================================================
-- R__seed_buildings — the 4 canonical buildings on the demo campus.
-- Pin coords are derived from where the fixture spaces concentrate on the
-- frontend's CampusMap (centroid of each building's spaces' pins). Real
-- GPS lat/long left null until the campus geo data lands.
-- ============================================================================

INSERT INTO buildings (name, short_code, campus_name, pin_x, pin_y, description) VALUES
    ('Hawthorn Sciences', 'HSC', 'Main Campus', 0.6000, 0.4100,
     'Sciences faculty: wet labs, biology / chemistry teaching space, rooftop greenhouse.'),
    ('Linden Hall',       'LHL', 'Main Campus', 0.4200, 0.5000,
     'Quiet study tower. Coding pods, recording studios, and after-hours floors.'),
    ('Pavilion North',    'PVN', 'Main Campus', 0.2800, 0.3200,
     'Open-plan glass commons; cafe-adjacent, loud-friendly, big tables.'),
    ('Magnolia Arts',     'MGA', 'Main Campus', 0.7100, 0.6200,
     'Arts faculty: print studio, photo darkroom, crit rooms, drawing labs.')
ON CONFLICT (name) DO UPDATE SET
    short_code  = EXCLUDED.short_code,
    campus_name = EXCLUDED.campus_name,
    pin_x       = EXCLUDED.pin_x,
    pin_y       = EXCLUDED.pin_y,
    description = EXCLUDED.description,
    updated_at  = NOW();
