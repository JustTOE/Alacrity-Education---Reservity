
import { useState as useStateCr, useMemo as useMemoCr, useRef as useRefCr, useEffect as useEffectCr } from "react";
import FloorPlanSketch from "@/components/viz/FloorPlanSketch";
import CampusMap from "@/components/viz/CampusMap";
import IsometricRoom from "@/components/viz/IsometricRoom";
import Sparkline from "@/components/viz/Sparkline";
import { formatHour } from "@/data/booking";


// ============================================================================
// Create listing page — "Offer a space for rent"
// Long-form editor (left) + sticky live preview (right)
// ============================================================================

const SPACE_TYPES = [
  { id: 'lab',    label: 'Lab',          sub: 'Certified, equipment-heavy' },
  { id: 'pod',    label: 'Pod',          sub: '1–2 people, soundproof'    },
  { id: 'open',   label: 'Open room',    sub: 'Drop-in, communal'         },
  { id: 'studio', label: 'Studio',       sub: 'Hands-on, messy welcome'   },
];

const BUILDINGS = ['Hawthorn Sciences', 'Linden Hall', 'Pavilion North', 'Magnolia Arts'];

const AMENITY_GROUPS = [
  { title: 'Tech',        items: ['Wi-Fi 6', 'Power at every seat', 'Dual monitors', 'Projector + screen', 'Bluetooth speaker', 'USB-C dock', 'Wired ethernet'] },
  { title: 'Furniture',   items: ['Standing desks', 'Aeron chairs', 'Movable tables', 'Whiteboard wall', 'Drying racks', 'Pin walls', 'Easels'] },
  { title: 'Comfort',     items: ['Natural light', 'Dimmable lighting', 'Soundproofed', 'Climate controlled', 'Acoustic panels', 'Plants', 'Window seat'] },
  { title: 'Specialty',   items: ['Fume hoods', 'Letterpress', 'Risograph', 'Recording booth', 'MIDI rig', 'Microscopes', 'Centrifuge', 'Darkroom'] },
  { title: 'Service',     items: ['Eyewash station', 'Wash bay', 'Cafe adjacent', 'Coat hooks', 'Lockers', 'Bike rack nearby'] },
];

const DAYS = ['Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat', 'Sun'];

const DEFAULT_FORM = () => ({
  name: '',
  tagline: '',
  type: 'pod',
  description: '',
  building: 'Linden Hall',
  floor: 2,
  room: '',
  pin: { x: 0.40, y: 0.46 },
  seats: 4,
  area: 18,
  isFree: false,
  price: 8,
  photos: [null, null, null, null], // photo "filled" booleans w/ variant labels
  amenities: new Set(['Wi-Fi 6', 'Power at every seat']),
  customAmenity: '',
  vibes: new Set(['focus']),
  rules: ['No food near equipment'],
  newRule: '',
  startDate: null, // Date
  weeklyHours: { open: 8, close: 21 },
  weekDays: new Set(['Mon','Tue','Wed','Thu','Fri']),
  minBlock: 1,
  cancellation: 'flexible', // flexible | strict
  instantBook: true,
});

