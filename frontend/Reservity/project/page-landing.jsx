/* global React, Logomark, Wordmark, VerbUnderline, FloorPlanSketch, getStatus, formatHour */
const { useState: useStateL, useMemo: useMemoL, useEffect: useEffectL, useRef: useRefL } = React;

function LandingPage({ data, onNavigate, onBook }) {
  const { now, SPACES, EVENTS } = data;
  const currentHour = now.getHours();
  const [q, setQ] = useStateL('');
  const [eventTab, setEventTab] = useStateL('live');
  const inputRef = useRefL(null);

  useEffectL(() => { inputRef.current && inputRef.current.focus({ preventScroll: true }); }, []);

  const results = useMemoL(() => {
    if (!q.trim()) return [];
    const term = q.trim().toLowerCase();
    return SPACES.filter(s =>
      s.name.toLowerCase().includes(term) ||
      s.type.includes(term) ||
      s.building.toLowerCase().includes(term) ||
      s.blurb.toLowerCase().includes(term)
    ).slice(0, 5);
  }, [q, SPACES]);

  const featured = SPACES.slice(0, 4);
  const filteredEvents = EVENTS.filter(e =>
    eventTab === 'live' ? e.status === 'live' :
    eventTab === 'soon' ? e.status === 'soon' :
    e.status === 'tomorrow'
  );

  return (
    <div>
      {/* HERO */}
      <section className="grain archgrid" style={{ position: 'relative', borderBottom: '1px solid var(--line-soft)' }}>
        <div className="container" style={{ padding: '88px 32px 96px', position: 'relative' }}>
          <div style={{ display: 'grid', gridTemplateColumns: 'minmax(0,1.3fr) minmax(0,1fr)', gap: 56, alignItems: 'start' }} className="hero-grid">
            <div>
              <p className="kicker" style={{ color: 'rgba(255,255,255,0.85)' }}>Reservity · Spring '26 · 10,400 spaces · 312 events live</p>
              <h1 className="h-display" style={{ fontSize: 'clamp(80px, 12vw, 168px)', color: '#fff', margin: '12px 0 0' }}>
                <span className="verb">Book.<VerbUnderline color="#ffb892" stroke={5} /></span>
              </h1>
              <p style={{ color: 'rgba(255,255,255,0.92)', fontSize: 22, lineHeight: 1.45, maxWidth: 540, margin: '20px 0 28px', fontWeight: 500 }}>
                Find a room. Find a moment. Search to booked in 30 seconds — try it.
              </p>

              <div style={{ background: '#fff', borderRadius: 14, padding: 8, boxShadow: '0 24px 60px rgba(27,27,31,0.25)', maxWidth: 560 }}>
                <div className="field" style={{ border: 0, padding: '8px 12px' }}>
                  <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="var(--ink-mute)" strokeWidth="2" strokeLinecap="round"><circle cx="11" cy="11" r="7"/><path d="m20 20-3.5-3.5"/></svg>
                  <input ref={inputRef} value={q} onChange={e => setQ(e.target.value)} placeholder="Try 'lab', 'window seat', 'poetry night'…" aria-label="Search spaces" />
                  {q && <button className="btn-link" onClick={() => setQ('')} aria-label="Clear">clear</button>}
                </div>
                {q && (
                  <div style={{ borderTop: '1px solid var(--line-soft)', marginTop: 8, paddingTop: 4 }}>
                    {results.length === 0 && <p style={{ padding: '12px 14px', color: 'var(--ink-mute)', margin: 0, fontSize: 14 }}>No matches. Try a building name or vibe.</p>}
                    {results.map(s => {
                      const status = getStatus(s, currentHour);
                      return (
                        <button key={s.id} onClick={() => onNavigate('listings', { spaceId: s.id })} style={{ display: 'flex', width: '100%', padding: '12px 14px', alignItems: 'center', gap: 14, borderRadius: 10, textAlign: 'left' }}>
                          <FloorPlanSketch space={s} width={56} height={36} />
                          <div className="grow">
                            <div style={{ fontWeight: 600, fontSize: 15 }}>{s.name}</div>
                            <div className="h-mono" style={{ fontSize: 11, color: 'var(--ink-mute)' }}>{s.building.toUpperCase()} · {s.area}M²</div>
                          </div>
                          <span className={`pill ${status.cls}`}><span className="dot"/>{status.label}</span>
                        </button>
                      );
                    })}
                  </div>
                )}
              </div>

              {!q && (
                <div className="row" style={{ gap: 8, marginTop: 16, flexWrap: 'wrap' }}>
                  {['Chemistry lab', 'Quiet pod', 'Window seat', 'Group of 6', 'After hours'].map(t => (
                    <button key={t} className="chip" style={{ background: 'rgba(255,255,255,0.92)', borderColor: 'transparent' }} onClick={() => setQ(t.toLowerCase())}>{t}</button>
                  ))}
                </div>
              )}
            </div>

            {/* EVENTS BOARD */}
            <div>
              <div className="row" style={{ justifyContent: 'space-between', alignItems: 'center', marginBottom: 12 }}>
                <span className="label" style={{ color: 'rgba(255,255,255,0.85)' }}>What's on, right now</span>
                <button className="btn-link" style={{ color: '#ffd9bf', fontSize: 12, fontWeight: 600 }} onClick={() => onNavigate('listings')}>All events →</button>
              </div>
              <div className="row" style={{ gap: 4, marginBottom: 12, padding: 4, background: 'rgba(255,255,255,0.12)', borderRadius: 10, display: 'inline-flex' }}>
                {[{id:'live', label:'● Live', count: EVENTS.filter(e=>e.status==='live').length}, {id:'soon', label:'Tonight', count: EVENTS.filter(e=>e.status==='soon').length}, {id:'tomorrow', label:'Tomorrow', count: EVENTS.filter(e=>e.status==='tomorrow').length}].map(t => (
                  <button key={t.id} onClick={() => setEventTab(t.id)} style={{ padding: '6px 12px', borderRadius: 7, fontSize: 12, fontWeight: 600, color: eventTab === t.id ? 'var(--indigo-deep)' : 'rgba(255,255,255,0.85)', background: eventTab === t.id ? '#fff' : 'transparent', transition: 'all .15s' }}>
                    {t.label} <span style={{ opacity: 0.6, marginLeft: 4 }}>{t.count}</span>
                  </button>
                ))}
              </div>
              <div className="col gap-3">
                {filteredEvents.length === 0 && (
                  <div style={{ background: 'rgba(255,255,255,0.96)', borderRadius: 12, padding: 16, color: 'var(--ink-soft)', fontSize: 14 }}>
                    Nothing on this slot — your move.
                    <button className="btn btn-orange" style={{ marginLeft: 12 }} onClick={() => onNavigate('listings')}>Host one →</button>
                  </div>
                )}
                {filteredEvents.slice(0, 3).map((ev, i) => (
                  <button key={ev.id} onClick={() => onNavigate('listings')}
                    style={{ background: 'rgba(255,255,255,0.96)', borderRadius: 12, padding: 14, display: 'flex', gap: 12, textAlign: 'left', border: 0 }}>
                    <div style={{ width: 40, height: 40, flexShrink: 0, borderRadius: 10, background: ev.status === 'live' ? 'linear-gradient(135deg,#ff9a5f,#ff823c)' : 'linear-gradient(135deg,#646bc2,#4b52a7)', color: '#fff', display: 'grid', placeItems: 'center', fontWeight: 700, fontSize: 13, letterSpacing: '0.04em' }}>{ev.hostInitials}</div>
                    <div className="grow" style={{ minWidth: 0 }}>
                      <div className="row" style={{ gap: 6, marginBottom: 2 }}>
                        {ev.status === 'live' && <span className="pill" style={{ background: '#ffe6d6', color: '#9f4200', padding: '1px 8px', fontSize: 10 }}>● LIVE</span>}
                        <span className="h-mono" style={{ fontSize: 10, color: 'var(--ink-mute)', letterSpacing: '0.12em' }}>{formatHour(ev.startH)}—{formatHour(ev.endH)} · {ev.space.toUpperCase()}</span>
                      </div>
                      <div style={{ fontSize: 14, fontWeight: 600, lineHeight: 1.3, overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>{ev.title}</div>
                      <div className="row" style={{ gap: 8, marginTop: 4 }}>
                        <span className="h-mono" style={{ fontSize: 10, color: 'var(--ink-mute)', letterSpacing: '0.08em' }}>{ev.host.toUpperCase()}</span>
                        <span style={{ fontSize: 11, color: 'var(--orange-deep)', fontWeight: 600 }}>{ev.attendees}/{ev.capacity}</span>
                      </div>
                    </div>
                  </button>
                ))}
              </div>
            </div>
          </div>
        </div>
      </section>

      {/* WHO WE ARE */}
      <section className="container" style={{ padding: '120px 32px 80px' }}>
        <div style={{ display: 'grid', gridTemplateColumns: 'minmax(0,1fr) minmax(0,1.2fr)', gap: 64, alignItems: 'start' }} className="who-grid">
          <div>
            <p className="kicker">01 / Who we are</p>
            <h2 className="h-display" style={{ fontSize: 'clamp(48px, 6vw, 88px)', margin: '12px 0 0' }}>
              The campus, <span className="verb">drawn<VerbUnderline /></span> to scale.
            </h2>
          </div>
          <div>
            <p style={{ fontSize: 20, lineHeight: 1.55, color: 'var(--ink-soft)', margin: 0, maxWidth: 580 }}>
              Reservity started as a graph-paper sketch on a chemistry-lab whiteboard. Two students, one frustrated TA, and a question: <em>why is booking a room harder than running the experiment in it?</em>
            </p>
            <p style={{ fontSize: 17, lineHeight: 1.6, color: 'var(--ink-soft)', margin: '20px 0 0', maxWidth: 580 }}>
              We built Reservity to make every room, studio, and rooftop on campus visible and accessible — and to give the people who already <em>use</em> them a way to invite everyone else in. It's part directory, part bulletin board, part tape-it-to-the-wall flyer.
            </p>
            <div className="row" style={{ gap: 32, marginTop: 32, flexWrap: 'wrap' }}>
              <LandingStat n="10,400" label="spaces drawn to scale" />
              <LandingStat n="42 min" label="avg. search → booked" />
              <LandingStat n="312" label="events this week" />
            </div>
          </div>
        </div>
      </section>

      {/* HOW WE HELP */}
      <section style={{ background: 'var(--paper-2)', borderTop: '1px solid var(--line-soft)', borderBottom: '1px solid var(--line-soft)' }}>
        <div className="container" style={{ padding: '96px 32px' }}>
          <div className="row" style={{ justifyContent: 'space-between', alignItems: 'flex-end', marginBottom: 40, flexWrap: 'wrap', gap: 16 }}>
            <div>
              <p className="kicker">02 / How we help</p>
              <h2 className="h-display" style={{ fontSize: 'clamp(40px, 5vw, 64px)', margin: '8px 0 0' }}>What are you here to <span className="verb">do?<VerbUnderline /></span></h2>
            </div>
          </div>
          <div style={{ display: 'grid', gridTemplateColumns: 'repeat(3, 1fr)', gap: 16 }} className="help-grid">
            <HelpCard
              kicker="01"
              title="I need a room. Now."
              body="Live availability, drawn floor plans, walk-in passes. No emails, no DMs, no 'let me check the calendar.'"
              cta="Find a space"
              onClick={() => onNavigate('listings')}
              accent="indigo"
            />
            <HelpCard
              kicker="02"
              title="I'm hosting something."
              body="Book a space and post the event in one flow. Set capacity, RSVP, or just leave the door open. Your event lands on the home page."
              cta="Host an event"
              onClick={() => onNavigate('listings')}
              accent="orange"
            />
            <HelpCard
              kicker="03"
              title="I want to wander in."
              body="Browse what's happening across all four buildings tonight. Show up, listen, leave when you want."
              cta="See tonight"
              onClick={() => onNavigate('landing')}
              accent="indigo"
            />
          </div>
        </div>
      </section>

      {/* BENTO — spaces */}
      <section className="container" style={{ padding: '96px 32px' }}>
        <div className="row" style={{ justifyContent: 'space-between', alignItems: 'flex-end', marginBottom: 32, flexWrap: 'wrap', gap: 16 }}>
          <div>
            <p className="kicker">03 / Catalog</p>
            <h2 className="h-display" style={{ fontSize: 'clamp(48px, 6vw, 80px)', margin: '8px 0 0' }}>
              Spaces for every <span className="verb">question.<VerbUnderline /></span>
            </h2>
          </div>
          <button className="btn btn-ghost" onClick={() => onNavigate('listings')}>Browse all 1,240 →</button>
        </div>

        <div style={{ display: 'grid', gridTemplateColumns: 'repeat(6, 1fr)', gridAutoRows: '180px', gap: 16 }} className="bento-grid">
          <BentoTile space={featured[0]} span="span 4 / span 4" rowSpan="span 2" big onNavigate={onNavigate}/>
          <BentoTile space={featured[1]} span="span 2 / span 2" rowSpan="span 1" onNavigate={onNavigate}/>
          <BentoTile space={featured[2]} span="span 2 / span 2" rowSpan="span 1" onNavigate={onNavigate}/>
          <BentoTile space={featured[3]} span="span 3 / span 3" rowSpan="span 1" onNavigate={onNavigate}/>
          <SuggestTile span="span 3 / span 3" rowSpan="span 1" />
        </div>
      </section>

      <Footer onNavigate={onNavigate} />

      <style>{`
        @media (max-width: 980px){
          .hero-grid, .who-grid, .help-grid { grid-template-columns: 1fr !important; gap: 32px !important; }
          .bento-grid { grid-template-columns: repeat(2, 1fr) !important; grid-auto-rows: 200px !important; }
          .bento-grid > * { grid-column: span 2 / span 2 !important; grid-row: span 1 !important; }
        }
      `}</style>
    </div>
  );
}

function LandingStat({ n, label }) {
  return (
    <div>
      <div className="h-display" style={{ fontSize: 36 }}>{n}</div>
      <div className="h-mono" style={{ fontSize: 11, color: 'var(--ink-mute)', letterSpacing: '0.14em', textTransform: 'uppercase', marginTop: 4 }}>{label}</div>
    </div>
  );
}

function HelpCard({ kicker, title, body, cta, onClick, accent }) {
  return (
    <article className="card archgrid" style={{ padding: 28, position: 'relative', overflow: 'hidden', display: 'flex', flexDirection: 'column', minHeight: 280 }}>
      <span className="h-mono" style={{ fontSize: 11, letterSpacing: '0.22em', color: accent === 'orange' ? 'var(--orange-deep)' : 'var(--indigo)' }}>{kicker}</span>
      <h3 className="h-display" style={{ fontSize: 28, margin: '12px 0 12px', maxWidth: 280 }}>{title}</h3>
      <p style={{ color: 'var(--ink-soft)', fontSize: 15, lineHeight: 1.55, margin: 0, flex: 1 }}>{body}</p>
      <button className={`btn ${accent === 'orange' ? 'btn-orange' : 'btn-primary'}`} style={{ alignSelf: 'flex-start', marginTop: 20 }} onClick={onClick}>{cta} →</button>
    </article>
  );
}

function BentoTile({ space, span, rowSpan, big, onNavigate }) {
  if (!space) return null;
  return (
    <button
      className="card archgrid"
      onClick={() => onNavigate('listings', { spaceId: space.id })}
      style={{
        gridColumn: span, gridRow: rowSpan,
        position: 'relative', overflow: 'hidden', padding: 0, textAlign: 'left',
        background: 'linear-gradient(180deg, #fff 0%, #f6f2f8 100%)',
      }}>
      <div style={{ position: 'absolute', inset: 0, display: 'flex', alignItems: 'center', justifyContent: 'center', opacity: 0.7 }}>
        <FloorPlanSketch space={space} width={big ? 480 : 280} height={big ? 280 : 160} />
      </div>
      <div style={{ position: 'absolute', left: 24, right: 24, bottom: 20, top: 20, display: 'flex', flexDirection: 'column', justifyContent: 'space-between' }}>
        <div className="row" style={{ justifyContent: 'space-between' }}>
          <span className="label">{space.type.toUpperCase()} · {space.building}</span>
          <span className="h-mono" style={{ fontSize: 10, color: 'var(--ink-mute)' }}>{space.area}M²</span>
        </div>
        <div>
          <h3 className="h-display" style={{ fontSize: big ? 48 : 24, margin: 0 }}>{space.name}</h3>
          {big && <p style={{ color: 'var(--ink-soft)', margin: '8px 0 0', maxWidth: 360 }}>{space.blurb}</p>}
        </div>
      </div>
    </button>
  );
}

function SuggestTile({ span, rowSpan }) {
  return (
    <div className="card grain" style={{ gridColumn: span, gridRow: rowSpan, padding: 24, color: '#fff', display: 'flex', flexDirection: 'column', justifyContent: 'space-between' }}>
      <span className="label" style={{ color: 'rgba(255,255,255,0.85)' }}>Empty state</span>
      <div>
        <h3 className="h-display" style={{ fontSize: 28, margin: 0 }}>Don't see your vibe?</h3>
        <button className="btn" style={{ marginTop: 14, background: '#fff', color: 'var(--indigo)' }}>Suggest a space →</button>
      </div>
    </div>
  );
}

function Footer({ onNavigate }) {
  return (
    <footer style={{ background: 'var(--ink)', color: 'rgba(243,240,245,0.92)', borderTop: '1px solid var(--line-soft)' }}>
      <div className="container" style={{ padding: '72px 32px 32px' }}>
        <div style={{ display: 'grid', gridTemplateColumns: '1.6fr 1fr 1fr 1fr 1.2fr', gap: 40 }} className="footer-grid">
          <div>
            <div className="row" style={{ gap: 10 }}>
              <Logomark size={32} color="#fff" accent="#ffb892" />
              <span className="h-display" style={{ fontSize: 24, color: '#fff', letterSpacing: '-0.02em' }}>Reservity</span>
            </div>
            <p style={{ marginTop: 16, fontSize: 14, lineHeight: 1.6, color: 'rgba(243,240,245,0.7)', maxWidth: 280 }}>
              Drawn to scale, booked in 30 seconds. Built on the Hawthorn Campus, opened to all.
            </p>
            <form onSubmit={e => e.preventDefault()} style={{ marginTop: 20, display: 'flex', gap: 6, maxWidth: 320 }}>
              <input
                type="email"
                placeholder="you@university.edu"
                aria-label="Email for weekly newsletter"
                style={{ flex: 1, background: 'rgba(255,255,255,0.08)', border: '1px solid rgba(255,255,255,0.15)', borderRadius: 8, padding: '10px 12px', color: '#fff', fontSize: 14, outline: 'none' }}
              />
              <button type="submit" className="btn btn-orange" style={{ padding: '10px 14px' }}>Subscribe</button>
            </form>
            <p className="h-mono" style={{ fontSize: 10, color: 'rgba(243,240,245,0.45)', letterSpacing: '0.12em', marginTop: 10 }}>WEEKLY · WHAT'S ON, IN ONE EMAIL</p>
          </div>

          <FooterCol title="Spaces" links={[
            { label: 'Browse all', onClick: () => onNavigate('listings') },
            { label: 'Labs' }, { label: 'Studios' }, { label: 'Pods' },
            { label: 'Open rooms' }, { label: 'Suggest a space' },
          ]} />
          <FooterCol title="Events" links={[
            { label: 'Live now' }, { label: 'Tonight' },
            { label: 'This week' }, { label: 'Host an event' },
            { label: 'Event guidelines' },
          ]} />
          <FooterCol title="Account" links={[
            { label: 'My passes', onClick: () => onNavigate('account') },
            { label: 'My events' }, { label: 'Vibe profile' },
            { label: 'Notifications' }, { label: 'Sign out' },
          ]} />
          <FooterCol title="Reservity" links={[
            { label: 'Who we are' }, { label: 'Campus partners' },
            { label: 'Press kit' }, { label: 'Help center' },
            { label: 'Accessibility' }, { label: 'Status · all green' },
          ]} />
        </div>

        <div style={{ height: 1, background: 'rgba(255,255,255,0.1)', margin: '48px 0 24px' }} />

        <div className="row" style={{ justifyContent: 'space-between', alignItems: 'center', flexWrap: 'wrap', gap: 16 }}>
          <span className="h-mono" style={{ fontSize: 11, letterSpacing: '0.16em', color: 'rgba(243,240,245,0.55)' }}>
            © 2026 RESERVITY · HAWTHORN CAMPUS · MADE WITH GRAPHITE & GRIDS
          </span>
          <div className="row" style={{ gap: 14 }}>
            <Social icon="ig" label="Instagram" />
            <Social icon="ig-tt" label="TikTok" />
            <Social icon="x" label="X" />
            <Social icon="ds" label="Discord" />
            <Social icon="gh" label="GitHub" />
          </div>
          <div className="row" style={{ gap: 16 }}>
            <a href="#" style={{ fontSize: 12, color: 'rgba(243,240,245,0.6)' }}>Privacy</a>
            <a href="#" style={{ fontSize: 12, color: 'rgba(243,240,245,0.6)' }}>Terms</a>
            <a href="#" style={{ fontSize: 12, color: 'rgba(243,240,245,0.6)' }}>Cookies</a>
          </div>
        </div>
      </div>
      <style>{`
        @media (max-width: 980px){
          .footer-grid { grid-template-columns: 1fr 1fr !important; gap: 32px !important; }
          .footer-grid > :first-child { grid-column: span 2 !important; }
        }
      `}</style>
    </footer>
  );
}

function FooterCol({ title, links }) {
  return (
    <div>
      <div className="h-mono" style={{ fontSize: 11, letterSpacing: '0.22em', color: 'rgba(243,240,245,0.55)', textTransform: 'uppercase', marginBottom: 16 }}>{title}</div>
      <ul style={{ listStyle: 'none', padding: 0, margin: 0, display: 'flex', flexDirection: 'column', gap: 10 }}>
        {links.map((l, i) => (
          <li key={i}>
            <button onClick={l.onClick} style={{ background: 'none', border: 0, color: 'rgba(243,240,245,0.85)', fontSize: 14, padding: 0, cursor: 'pointer', textAlign: 'left' }}
              onMouseEnter={e => e.currentTarget.style.color = '#ffb892'}
              onMouseLeave={e => e.currentTarget.style.color = 'rgba(243,240,245,0.85)'}>
              {l.label}
            </button>
          </li>
        ))}
      </ul>
    </div>
  );
}

function Social({ icon, label }) {
  const paths = {
    ig: <><rect x="3" y="3" width="18" height="18" rx="5" /><circle cx="12" cy="12" r="4" /><circle cx="17.5" cy="6.5" r="0.8" fill="currentColor" /></>,
    'ig-tt': <path d="M14 4v9.5a3.5 3.5 0 1 1-3.5-3.5M14 4c.5 2.5 2 4 4.5 4.5" />,
    x: <><path d="M4 4l16 16" /><path d="M20 4L4 20" /></>,
    ds: <><path d="M5 7c2-1 4-1.5 7-1.5s5 .5 7 1.5l1 10c-2 2-4 2.5-7 2.5l-1-2-1 2c-3 0-5-.5-7-2.5z" /><circle cx="9" cy="13" r="1" fill="currentColor"/><circle cx="15" cy="13" r="1" fill="currentColor"/></>,
    gh: <path d="M12 3a9 9 0 0 0-3 17.5c.5.1.7-.2.7-.5v-2c-2.5.5-3-1-3-1-.5-1-1-1.5-1-1.5-1-.5 0-.5 0-.5 1 .1 1.5 1 1.5 1 1 1.5 2.5 1 3 .8.1-.7.4-1 .7-1.3-2-.2-4-1-4-4.5 0-1 .4-1.8 1-2.5 0-.2-.4-1.2.1-2.5 0 0 .8-.3 2.5 1a8.5 8.5 0 0 1 4.5 0c1.7-1.3 2.5-1 2.5-1 .5 1.3.2 2.3.1 2.5.6.7 1 1.5 1 2.5 0 3.5-2 4.3-4 4.5.4.3.7 1 .7 1.9v3c0 .3.2.6.7.5A9 9 0 0 0 12 3z" />,
  };
  return (
    <a href="#" aria-label={label} title={label}
      style={{ display: 'grid', placeItems: 'center', width: 36, height: 36, borderRadius: 9, background: 'rgba(255,255,255,0.06)', color: 'rgba(243,240,245,0.85)', transition: 'all .15s' }}
      onMouseEnter={e => { e.currentTarget.style.background = '#ffb892'; e.currentTarget.style.color = '#1b1b1f'; }}
      onMouseLeave={e => { e.currentTarget.style.background = 'rgba(255,255,255,0.06)'; e.currentTarget.style.color = 'rgba(243,240,245,0.85)'; }}>
      <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round">{paths[icon]}</svg>
    </a>
  );
}

window.LandingPage = LandingPage;