export default function CreateListingPage({ data, onNavigate, onToast, onSubmit, isSubmitting }) {
  const { VIBES, SPACES, DATE_WINDOW, now } = data;
  const [f, setF] = useStateCr(DEFAULT_FORM());
  const [activeSection, setActiveSection] = useStateCr('basics');

  const set = (k, v) => setF(prev => ({ ...prev, [k]: typeof v === 'function' ? v(prev[k]) : v }));
  const toggleSet = (k, id) => setF(prev => {
    const n = new Set(prev[k]); n.has(id) ? n.delete(id) : n.add(id); return { ...prev, [k]: n };
  });

  // Track section visibility for the spine
  const refs = {
    basics: useRefCr(null), photos: useRefCr(null), location: useRefCr(null),
    capacity: useRefCr(null), amenities: useRefCr(null), vibes: useRefCr(null),
    rules: useRefCr(null), availability: useRefCr(null),
  };
  useEffectCr(() => {
    const obs = new IntersectionObserver((entries) => {
      entries.forEach(e => {
        if (e.isIntersecting && e.intersectionRatio > 0.3) {
          setActiveSection(e.target.dataset.section);
        }
      });
    }, { rootMargin: '-30% 0px -50% 0px', threshold: [0.3, 0.6] });
    Object.values(refs).forEach(r => r.current && obs.observe(r.current));
    return () => obs.disconnect();
  }, []);

  const scrollTo = (k) => {
    refs[k]?.current?.scrollIntoView({ behavior: 'smooth', block: 'start' });
  };

  // ---- Completeness scoring -----------------------------------------------
  const checks = useMemoCr(() => ([
    { 
      id: 'basics',       
      label: 'The Basics',     
      ok: f.name.trim().length >= 3 && 
          f.tagline.trim().length >= 5 && 
          f.description.trim().length >= 30 && 
          !!f.type 
    },
    { id: 'photos',       label: 'At least 1 photo', ok: f.photos.some(Boolean) },
    { id: 'location',     label: 'Where it lives',   ok: !!f.building && !!f.room },
    { id: 'capacity',     label: 'Seats & price',    ok: f.seats > 0 && (f.isFree || f.price >= 0) },
    { id: 'amenities',    label: '≥3 amenities',     ok: f.amenities.size >= 3 },
    { id: 'availability', label: 'Start date set',   ok: !!f.startDate },
  ]), [f]);

  const completed = checks.filter(c => c.ok).length;
  const pct = Math.round((completed / checks.length) * 100);

  // ---- Publish handler -----------------------------------------------------
  const canPublish = checks.every(c => c.ok) && !isSubmitting;
  const onPublish = () => {
    if (!canPublish) { onToast?.('Finish the checklist first'); return; }
    if (onSubmit) {
      onSubmit(f);
    } else {
      onToast?.(`Listed “${f.name}” — verifying ownership…`);
      onNavigate('listings');
    }
  };


  return (
    <div className="cl-page">
      {/* ---------- Page header ---------- */}
      <header className="cl-hero">
        <div className="cl-hero-inner">
          <div>
            <span className="kicker">List your space · Hawthorn Campus</span>
            <h1 className="cl-h1">
              Offer a space <span className="verb">for rent<svg viewBox="0 0 200 18" preserveAspectRatio="none" aria-hidden="true"><path d="M2 12 C 40 4, 90 4, 130 10 S 195 14, 198 8" stroke="#ff823c" strokeWidth="4" fill="none" strokeLinecap="round"/></svg></span>
            </h1>
            <p className="cl-hero-sub">A pod, a darkroom, a sun-warmed corner of the atrium — somebody on campus is looking for exactly that. Take ten minutes to write it down well.</p>
          </div>

          <div className="cl-progress">
            <div className="cl-progress-ring" role="progressbar" aria-valuenow={pct} aria-valuemin={0} aria-valuemax={100}>
              <svg viewBox="0 0 56 56" width="56" height="56" aria-hidden="true">
                <circle cx="28" cy="28" r="24" fill="none" stroke="var(--paper-3)" strokeWidth="4"/>
                <circle cx="28" cy="28" r="24" fill="none" stroke="var(--orange)" strokeWidth="4"
                  strokeDasharray={`${(pct/100) * 2 * Math.PI * 24} 999`}
                  strokeLinecap="round" transform="rotate(-90 28 28)"/>
              </svg>
              <span className="cl-progress-pct">{pct}<small>%</small></span>
            </div>
            <div>
              <div className="h-mono cl-progress-label">DRAFT · AUTO-SAVED</div>
              <div className="cl-progress-status">{completed} of {checks.length} checks passed</div>
            </div>
          </div>
        </div>
      </header>

      {/* ---------- Body: spine | form | preview ---------- */}
      <div className="cl-body">
        {/* Spine */}
        <nav className="cl-spine" aria-label="Sections">
          {[
            ['basics',       '01', 'Basics'],
            ['photos',       '02', 'Photos'],
            ['location',     '03', 'Location'],
            ['capacity',     '04', 'Capacity & price'],
            ['amenities',    '05', 'Amenities'],
            ['vibes',        '06', 'Vibe tags'],
            ['rules',        '07', 'House rules'],
            ['availability', '08', 'When it\u2019s open'],
          ].map(([id, n, label]) => {
            const check = checks.find(c => c.id === id);
            return (
              <button key={id} className={`cl-spine-item ${activeSection === id ? 'active' : ''}`} onClick={() => scrollTo(id)}>
                <span className="cl-spine-num">{n}</span>
                <span className="cl-spine-label">{label}</span>
                {check && (
                  <span className={`cl-spine-dot ${check.ok ? 'ok' : ''}`} aria-hidden="true">
                    {check.ok ? (
                      <svg width="10" height="10" viewBox="0 0 10 10"><path d="M2 5 L4 7 L8 3" fill="none" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round"/></svg>
                    ) : null}
                  </span>
                )}
              </button>
            );
          })}
        </nav>

        {/* Form */}
        <main className="cl-form">
          {/* 01 BASICS ------------------------------------------------------ */}
          <Section n="01" title="The basics" sub="Give it a name people will remember, and a one-line tagline." secRef={refs.basics} dataSection="basics">
            <Field label="Display name" hint="Be specific. ‘Coding Pod 7’ beats ‘Quiet room.’">
              <input
                className="cl-input cl-input-xl"
                value={f.name}
                onChange={e => set('name', e.target.value)}
                placeholder="e.g. Coding Pod 7"
                maxLength={48}
              />
              <Counter v={f.name.trim().length} max={48}/>
            </Field>

            <Field label="Tagline" hint="One sentence — what makes this space feel like itself.">
              <input
                className="cl-input"
                value={f.tagline}
                onChange={e => set('tagline', e.target.value)}
                placeholder="e.g. Soundproof, dual monitor, plant on the sill."
                maxLength={90}
              />
              <Counter v={f.tagline.trim().length} max={90}/>
            </Field>


            <Field label="What kind of space?">
              <div className="cl-type-grid">
                {SPACE_TYPES.map(t => (
                  <button key={t.id} type="button" className={`cl-type-card ${f.type === t.id ? 'active' : ''}`} onClick={() => set('type', t.id)}>
                    <TypeGlyph kind={t.id}/>
                    <div className="cl-type-text">
                      <div className="cl-type-label">{t.label}</div>
                      <div className="cl-type-sub">{t.sub}</div>
                    </div>
                  </button>
                ))}
              </div>
            </Field>

            <Field label="Describe the space" hint="What's it like to be in here? What's it best for? Be honest about quirks.">
              <textarea
                className="cl-input cl-textarea"
                rows={5}
                value={f.description}
                onChange={e => set('description', e.target.value)}
                placeholder="A 6 m² soundproof pod on the 2nd floor of Linden Hall. Dual 27&quot; monitors, mechanical keyboard, Aeron chair, dimmable warm lighting. Lock the door, put on headphones, vanish for three hours."
                maxLength={520}
              />
              <Counter v={f.description.trim().length} max={520}/>
            </Field>

          </Section>

          {/* 02 PHOTOS ------------------------------------------------------ */}
          <Section n="02" title="Photos" sub="Four good photos beat twelve mediocre ones. The first is your cover." secRef={refs.photos} dataSection="photos">
            <div className="cl-photo-grid">
              {f.photos.map((p, i) => (
                <PhotoSlot
                  key={i}
                  index={i}
                  filled={p}
                  primary={i === 0}
                  onFill={(label) => set('photos', arr => arr.map((x, j) => j === i ? label : x))}
                  onClear={() => set('photos', arr => arr.map((x, j) => j === i ? null : x))}
                  space={{ id: f.type + i, type: f.type, area: f.area, seats: f.seats }}
                />
              ))}
            </div>
            <div className="cl-photo-tips">
              <span className="h-mono cl-photo-tip-label">SHOOT LIKE THIS</span>
              <ul>
                <li>Wide establishing shot at the door</li>
                <li>One showing the natural light at its best hour</li>
                <li>A detail — the desk, the rack, the view out the window</li>
                <li>The corner that surprises people</li>
              </ul>
            </div>
          </Section>

          {/* 03 LOCATION --------------------------------------------------- */}
          <Section n="03" title="Where it lives" sub="Building, floor, exact room — then drop a pin so renters can find the door." secRef={refs.location} dataSection="location">
            <div className="cl-loc-grid">
              <Field label="Building">
                <div className="cl-select-wrap">
                  <select className="cl-input" value={f.building} onChange={e => set('building', e.target.value)}>
                    {BUILDINGS.map(b => <option key={b} value={b}>{b}</option>)}
                  </select>
                  <span className="cl-select-caret">▾</span>
                </div>
              </Field>

              <Field label="Floor">
                <NumberStepper value={f.floor} min={-2} max={10} onChange={v => set('floor', v)}
                  format={(v) => v < 0 ? `B${Math.abs(v)}` : v === 0 ? 'G' : `${v}`}/>
              </Field>

              <Field label="Room number / code">
                <input className="cl-input" value={f.room} onChange={e => set('room', e.target.value)} placeholder="e.g. 2-08"/>
              </Field>
            </div>

            <Field label="Drop the pin" hint="Click anywhere on the campus map. Renters will follow this to find your door.">
              <div className="cl-map-frame">
                <PinPlacerMap
                  pin={f.pin}
                  onPlace={(pt) => set('pin', pt)}
                  building={f.building}
                />
                <div className="cl-map-coord">
                  <span className="h-mono">PIN</span>
                  <span className="h-mono cl-map-coord-val">{f.pin.x.toFixed(2)}, {f.pin.y.toFixed(2)}</span>
                </div>
              </div>
            </Field>

            <Field label="Door access" hint="How do people get in when they arrive?">
              <div className="cl-radio-row">
                {[
                  { id: 'student-id', label: 'Tap student ID' },
                  { id: 'pin', label: 'Numeric door code' },
                  { id: 'host', label: 'Host meets you' },
                ].map(a => (
                  <label key={a.id} className={`cl-radio ${f.access === a.id ? 'active' : ''}`}>
                    <input type="radio" name="access" checked={f.access === a.id} onChange={() => set('access', a.id)}/>
                    <span>{a.label}</span>
                  </label>
                ))}
              </div>
            </Field>
          </Section>

          {/* 04 CAPACITY & PRICE ------------------------------------------- */}
          <Section n="04" title="Capacity & price" sub="How many seats, how big, how much per hour." secRef={refs.capacity} dataSection="capacity">
            <div className="cl-cap-grid">
              <Field label="Seats" hint="People at once.">
                <NumberStepper value={f.seats} min={1} max={120} onChange={v => set('seats', v)}/>
              </Field>
              <Field label="Floor area" hint="Approx square meters.">
                <div className="cl-input-suffix">
                  <input className="cl-input" type="number" value={f.area} onChange={e => set('area', Number(e.target.value) || 0)} min={1}/>
                  <span className="cl-input-suffix-tag">m²</span>
                </div>
              </Field>
            </div>

            <Field label="Price" hint="Set hourly — or list it free and offer it as a gift to the commons.">
              <div className="cl-price-row">
                <button type="button" className={`cl-price-toggle ${!f.isFree ? 'active' : ''}`} onClick={() => set('isFree', false)}>
                  <span className="h-mono cl-price-toggle-label">HOURLY</span>
                  <span className="cl-price-toggle-desc">Per-hour rate</span>
                </button>
                <button type="button" className={`cl-price-toggle ${f.isFree ? 'active' : ''}`} onClick={() => set('isFree', true)}>
                  <span className="h-mono cl-price-toggle-label">FREE</span>
                  <span className="cl-price-toggle-desc">Open to all, no charge</span>
                </button>
              </div>
              {!f.isFree && (
                <div className="cl-price-input-row">
                  <div className="cl-input-prefix">
                    <span className="cl-input-prefix-tag">$</span>
                    <input className="cl-input cl-price-input" type="number" value={f.price} min={0} step={1}
                      onChange={e => set('price', Math.max(0, Number(e.target.value) || 0))}/>
                    <span className="cl-input-suffix-tag" style={{ borderLeft: '1px solid var(--line-soft)', borderRight: 0 }}>/ hour</span>
                  </div>
                  <div className="cl-price-slider">
                    <input type="range" min={0} max={50} step={1} value={f.price}
                      onChange={e => set('price', Number(e.target.value))} style={{ accentColor: 'var(--indigo)' }}/>
                    <div className="row" style={{ justifyContent: 'space-between' }}>
                      <span className="h-mono cl-meta">$0</span>
                      <span className="h-mono cl-meta">$25</span>
                      <span className="h-mono cl-meta">$50</span>
                    </div>
                  </div>
                </div>
              )}
              {!f.isFree && f.price > 0 && (
                <div className="cl-earn-callout">
                  <div>
                    <div className="h-mono cl-earn-label">EARN ESTIMATE</div>
                    <div className="cl-earn-val">~ ${Math.round(f.price * 4.5 * 22)}/mo</div>
                    <div className="cl-earn-sub">if booked ~4.5h/day, 22 weekdays.</div>
                  </div>
                  <FloorPlanSketch space={{ type: f.type, area: f.area, seats: f.seats, id: 'preview' }} width={120} height={68}/>
                </div>
              )}
            </Field>
          </Section>

          {/* 05 AMENITIES -------------------------------------------------- */}
          <Section n="05" title="Amenities" sub="Tags that help renters filter — and set expectations. Pick at least three." secRef={refs.amenities} dataSection="amenities">
            <div className="cl-amen-stack">
              {AMENITY_GROUPS.map(g => (
                <div key={g.title} className="cl-amen-group">
                  <span className="h-mono cl-amen-title">{g.title.toUpperCase()}</span>
                  <div className="cl-amen-chips">
                    {g.items.map(a => (
                      <button key={a} type="button"
                        className={`chip cl-amen-chip ${f.amenities.has(a) ? 'active' : ''}`}
                        onClick={() => toggleSet('amenities', a)}>
                        {f.amenities.has(a) && <span className="cl-amen-check">✓</span>}
                        {a}
                      </button>
                    ))}
                  </div>
                </div>
              ))}
              <div className="cl-amen-custom">
                <span className="h-mono cl-amen-title">CUSTOM</span>
                <div className="cl-amen-custom-row">
                  <input
                    className="cl-input"
                    value={f.customAmenity}
                    onChange={e => set('customAmenity', e.target.value)}
                    placeholder="e.g. Vintage Heidelberg press, working"
                    onKeyDown={(e) => { if (e.key === 'Enter' && f.customAmenity.trim()) {
                      e.preventDefault();
                      setF(p => ({ ...p, amenities: new Set([...p.amenities, p.customAmenity.trim()]), customAmenity: '' }));
                    }}}
                  />
                  <button className="btn btn-ghost" type="button" disabled={!f.customAmenity.trim()}
                    onClick={() => setF(p => ({ ...p, amenities: new Set([...p.amenities, p.customAmenity.trim()]), customAmenity: '' }))}>
                    + Add
                  </button>
                </div>
                <div className="row" style={{ flexWrap: 'wrap', gap: 6, marginTop: 10 }}>
                  {[...f.amenities].filter(a => !AMENITY_GROUPS.some(g => g.items.includes(a))).map(a => (
                    <span key={a} className="pill pill-indigo cl-custom-pill">{a}<button type="button" onClick={() => toggleSet('amenities', a)} aria-label={`Remove ${a}`}>×</button></span>
                  ))}
                </div>
              </div>
            </div>
          </Section>

          {/* 06 VIBES ------------------------------------------------------ */}
          <Section n="06" title="Vibe tags" sub="Two or three words that tell renters what to expect when they walk in." secRef={refs.vibes} dataSection="vibes">
            <div className="cl-vibe-grid">
              {VIBES.map(v => (
                <button key={v.id} type="button"
                  className={`cl-vibe-card ${f.vibes.has(v.id) ? 'active' : ''}`}
                  onClick={() => toggleSet('vibes', v.id)}>
                  <span className="cl-vibe-glyph">{v.icon}</span>
                  <span className="cl-vibe-label">{v.label}</span>
                </button>
              ))}
            </div>
          </Section>

          {/* 07 RULES ------------------------------------------------------ */}
          <Section n="07" title="House rules" sub="Set the ground rules. Direct, short, enforceable." secRef={refs.rules} dataSection="rules">
            <ul className="cl-rules-list">
              {f.rules.map((r, i) => (
                <li key={i} className="cl-rule-row">
                  <span className="cl-rule-bullet">·</span>
                  <input
                    className="cl-input cl-rule-input"
                    value={r}
                    onChange={(e) => set('rules', arr => arr.map((x, j) => j === i ? e.target.value : x))}/>
                  <button type="button" className="cl-rule-x" aria-label="Remove rule"
                    onClick={() => set('rules', arr => arr.filter((_, j) => j !== i))}>×</button>
                </li>
              ))}
            </ul>
            <div className="cl-rule-add-row">
              <input
                className="cl-input"
                value={f.newRule}
                onChange={(e) => set('newRule', e.target.value)}
                placeholder="e.g. Lab cert required"
                onKeyDown={(e) => { if (e.key === 'Enter' && f.newRule.trim()) {
                  e.preventDefault();
                  setF(p => ({ ...p, rules: [...p.rules, p.newRule.trim()], newRule: '' }));
                }}}/>
              <button className="btn btn-ghost" type="button" disabled={!f.newRule.trim()}
                onClick={() => setF(p => ({ ...p, rules: [...p.rules, p.newRule.trim()], newRule: '' }))}>
                + Add rule
              </button>
            </div>
            <div className="cl-rule-suggestions">
              <span className="h-mono cl-meta">QUICK ADD</span>
              {['Closed-toe shoes', 'No food', 'Lab cert required', 'Quiet floor — phone calls outside', 'Clean as you go'].map(r => (
                <button key={r} type="button" className="chip" onClick={() => !f.rules.includes(r) && setF(p => ({ ...p, rules: [...p.rules, r] }))}>{r}</button>
              ))}
            </div>
          </Section>

          {/* 08 AVAILABILITY ---------------------------------------------- */}
          <Section n="08" title="When it's open" sub="The first day renters can book, plus the weekly window." secRef={refs.availability} dataSection="availability">
            <Field label="Listings goes live & opens for bookings on">
              <DatePickerInline
                now={now}
                value={f.startDate}
                onChange={(d) => set('startDate', d)}
              />
            </Field>

            <div className="cl-loc-grid">
              <Field label="Days of the week">
                <div className="cl-days-row">
                  {DAYS.map(d => (
                    <button key={d} type="button"
                      className={`cl-day-pill ${f.weekDays.has(d) ? 'active' : ''}`}
                      onClick={() => toggleSet('weekDays', d)}>{d.slice(0,1)}</button>
                  ))}
                </div>
              </Field>

              <Field label="Daily hours">
                <div className="cl-hours-row">
                  <select className="cl-input cl-hour-select" value={f.weeklyHours.open}
                    onChange={e => set('weeklyHours', h => ({ ...h, open: Number(e.target.value) }))}>
                    {Array.from({ length: 24 }, (_, i) => i).map(h => <option key={h} value={h}>{formatHour(h)}</option>)}
                  </select>
                  <span className="cl-hours-dash">→</span>
                  <select className="cl-input cl-hour-select" value={f.weeklyHours.close}
                    onChange={e => set('weeklyHours', h => ({ ...h, close: Number(e.target.value) }))}>
                    {Array.from({ length: 24 }, (_, i) => i).map(h => <option key={h} value={h}>{formatHour(h)}</option>)}
                  </select>
                </div>
              </Field>
            </div>

            <Field label="Minimum booking length">
              <div className="cl-min-row">
                {[1, 2, 3, 4].map(n => (
                  <button key={n} type="button" className={`chip ${f.minBlock === n ? 'active' : ''}`} onClick={() => set('minBlock', n)}>
                    {n} hr{n > 1 ? 's' : ''}
                  </button>
                ))}
              </div>
            </Field>

            <Field label="Cancellation policy">
              <div className="cl-radio-row">
                <label className={`cl-radio cl-radio-wide ${f.cancellation === 'flexible' ? 'active' : ''}`}>
                  <input type="radio" name="cancel" checked={f.cancellation === 'flexible'} onChange={() => set('cancellation', 'flexible')}/>
                  <div>
                    <div className="cl-radio-title">Flexible</div>
                    <div className="cl-radio-sub">Full refund up to 1 hour before.</div>
                  </div>
                </label>
                <label className={`cl-radio cl-radio-wide ${f.cancellation === 'strict' ? 'active' : ''}`}>
                  <input type="radio" name="cancel" checked={f.cancellation === 'strict'} onChange={() => set('cancellation', 'strict')}/>
                  <div>
                    <div className="cl-radio-title">Strict</div>
                    <div className="cl-radio-sub">No refunds within 24 hours.</div>
                  </div>
                </label>
              </div>
            </Field>

            <Field label="Booking style">
              <label className="cl-switch-row">
                <span>
                  <span className="cl-radio-title">Instant booking</span>
                  <span className="cl-radio-sub">Renters can book without asking. Recommended for non-certified spaces.</span>
                </span>
                <Switch on={f.instantBook} onChange={(v) => set('instantBook', v)}/>
              </label>
            </Field>
          </Section>

          {/* Action footer */}
          <div className="cl-actions">
            <button className="btn btn-ghost" onClick={() => onNavigate('listings')}>Cancel</button>
            <button className="btn btn-ghost" onClick={() => onToast?.('Draft saved')}>Save draft</button>
            <button 
              className={`btn btn-orange ${!canPublish || isSubmitting ? 'is-disabled' : ''}`} 
              onClick={onPublish}
              disabled={isSubmitting}
            >
              {isSubmitting ? 'Publishing...' : canPublish ? 'Publish listing →' : `${checks.length - completed} more to fill in`}
            </button>
          </div>
        </main>

        {/* ---------- Sticky preview pane ---------- */}
        <aside className="cl-preview">
          <div className="cl-preview-card">
            <div className="h-mono cl-preview-pill">LIVE PREVIEW</div>
            <ListingPreview form={f}/>
            <div className="cl-preview-stats">
              <PStat n={f.seats || '—'} l="seats"/>
              <PStat n={f.area ? `${f.area}m²` : '—'} l="area"/>
              <PStat n={f.floor < 0 ? `B${Math.abs(f.floor)}` : f.floor === 0 ? 'G' : f.floor} l="floor"/>
              <PStat n={f.isFree ? 'Free' : f.price ? `$${f.price}` : '—'} l={f.isFree ? '' : '/ hr'}/>
            </div>
            <div className="cl-preview-amen">
              {[...f.amenities].slice(0, 6).map(a => (
                <span key={a} className="pill pill-indigo" style={{ fontSize: 11 }}>{a}</span>
              ))}
              {f.amenities.size > 6 && <span className="h-mono cl-meta">+{f.amenities.size - 6} more</span>}
            </div>
          </div>

          <div className="cl-preview-card cl-preview-checklist">
            <div className="h-mono cl-preview-pill">CHECKLIST · {completed}/{checks.length}</div>
            <ul className="cl-check-list">
              {checks.map(c => (
                <li key={c.id} className={c.ok ? 'is-ok' : ''}>
                  <span className="cl-check-glyph">{c.ok ? '✓' : '○'}</span>
                  <span>{c.label}</span>
                </li>
              ))}
            </ul>
          </div>
        </aside>
      </div>

      <CreateListingStyles/>
    </div>
  );
}

// ============================================================================
// Sub-components
// ============================================================================
function Section({ n, title, sub, children, secRef, dataSection }) {
  return (
    <section ref={secRef} data-section={dataSection} className="cl-section">
      <header className="cl-section-head">
        <span className="cl-section-n">{n}</span>
        <div>
          <h2 className="cl-section-title">{title}</h2>
          <p className="cl-section-sub">{sub}</p>
        </div>
      </header>
      <div className="cl-section-body">{children}</div>
    </section>
  );
}

function Field({ label, hint, children }) {
  return (
    <div className="cl-field">
      <label className="cl-field-label">{label}</label>
      {hint && <div className="cl-field-hint">{hint}</div>}
      <div className="cl-field-control">{children}</div>
    </div>
  );
}

function Counter({ v, max }) {
  const near = v > max * 0.9;
  return <span className={`cl-counter ${near ? 'is-near' : ''}`}>{v} / {max}</span>;
}

function NumberStepper({ value, onChange, min = 0, max = 999, format }) {
  return (
    <div className="cl-stepper">
      <button type="button" className="cl-stepper-btn" onClick={() => onChange(Math.max(min, value - 1))} aria-label="Decrement">−</button>
      <span className="cl-stepper-val">{format ? format(value) : value}</span>
      <button type="button" className="cl-stepper-btn" onClick={() => onChange(Math.min(max, value + 1))} aria-label="Increment">+</button>
    </div>
  );
}

function TypeGlyph({ kind }) {
  const g = {
    lab: <g><rect x="6" y="14" width="20" height="14" rx="1"/><line x1="6" y1="20" x2="26" y2="20"/><circle cx="12" cy="9" r="3"/><line x1="20" y1="8" x2="24" y2="12"/></g>,
    pod: <g><rect x="6" y="10" width="20" height="16" rx="2"/><rect x="10" y="14" width="12" height="8"/><circle cx="16" cy="28" r="1.5"/></g>,
    open: <g><rect x="4" y="8" width="24" height="18" rx="1"/><line x1="4" y1="20" x2="28" y2="20"/><circle cx="9" cy="14" r="1.5"/><circle cx="16" cy="14" r="1.5"/><circle cx="23" cy="14" r="1.5"/></g>,
    studio: <g><rect x="5" y="6" width="6" height="22"/><rect x="14" y="6" width="6" height="22"/><rect x="23" y="6" width="4" height="22"/></g>,
  };
  return (
    <svg viewBox="0 0 32 32" width="32" height="32" aria-hidden="true" className="cl-type-glyph">
      <g fill="none" stroke="currentColor" strokeWidth="1.5" strokeLinecap="round" strokeLinejoin="round">{g[kind]}</g>
    </svg>
  );
}

function PhotoSlot({ index, filled, primary, onFill, onClear, space }) {
  const [hover, setHover] = useStateCr(false);
  const fileRef = useRefCr(null);
  const variants = ['a', 'b', 'c', 'd'];
  return (
    <div
      className={`cl-photo-slot ${filled ? 'filled' : ''} ${primary ? 'primary' : ''}`}
      onDragOver={(e) => { e.preventDefault(); setHover(true); }}
      onDragLeave={() => setHover(false)}
      onDrop={(e) => { e.preventDefault(); setHover(false); onFill(variants[index] || 'a'); }}
      style={hover ? { borderColor: 'var(--indigo)', background: 'var(--indigo-pale)' } : null}>
      {filled ? (
        <>
          <div className="cl-photo-fill">
            <IsometricRoom space={space} width={220} height={150}/>
          </div>
          <div className="cl-photo-overlay">
            {primary && <span className="cl-photo-badge">COVER</span>}
            <button type="button" className="cl-photo-x" onClick={(e) => { e.stopPropagation(); onClear(); }} aria-label="Remove">×</button>
          </div>
        </>
      ) : (
        <button type="button" className="cl-photo-empty" onClick={() => { onFill(variants[index] || 'a'); }}>
          <svg viewBox="0 0 32 32" width="28" height="28" fill="none" stroke="currentColor" strokeWidth="1.5" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
            <rect x="4" y="6" width="24" height="20" rx="2"/>
            <circle cx="11" cy="13" r="2"/>
            <path d="M4 22 L12 16 L18 21 L22 18 L28 24"/>
          </svg>
          <span className="cl-photo-empty-label">{primary ? 'Cover photo' : `Photo ${index + 1}`}</span>
          <span className="cl-photo-empty-hint">Click or drag a file</span>
          <input ref={fileRef} type="file" accept="image/*" hidden onChange={() => onFill(variants[index] || 'a')}/>
        </button>
      )}
    </div>
  );
}

function PinPlacerMap({ pin, onPlace, building }) {
  const ref = useRefCr(null);
  const handleClick = (e) => {
    const rect = ref.current.getBoundingClientRect();
    const x = (e.clientX - rect.left) / rect.width;
    const y = (e.clientY - rect.top) / rect.height;
    onPlace({ x: Math.max(0.02, Math.min(0.98, x)), y: Math.max(0.02, Math.min(0.98, y)) });
  };
  return (
    <div ref={ref} onClick={handleClick} className="cl-map-canvas" role="application" aria-label="Click to place pin">
      <svg viewBox="0 0 600 420" style={{ width: '100%', height: '100%', display: 'block' }}>
        <defs>
          <pattern id="cl-mapgrid" width="24" height="24" patternUnits="userSpaceOnUse">
            <path d="M 24 0 L 0 0 0 24" fill="none" stroke="rgba(75,82,167,0.1)" strokeWidth="1"/>
          </pattern>
        </defs>
        <rect width="600" height="420" fill="#fbf8fe"/>
        <rect width="600" height="420" fill="url(#cl-mapgrid)"/>

        <g fill="none" stroke="var(--indigo)" strokeWidth="1.5" strokeLinecap="round" strokeLinejoin="round" opacity="0.85">
          {/* Hawthorn Sciences */}
          <rect x="340" y="120" width="180" height="120" rx="4" fill={building === 'Hawthorn Sciences' ? 'rgba(75,82,167,0.10)' : 'rgba(75,82,167,0.04)'} />
          <rect x="360" y="140" width="60" height="36"/>
          <rect x="440" y="140" width="60" height="36"/>
          <rect x="360" y="190" width="140" height="42"/>
          {/* Linden Hall */}
          <rect x="180" y="180" width="160" height="110" rx="4" fill={building === 'Linden Hall' ? 'rgba(75,82,167,0.10)' : 'rgba(75,82,167,0.04)'} />
          <line x1="260" y1="180" x2="260" y2="290"/>
          {/* Pavilion North */}
          <rect x="120" y="60" width="160" height="100" rx="4" fill={building === 'Pavilion North' ? 'rgba(75,82,167,0.10)' : 'rgba(75,82,167,0.04)'} />
          <line x1="200" y1="60" x2="200" y2="160"/>
          {/* Magnolia Arts */}
          <rect x="380" y="260" width="180" height="120" rx="4" fill={building === 'Magnolia Arts' ? 'rgba(75,82,167,0.10)' : 'rgba(75,82,167,0.04)'} />
          <line x1="380" y1="320" x2="560" y2="320"/>
        </g>
        <g stroke="rgba(75,82,167,0.32)" strokeWidth="1.2" strokeDasharray="3 4" fill="none">
          <path d="M40 240 C 200 240, 280 240, 380 240"/>
          <path d="M300 30 C 300 160, 300 230, 320 360"/>
          <path d="M60 370 C 280 370, 420 370, 580 370"/>
        </g>
        <g fontFamily="JetBrains Mono" fontSize="9" fill="var(--ink-mute)" letterSpacing="0.16em">
          <text x="430" y="115" textAnchor="middle">HAWTHORN SCIENCES</text>
          <text x="260" y="172" textAnchor="middle">LINDEN HALL</text>
          <text x="200" y="52" textAnchor="middle">PAVILION NORTH</text>
          <text x="470" y="252" textAnchor="middle">MAGNOLIA ARTS</text>
        </g>

        {/* Pin */}
        <g transform={`translate(${pin.x * 600},${pin.y * 420})`} style={{ transition: 'transform .25s cubic-bezier(.2,.9,.3,1.2)' }}>
          <circle r="22" fill="var(--orange)" opacity="0.18"/>
          <circle r="14" fill="var(--orange)" opacity="0.32"/>
          <path d="M0 -22 C -10 -22, -14 -14, -14 -6 C -14 4, 0 22, 0 22 C 0 22, 14 4, 14 -6 C 14 -14, 10 -22, 0 -22 Z"
                fill="var(--orange)" stroke="#fff" strokeWidth="2"/>
          <circle r="4" fill="#fff" cy="-6"/>
        </g>
      </svg>
      <div className="cl-map-hint h-mono">CLICK TO MOVE THE PIN</div>
    </div>
  );
}

function DatePickerInline({ now, value, onChange }) {
  const [viewMonth, setViewMonth] = useStateCr(new Date(now.getFullYear(), now.getMonth(), 1));
  const monthStart = new Date(viewMonth.getFullYear(), viewMonth.getMonth(), 1);
  const monthEnd = new Date(viewMonth.getFullYear(), viewMonth.getMonth() + 1, 0);
  const startOffset = (monthStart.getDay() + 6) % 7; // Monday-first
  const days = [];
  for (let i = 0; i < startOffset; i++) days.push(null);
  for (let d = 1; d <= monthEnd.getDate(); d++) days.push(new Date(viewMonth.getFullYear(), viewMonth.getMonth(), d));
  while (days.length % 7 !== 0) days.push(null);

  const same = (a, b) => a && b && a.getFullYear() === b.getFullYear() && a.getMonth() === b.getMonth() && a.getDate() === b.getDate();
  const isPast = (d) => d && d < new Date(now.getFullYear(), now.getMonth(), now.getDate());
  const monthLabel = monthStart.toLocaleDateString('en', { month: 'long', year: 'numeric' });

  const shiftMonth = (dx) => setViewMonth(new Date(monthStart.getFullYear(), monthStart.getMonth() + dx, 1));

  return (
    <div className="cl-datepicker">
      <div className="cl-dp-head">
        <button type="button" className="cl-dp-nav" onClick={() => shiftMonth(-1)} aria-label="Previous month">‹</button>
        <span className="cl-dp-month h-headline">{monthLabel}</span>
        <button type="button" className="cl-dp-nav" onClick={() => shiftMonth(1)} aria-label="Next month">›</button>
      </div>
      <div className="cl-dp-weekrow">
        {['M','T','W','T','F','S','S'].map((d, i) => <span key={i} className="cl-dp-weekday">{d}</span>)}
      </div>
      <div className="cl-dp-grid">
        {days.map((d, i) => {
          if (!d) return <span key={i} className="cl-dp-cell empty"/>;
          const past = isPast(d);
          const sel = same(d, value);
          const today = same(d, now);
          return (
            <button key={i} type="button"
              className={`cl-dp-cell ${sel ? 'sel' : ''} ${today ? 'today' : ''} ${past ? 'past' : ''}`}
              disabled={past}
              onClick={() => onChange(d)}>
              {d.getDate()}
            </button>
          );
        })}
      </div>
      <div className="cl-dp-readout">
        {value ? (
          <>
            <span className="h-mono cl-meta">SELECTED</span>
            <span className="cl-dp-readout-val">{value.toLocaleDateString('en', { weekday: 'long', month: 'long', day: 'numeric' })}</span>
          </>
        ) : (
          <>
            <span className="h-mono cl-meta">SELECT A START DATE</span>
            <span className="cl-dp-readout-val">No date chosen</span>
          </>
        )}
      </div>
    </div>
  );
}

function Switch({ on, onChange }) {
  return (
    <button type="button" className={`cl-switch ${on ? 'on' : ''}`} onClick={() => onChange(!on)} role="switch" aria-checked={on}>
      <span className="cl-switch-knob"/>
    </button>
  );
}

// ---------- Preview pane sub-pieces ----------
function ListingPreview({ form }) {
  return (
    <article className="cl-preview-listing">
      <div className="cl-preview-photo">
        {form.photos.some(Boolean) ? (
          <div style={{ position: 'absolute', inset: 0, background: 'linear-gradient(135deg,#f6f2f8,#e8e4ee)', display: 'grid', placeItems: 'center' }}>
            <IsometricRoom space={{ type: form.type, area: form.area, seats: form.seats, id: 'preview' }} width={260} height={170}/>
          </div>
        ) : (
          <div className="cl-preview-photo-empty">
            <svg width="28" height="28" viewBox="0 0 32 32" fill="none" stroke="currentColor" strokeWidth="1.5" aria-hidden="true">
              <rect x="4" y="6" width="24" height="20" rx="2"/>
              <circle cx="11" cy="13" r="2"/>
              <path d="M4 22 L12 16 L18 21 L22 18 L28 24"/>
            </svg>
            <span className="h-mono">ADD A COVER PHOTO</span>
          </div>
        )}
        {form.isFree && <span className="cl-preview-free-tag">FREE</span>}
      </div>
      <div className="cl-preview-body">
        <div className="row" style={{ justifyContent: 'space-between', alignItems: 'flex-start', gap: 8 }}>
          <div style={{ minWidth: 0 }}>
            <span className="h-mono cl-meta">{form.type?.toUpperCase()} · {form.building.toUpperCase()}</span>
            <h3 className="cl-preview-title">{form.name || <em className="cl-preview-empty">Untitled space</em>}</h3>
          </div>
          {!form.isFree && (
            <div style={{ textAlign: 'right', flexShrink: 0 }}>
              <span className="cl-preview-price">${form.price || 0}</span>
              <span className="h-mono cl-meta" style={{ display: 'block' }}>/HR</span>
            </div>
          )}
        </div>
        <p className="cl-preview-tagline">{form.tagline || <em className="cl-preview-empty">A short tagline goes here.</em>}</p>
        <div className="row" style={{ gap: 6, flexWrap: 'wrap', marginTop: 8 }}>
          {[...form.vibes].slice(0, 3).map(v => (
            <span key={v} className="pill pill-indigo" style={{ fontSize: 11 }}>{v}</span>
          ))}
        </div>
      </div>
    </article>
  );
}

function PStat({ n, l }) {
  return (
    <div className="cl-pstat">
      <div className="cl-pstat-n">{n}</div>
      <div className="h-mono cl-pstat-l">{l}</div>
    </div>
  );
}

// ============================================================================
// Styles
// ============================================================================
function CreateListingStyles() {
  return (
    <style>{`
      .cl-page { background: var(--paper); min-height: calc(100vh - 65px); padding-bottom: 64px; }

      /* Hero */
      .cl-hero { border-bottom: 1px solid var(--line-soft); background: linear-gradient(180deg, #fff 0%, var(--paper) 100%); }
      .cl-hero-inner { max-width: 1440px; margin: 0 auto; padding: 36px 32px 32px; display: grid; grid-template-columns: 1fr auto; gap: 24px; align-items: end; }
      .cl-h1 { font-family: Lexend; font-weight: 700; font-size: 56px; letter-spacing: -0.03em; line-height: 0.98; margin: 6px 0 0; }
      .cl-hero-sub { color: var(--ink-soft); font-size: 16px; max-width: 56ch; margin: 16px 0 0; line-height: 1.55; }
      .cl-progress { display: flex; gap: 16px; align-items: center; padding: 14px 20px; background: #fff; border: 1px solid var(--line-soft); border-radius: 14px; box-shadow: var(--shadow-sm); }
      .cl-progress-ring { position: relative; width: 56px; height: 56px; }
      .cl-progress-pct { position: absolute; inset: 0; display: grid; place-items: center; font-family: Lexend; font-weight: 700; font-size: 16px; color: var(--ink); }
      .cl-progress-pct small { font-size: 9px; font-weight: 500; color: var(--ink-mute); margin-left: 1px; }
      .cl-progress-label { font-size: 10px; letter-spacing: 0.18em; color: var(--ink-mute); }
      .cl-progress-status { font-size: 13px; color: var(--ink-soft); margin-top: 2px; }

      /* Body grid */
      .cl-body { max-width: 1440px; margin: 0 auto; padding: 32px 32px 0; display: grid; grid-template-columns: 200px minmax(0, 1fr) 360px; gap: 36px; align-items: start; }

      /* Spine nav */
      .cl-spine { position: sticky; top: 89px; display: flex; flex-direction: column; gap: 2px; padding: 8px 0; }
      .cl-spine-item { display: grid; grid-template-columns: 28px 1fr 16px; align-items: center; gap: 10px; padding: 8px 10px; border-radius: 8px; color: var(--ink-soft); cursor: pointer; text-align: left; transition: all .12s; }
      .cl-spine-item:hover { background: var(--paper-2); color: var(--ink); }
      .cl-spine-item.active { background: var(--indigo); color: #fff; }
      .cl-spine-item.active .cl-spine-num { color: rgba(255,255,255,0.7); }
      .cl-spine-num { font-family: 'JetBrains Mono', monospace; font-size: 10px; letter-spacing: 0.12em; color: var(--ink-mute); }
      .cl-spine-label { font-size: 13px; font-weight: 600; }
      .cl-spine-dot { width: 16px; height: 16px; border-radius: 999px; border: 1.5px solid currentColor; opacity: 0.4; display: grid; place-items: center; }
      .cl-spine-dot.ok { background: var(--orange); border-color: var(--orange); color: #fff; opacity: 1; }

      /* Form column */
      .cl-form { display: flex; flex-direction: column; gap: 56px; }
      .cl-section { padding: 0; scroll-margin-top: 96px; }
      .cl-section-head { display: grid; grid-template-columns: 60px 1fr; gap: 18px; align-items: start; padding-bottom: 22px; margin-bottom: 22px; border-bottom: 1px dashed var(--line-soft); }
      .cl-section-n { font-family: Lexend; font-weight: 700; font-size: 36px; line-height: 1; color: var(--indigo); letter-spacing: -0.02em; }
      .cl-section-title { font-family: Lexend; font-weight: 700; font-size: 26px; letter-spacing: -0.02em; margin: 0; line-height: 1.1; }
      .cl-section-sub { color: var(--ink-soft); font-size: 14px; margin: 6px 0 0; max-width: 60ch; line-height: 1.5; }
      .cl-section-body { display: flex; flex-direction: column; gap: 26px; }

      /* Fields */
      .cl-field { display: flex; flex-direction: column; gap: 6px; }
      .cl-field-label { font-size: 14px; font-weight: 600; color: var(--ink); }
      .cl-field-hint { font-size: 13px; color: var(--ink-mute); line-height: 1.5; }
      .cl-field-control { display: flex; flex-direction: column; gap: 8px; position: relative; }

      .cl-input { display: block; width: 100%; padding: 12px 14px; border: 1px solid var(--line-soft); border-radius: 10px; background: #fff; font-size: 15px; font-family: 'Work Sans', sans-serif; outline: none; color: var(--ink); transition: border-color .12s, box-shadow .12s; }
      .cl-input::placeholder { color: var(--ink-mute); }
      .cl-input:focus { border-color: var(--indigo); box-shadow: 0 0 0 3px rgba(75,82,167,0.15); }
      .cl-input-xl { font-family: Lexend; font-weight: 600; font-size: 22px; letter-spacing: -0.01em; padding: 14px 16px; }
      .cl-textarea { resize: vertical; min-height: 120px; line-height: 1.55; }
      .cl-counter { position: absolute; right: 12px; bottom: -22px; font-family: 'JetBrains Mono', monospace; font-size: 10px; color: var(--ink-mute); letter-spacing: 0.08em; }
      .cl-counter.is-near { color: var(--orange-deep); }

      .cl-select-wrap { position: relative; }
      .cl-select-wrap select { appearance: none; padding-right: 36px; cursor: pointer; }
      .cl-select-caret { position: absolute; right: 14px; top: 50%; transform: translateY(-50%); color: var(--ink-mute); pointer-events: none; font-size: 12px; }

      /* Type cards */
      .cl-type-grid { display: grid; grid-template-columns: repeat(4, 1fr); gap: 10px; }
      .cl-type-card { display: flex; flex-direction: column; align-items: flex-start; gap: 10px; padding: 16px; border: 1px solid var(--line-soft); border-radius: 12px; background: #fff; cursor: pointer; text-align: left; color: var(--ink-soft); transition: all .12s; }
      .cl-type-card:hover { border-color: var(--lavender); color: var(--ink); }
      .cl-type-card.active { border-color: var(--indigo); background: rgba(75,82,167,0.06); color: var(--indigo); box-shadow: 0 0 0 2px rgba(75,82,167,0.12); }
      .cl-type-glyph { display: block; }
      .cl-type-text { min-width: 0; }
      .cl-type-label { font-family: Lexend; font-weight: 600; font-size: 16px; letter-spacing: -0.01em; color: var(--ink); }
      .cl-type-card.active .cl-type-label { color: var(--indigo); }
      .cl-type-sub { font-size: 12px; color: var(--ink-mute); margin-top: 2px; }

      /* Photo grid */
      .cl-photo-grid { display: grid; grid-template-columns: 2fr 1fr 1fr; grid-template-rows: 1fr 1fr; gap: 10px; aspect-ratio: 8/5; }
      .cl-photo-slot { position: relative; border-radius: 12px; overflow: hidden; background: var(--paper-2); border: 1.5px dashed var(--line); transition: border-color .15s, background .15s; min-height: 0; }
      .cl-photo-slot.primary { grid-row: span 2; }
      .cl-photo-slot.filled { border: 1px solid var(--line-soft); background: #fff; }
      .cl-photo-fill { position: absolute; inset: 0; display: grid; place-items: center; background: linear-gradient(135deg,#f6f2f8,#e8e4ee); }
      .cl-photo-overlay { position: absolute; top: 8px; left: 8px; right: 8px; display: flex; justify-content: space-between; align-items: center; pointer-events: none; }
      .cl-photo-overlay > * { pointer-events: auto; }
      .cl-photo-badge { background: var(--ink); color: #fff; font-family: 'JetBrains Mono', monospace; font-size: 9px; letter-spacing: 0.18em; padding: 4px 8px; border-radius: 999px; }
      .cl-photo-x { width: 24px; height: 24px; border-radius: 999px; background: rgba(27,27,31,0.75); color: #fff; font-size: 16px; line-height: 1; display: grid; place-items: center; cursor: pointer; }
      .cl-photo-x:hover { background: var(--ink); }
      .cl-photo-empty { width: 100%; height: 100%; display: flex; flex-direction: column; align-items: center; justify-content: center; gap: 6px; padding: 12px; background: transparent; cursor: pointer; color: var(--ink-mute); text-align: center; }
      .cl-photo-empty:hover { color: var(--indigo); }
      .cl-photo-empty-label { font-size: 13px; font-weight: 600; color: var(--ink); }
      .cl-photo-empty-hint { font-size: 11px; color: var(--ink-mute); }
      .cl-photo-slot.primary .cl-photo-empty-label { font-size: 15px; }
      .cl-photo-tips { display: grid; grid-template-columns: auto 1fr; gap: 18px; padding: 16px 18px; background: var(--paper-2); border-radius: 12px; align-items: start; }
      .cl-photo-tip-label { font-size: 10px; letter-spacing: 0.18em; color: var(--ink-soft); }
      .cl-photo-tips ul { margin: 0; padding: 0; list-style: none; columns: 2; column-gap: 24px; }
      .cl-photo-tips li { font-size: 13px; color: var(--ink-soft); padding: 3px 0; break-inside: avoid; }
      .cl-photo-tips li::before { content: "→ "; color: var(--indigo); font-family: 'JetBrains Mono', monospace; margin-right: 4px; }

      /* Location */
      .cl-loc-grid { display: grid; grid-template-columns: 1.4fr 0.8fr 1fr; gap: 14px; }
      .cl-stepper { display: inline-flex; align-items: stretch; border: 1px solid var(--line-soft); border-radius: 10px; background: #fff; overflow: hidden; }
      .cl-stepper-btn { padding: 0 14px; font-size: 18px; color: var(--ink-soft); background: #fff; cursor: pointer; min-width: 40px; }
      .cl-stepper-btn:hover { background: var(--paper-2); color: var(--indigo); }
      .cl-stepper-val { min-width: 56px; text-align: center; padding: 12px 8px; font-family: Lexend; font-weight: 600; font-size: 16px; border-left: 1px solid var(--line-soft); border-right: 1px solid var(--line-soft); }

      .cl-map-frame { position: relative; border: 1px solid var(--line-soft); border-radius: 12px; overflow: hidden; background: #fff; }
      .cl-map-canvas { position: relative; aspect-ratio: 600/420; cursor: crosshair; user-select: none; }
      .cl-map-canvas:hover { background: var(--paper-2); }
      .cl-map-hint { position: absolute; left: 12px; bottom: 12px; font-size: 10px; letter-spacing: 0.18em; color: var(--ink-mute); background: rgba(255,255,255,0.85); padding: 4px 8px; border-radius: 999px; pointer-events: none; }
      .cl-map-coord { position: absolute; right: 12px; bottom: 12px; display: flex; gap: 8px; align-items: center; background: rgba(255,255,255,0.95); padding: 4px 10px; border-radius: 999px; font-size: 10px; letter-spacing: 0.14em; color: var(--ink-soft); }
      .cl-map-coord-val { color: var(--indigo); font-weight: 600; }

      .cl-radio-row { display: flex; gap: 10px; flex-wrap: wrap; }
      .cl-radio { display: inline-flex; align-items: center; gap: 10px; padding: 12px 16px; border: 1px solid var(--line-soft); border-radius: 10px; cursor: pointer; background: #fff; transition: all .12s; font-size: 14px; }
      .cl-radio input { accent-color: var(--indigo); }
      .cl-radio.active { border-color: var(--indigo); background: rgba(75,82,167,0.05); color: var(--indigo); }
      .cl-radio-wide { flex: 1; min-width: 0; align-items: flex-start; padding: 16px; }
      .cl-radio-title { font-size: 14px; font-weight: 600; color: var(--ink); }
      .cl-radio.active .cl-radio-title { color: var(--indigo); }
      .cl-radio-sub { font-size: 12px; color: var(--ink-mute); margin-top: 2px; line-height: 1.4; }

      /* Capacity & price */
      .cl-cap-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 14px; }
      .cl-input-suffix, .cl-input-prefix { display: inline-flex; align-items: stretch; border: 1px solid var(--line-soft); border-radius: 10px; background: #fff; overflow: hidden; width: 100%; }
      .cl-input-suffix .cl-input, .cl-input-prefix .cl-input { border: 0; border-radius: 0; }
      .cl-input-suffix .cl-input:focus, .cl-input-prefix .cl-input:focus { box-shadow: none; }
      .cl-input-suffix:focus-within, .cl-input-prefix:focus-within { border-color: var(--indigo); box-shadow: 0 0 0 3px rgba(75,82,167,0.15); }
      .cl-input-suffix-tag, .cl-input-prefix-tag { padding: 0 14px; display: grid; place-items: center; background: var(--paper-2); color: var(--ink-soft); font-size: 14px; font-weight: 500; }
      .cl-input-prefix-tag { font-family: Lexend; font-weight: 600; font-size: 18px; color: var(--ink); }

      .cl-price-row { display: grid; grid-template-columns: 1fr 1fr; gap: 10px; }
      .cl-price-toggle { padding: 14px 16px; background: #fff; border: 1px solid var(--line-soft); border-radius: 10px; cursor: pointer; text-align: left; transition: all .12s; }
      .cl-price-toggle:hover { border-color: var(--lavender); }
      .cl-price-toggle.active { border-color: var(--indigo); background: rgba(75,82,167,0.05); }
      .cl-price-toggle-label { display: block; font-size: 10px; letter-spacing: 0.18em; color: var(--ink-soft); }
      .cl-price-toggle.active .cl-price-toggle-label { color: var(--indigo); }
      .cl-price-toggle-desc { display: block; font-size: 13px; color: var(--ink); margin-top: 4px; font-weight: 500; }

      .cl-price-input-row { display: grid; grid-template-columns: 220px 1fr; gap: 16px; align-items: center; margin-top: 12px; }
      .cl-price-input { font-family: Lexend; font-weight: 600; font-size: 20px; }
      .cl-price-slider { display: flex; flex-direction: column; gap: 4px; }
      .cl-price-slider input[type=range] { width: 100%; }
      .cl-meta { font-family: 'JetBrains Mono', monospace; font-size: 10px; letter-spacing: 0.14em; color: var(--ink-mute); }

      .cl-earn-callout { display: flex; gap: 18px; justify-content: space-between; align-items: center; padding: 16px 20px; background: linear-gradient(135deg, rgba(255,130,60,0.10), rgba(255,130,60,0.02)); border: 1px solid rgba(255,130,60,0.25); border-radius: 12px; margin-top: 14px; }
      .cl-earn-label { font-size: 10px; letter-spacing: 0.18em; color: var(--orange-deep); }
      .cl-earn-val { font-family: Lexend; font-weight: 700; font-size: 28px; color: var(--orange-deep); letter-spacing: -0.02em; line-height: 1.1; margin-top: 4px; }
      .cl-earn-sub { font-size: 12px; color: var(--ink-soft); margin-top: 2px; }

      /* Amenities */
      .cl-amen-stack { display: flex; flex-direction: column; gap: 18px; }
      .cl-amen-group { display: grid; grid-template-columns: 90px 1fr; gap: 16px; align-items: start; padding: 6px 0; }
      .cl-amen-title { font-size: 10px; letter-spacing: 0.18em; color: var(--ink-soft); padding-top: 10px; }
      .cl-amen-chips { display: flex; flex-wrap: wrap; gap: 6px; }
      .cl-amen-chip { font-size: 13px; }
      .cl-amen-chip.active { background: var(--indigo); border-color: var(--indigo); color: #fff; }
      .cl-amen-check { font-size: 11px; }
      .cl-amen-custom { display: grid; grid-template-columns: 90px 1fr; gap: 16px; padding-top: 16px; border-top: 1px solid var(--line-soft); }
      .cl-amen-custom-row { display: flex; gap: 10px; }
      .cl-amen-custom-row .cl-input { flex: 1; }
      .cl-custom-pill { display: inline-flex; align-items: center; gap: 6px; padding-right: 6px; }
      .cl-custom-pill button { background: rgba(255,255,255,0.4); border-radius: 999px; width: 18px; height: 18px; display: grid; place-items: center; font-size: 14px; line-height: 1; color: var(--indigo-deep); cursor: pointer; }

      /* Vibes */
      .cl-vibe-grid { display: grid; grid-template-columns: repeat(4, 1fr); gap: 8px; }
      .cl-vibe-card { display: flex; flex-direction: column; align-items: flex-start; gap: 8px; padding: 14px; background: #fff; border: 1px solid var(--line-soft); border-radius: 12px; cursor: pointer; transition: all .12s; }
      .cl-vibe-card:hover { border-color: var(--lavender); }
      .cl-vibe-card.active { background: var(--indigo); border-color: var(--indigo); color: #fff; }
      .cl-vibe-glyph { font-size: 20px; color: var(--indigo); line-height: 1; }
      .cl-vibe-card.active .cl-vibe-glyph { color: #fff; }
      .cl-vibe-label { font-size: 13px; font-weight: 600; }

      /* Rules */
      .cl-rules-list { list-style: none; padding: 0; margin: 0; display: flex; flex-direction: column; gap: 4px; }
      .cl-rule-row { display: grid; grid-template-columns: 16px 1fr 30px; gap: 10px; align-items: center; padding: 4px 0; }
      .cl-rule-bullet { color: var(--orange); font-weight: 700; font-size: 18px; text-align: center; }
      .cl-rule-input { padding: 8px 12px; font-size: 14px; }
      .cl-rule-x { width: 26px; height: 26px; border-radius: 999px; color: var(--ink-mute); font-size: 16px; line-height: 1; cursor: pointer; transition: all .12s; }
      .cl-rule-x:hover { background: var(--paper-3); color: var(--orange-deep); }
      .cl-rule-add-row { display: flex; gap: 10px; margin-top: 12px; padding-top: 12px; border-top: 1px dashed var(--line-soft); }
      .cl-rule-add-row .cl-input { flex: 1; }
      .cl-rule-suggestions { display: flex; flex-wrap: wrap; gap: 6px; margin-top: 14px; align-items: center; }
      .cl-rule-suggestions .cl-meta { margin-right: 4px; }

      /* Availability */
      .cl-datepicker { display: inline-block; padding: 16px; background: #fff; border: 1px solid var(--line-soft); border-radius: 14px; min-width: 320px; }
      .cl-dp-head { display: flex; justify-content: space-between; align-items: center; margin-bottom: 8px; }
      .cl-dp-month { font-size: 16px; }
      .cl-dp-nav { width: 30px; height: 30px; border-radius: 8px; color: var(--ink-soft); cursor: pointer; font-size: 16px; transition: all .12s; }
      .cl-dp-nav:hover { background: var(--paper-2); color: var(--indigo); }
      .cl-dp-weekrow { display: grid; grid-template-columns: repeat(7, 1fr); gap: 2px; margin-bottom: 4px; }
      .cl-dp-weekday { font-family: 'JetBrains Mono', monospace; font-size: 10px; color: var(--ink-mute); text-align: center; padding: 6px 0; letter-spacing: 0.08em; }
      .cl-dp-grid { display: grid; grid-template-columns: repeat(7, 1fr); gap: 2px; }
      .cl-dp-cell { aspect-ratio: 1; min-width: 36px; border-radius: 8px; background: transparent; cursor: pointer; font-size: 13px; font-weight: 500; transition: all .12s; }
      .cl-dp-cell:hover:not(.empty):not(.past) { background: var(--paper-2); color: var(--indigo); }
      .cl-dp-cell.today { box-shadow: inset 0 0 0 1px var(--indigo); color: var(--indigo); font-weight: 700; }
      .cl-dp-cell.sel { background: var(--orange) !important; color: #fff; font-weight: 700; }
      .cl-dp-cell.past { color: var(--ink-mute); opacity: 0.4; cursor: not-allowed; }
      .cl-dp-cell.empty { cursor: default; }
      .cl-dp-readout { display: flex; justify-content: space-between; align-items: center; margin-top: 12px; padding-top: 12px; border-top: 1px solid var(--line-soft); }
      .cl-dp-readout-val { font-family: Lexend; font-weight: 600; font-size: 14px; }

      .cl-days-row { display: flex; gap: 6px; }
      .cl-day-pill { width: 36px; height: 36px; border-radius: 999px; background: #fff; border: 1px solid var(--line-soft); color: var(--ink-soft); cursor: pointer; font-weight: 600; font-size: 13px; transition: all .12s; }
      .cl-day-pill:hover { border-color: var(--indigo); color: var(--indigo); }
      .cl-day-pill.active { background: var(--indigo); border-color: var(--indigo); color: #fff; }

      .cl-hours-row { display: flex; gap: 10px; align-items: center; }
      .cl-hour-select { flex: 1; }
      .cl-hours-dash { color: var(--ink-mute); font-size: 16px; }

      .cl-min-row { display: flex; gap: 6px; flex-wrap: wrap; }

      .cl-switch-row { display: flex; justify-content: space-between; align-items: center; gap: 16px; padding: 14px 18px; background: #fff; border: 1px solid var(--line-soft); border-radius: 12px; }
      .cl-switch { width: 44px; height: 24px; border-radius: 999px; background: var(--paper-3); position: relative; cursor: pointer; transition: background .2s; border: 0; flex-shrink: 0; }
      .cl-switch.on { background: var(--indigo); }
      .cl-switch-knob { position: absolute; top: 2px; left: 2px; width: 20px; height: 20px; border-radius: 999px; background: #fff; box-shadow: 0 1px 3px rgba(0,0,0,0.2); transition: left .2s; }
      .cl-switch.on .cl-switch-knob { left: 22px; }

      /* Actions */
      .cl-actions { display: flex; justify-content: flex-end; gap: 10px; padding: 28px 0 8px; margin-top: 16px; border-top: 1px solid var(--line-soft); }
      .cl-actions .btn { padding: 12px 22px; font-size: 14px; }
      .cl-actions .btn-orange.is-disabled { background: var(--paper-3); color: var(--ink-mute); cursor: not-allowed; box-shadow: none; }
      .cl-actions .btn-orange.is-disabled:hover { background: var(--paper-3); }

      /* Preview pane */
      .cl-preview { position: sticky; top: 89px; display: flex; flex-direction: column; gap: 14px; }
      .cl-preview-card { background: #fff; border: 1px solid var(--line-soft); border-radius: 16px; padding: 18px; box-shadow: var(--shadow-sm); }
      .cl-preview-pill { font-size: 10px; letter-spacing: 0.18em; color: var(--orange-deep); background: var(--orange-pale); display: inline-block; padding: 4px 10px; border-radius: 999px; margin-bottom: 12px; }
      .cl-preview-listing { border: 1px solid var(--line-soft); border-radius: 12px; overflow: hidden; }
      .cl-preview-photo { position: relative; aspect-ratio: 4/3; background: var(--paper-2); }
      .cl-preview-photo-empty { position: absolute; inset: 0; display: flex; flex-direction: column; align-items: center; justify-content: center; gap: 8px; color: var(--ink-mute); font-size: 11px; letter-spacing: 0.16em; }
      .cl-preview-free-tag { position: absolute; top: 10px; right: 10px; background: var(--green); color: #fff; font-family: 'JetBrains Mono', monospace; font-size: 9px; letter-spacing: 0.18em; padding: 4px 8px; border-radius: 999px; }
      .cl-preview-body { padding: 14px 16px 16px; }
      .cl-preview-title { font-family: Lexend; font-weight: 700; font-size: 20px; letter-spacing: -0.02em; margin: 4px 0 0; line-height: 1.15; }
      .cl-preview-price { font-family: Lexend; font-weight: 700; font-size: 22px; }
      .cl-preview-tagline { font-size: 13px; color: var(--ink-soft); margin: 8px 0 0; line-height: 1.5; }
      .cl-preview-empty { color: var(--ink-mute); font-style: italic; font-weight: 500; }
      .cl-preview-stats { display: grid; grid-template-columns: repeat(4, 1fr); gap: 0; margin-top: 14px; padding: 12px 0; border-top: 1px solid var(--line-soft); border-bottom: 1px solid var(--line-soft); }
      .cl-preview-stats > div + div { border-left: 1px solid var(--line-soft); }
      .cl-pstat { padding: 0 6px; text-align: center; }
      .cl-pstat-n { font-family: Lexend; font-weight: 700; font-size: 17px; line-height: 1.1; }
      .cl-pstat-l { font-size: 9px; letter-spacing: 0.16em; color: var(--ink-mute); margin-top: 3px; text-transform: uppercase; }
      .cl-preview-amen { display: flex; flex-wrap: wrap; gap: 4px; margin-top: 12px; }

      .cl-preview-checklist .cl-check-list { list-style: none; padding: 0; margin: 0; display: flex; flex-direction: column; gap: 6px; }
      .cl-check-list li { display: flex; gap: 10px; align-items: center; font-size: 13px; color: var(--ink-soft); }
      .cl-check-list li.is-ok { color: var(--ink); }
      .cl-check-glyph { width: 18px; height: 18px; border-radius: 999px; background: var(--paper-3); color: var(--ink-mute); display: grid; place-items: center; font-size: 11px; font-weight: 700; }
      .cl-check-list li.is-ok .cl-check-glyph { background: var(--orange); color: #fff; }

      /* Responsive */
      @media (max-width: 1280px) {
        .cl-body { grid-template-columns: 180px minmax(0, 1fr) 320px; gap: 28px; }
      }
      @media (max-width: 1080px) {
        .cl-body { grid-template-columns: 1fr; }
        .cl-spine { position: static; flex-direction: row; overflow-x: auto; padding: 4px 0 12px; gap: 6px; }
        .cl-spine-item { grid-template-columns: auto auto auto; padding: 6px 12px; flex-shrink: 0; }
        .cl-preview { position: static; }
        .cl-type-grid, .cl-vibe-grid { grid-template-columns: 1fr 1fr; }
        .cl-loc-grid, .cl-cap-grid { grid-template-columns: 1fr 1fr; }
        .cl-price-input-row { grid-template-columns: 1fr; }
        .cl-photo-grid { aspect-ratio: 4/3; }
      }
      @media (max-width: 720px) {
        .cl-hero-inner { grid-template-columns: 1fr; padding: 24px 16px 20px; }
        .cl-body { padding: 20px 16px 0; }
        .cl-h1 { font-size: 36px; }
        .cl-section-head { grid-template-columns: 1fr; gap: 6px; }
        .cl-section-n { font-size: 24px; }
        .cl-section-title { font-size: 20px; }
        .cl-type-grid, .cl-vibe-grid, .cl-loc-grid, .cl-cap-grid, .cl-price-row { grid-template-columns: 1fr; }
        .cl-photo-grid { grid-template-columns: 1fr 1fr; aspect-ratio: 1/1; }
        .cl-photo-slot.primary { grid-row: span 1; }
        .cl-amen-group, .cl-amen-custom { grid-template-columns: 1fr; }
      }
    `}</style>
  );
}

window.CreateListingPage = CreateListingPage;
