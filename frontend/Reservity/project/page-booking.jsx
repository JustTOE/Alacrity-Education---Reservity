/* global React, Logomark, IsometricRoom, FloorPlanSketch, formatHour */
const { useState: useStateBp, useEffect: useEffectBp, useMemo: useMemoBp } = React;

function BookingFlow({ space, hour, ctx, data, onCancel, onConfirm }) {
  if (!space) return null;
  const initialDate = (ctx && ctx.date) ? ctx.date : new Date(data.now);
  const bookedForDate = (ctx && ctx.bookedForDate) || data.bookedForDate;

  // Build a 7-day window starting from initialDate
  const week = useMemoBp(() => Array.from({ length: 7 }, (_, i) => {
    const d = new Date(initialDate); d.setHours(0,0,0,0); d.setDate(d.getDate() + i);
    return d;
  }), [initialDate]);

  const HOURS = Array.from({ length: 14 }, (_, i) => 8 + i); // 8..21

  const [selectedSlot, setSelectedSlot] = useStateBp(null); // { date, hour }
  const [duration, setDuration] = useStateBp(2);
  const [recurring, setRecurring] = useStateBp(false);

  // Pre-resolve schedules for each day
  const schedules = useMemoBp(() => week.map(d => ({ d, sched: bookedForDate(space, d) })), [week, space.id]);

  const isHourBooked = (dayIdx, h) => {
    const sched = schedules[dayIdx].sched;
    return sched.booked.includes(h);
  };
  const eventForCell = (dayIdx, h) => {
    return (schedules[dayIdx].sched.events || []).find(e => h >= e.startH && h < e.endH);
  };

  // Block fits (no booked overlap)
  const slotFits = (dayIdx, startH, dur) => {
    if (startH + dur > 22) return false;
    for (let i = 0; i < dur; i++) if (isHourBooked(dayIdx, startH + i)) return false;
    return true;
  };

  const startH = selectedSlot ? selectedSlot.hour : null;
  const endH = startH != null ? startH + duration : null;
  const selectedDate = selectedSlot ? week[selectedSlot.dayIdx] : null;
  const dayDateStr = selectedDate ? selectedDate.toLocaleDateString('en', { weekday: 'long', month: 'short', day: 'numeric' }) : null;

  // If user picks slot but increases duration past free space, we degrade silently — show warning
  const slotValid = selectedSlot ? slotFits(selectedSlot.dayIdx, selectedSlot.hour, duration) : false;

  return (
    <div role="dialog" aria-modal="true" className="bf-overlay" onClick={onCancel}>
      <div className="bf-modal card archgrid" onClick={e => e.stopPropagation()}>
        <header className="bf-header">
          <div>
            <span className="kicker">Pick a time</span>
            <h2 className="h-display" style={{ fontSize: 32, margin: '4px 0 4px' }}>{space.name}</h2>
            <p className="h-mono" style={{ fontSize: 11, color: 'var(--ink-mute)', margin: 0, letterSpacing: '0.12em' }}>{space.building.toUpperCase()} · FLOOR {space.floor < 0 ? 'B1' : space.floor} · {space.area}M² · {space.seats} SEATS</p>
          </div>
          <button onClick={onCancel} aria-label="Close" className="bf-close">✕</button>
        </header>

        <div className="bf-body">
          {/* Calendar */}
          <div className="bf-cal-wrap">
            <div className="row" style={{ justifyContent: 'space-between', marginBottom: 8 }}>
              <span className="label">{week[0].toLocaleDateString('en', { month: 'short', day: 'numeric' })} – {week[6].toLocaleDateString('en', { month: 'short', day: 'numeric' })}</span>
              <div className="row gap-2" style={{ fontSize: 11 }}>
                <Legend swatch="var(--green-pale)" label="Open" />
                <Legend swatchPattern label="Booked" />
                <Legend swatch="var(--indigo)" label="Selected" textWhite/>
              </div>
            </div>

            <div className="bf-cal" style={{ '--rows': HOURS.length }}>
              {/* Day headers */}
              <div className="bf-cal-row bf-cal-headers">
                <div className="bf-cal-corner"/>
                {week.map((d, i) => {
                  const isToday = d.toDateString() === data.now.toDateString();
                  return (
                    <div key={i} className="bf-cal-dayhead">
                      <span className="h-mono" style={{ fontSize: 9, letterSpacing: '0.16em', color: 'var(--ink-mute)' }}>{d.toLocaleDateString('en', { weekday: 'short' }).toUpperCase()}</span>
                      <span className="h-display" style={{ fontSize: 16 }}>{d.getDate()}</span>
                      {isToday && <span className="bf-today">TODAY</span>}
                    </div>
                  );
                })}
              </div>

              {/* Hour rows */}
              {HOURS.map(h => (
                <div key={h} className="bf-cal-row">
                  <div className="bf-cal-hour">
                    <span className="h-mono" style={{ fontSize: 10, color: 'var(--ink-mute)', letterSpacing: '0.06em' }}>{formatHour(h)}</span>
                  </div>
                  {week.map((d, dIdx) => {
                    const booked = isHourBooked(dIdx, h);
                    const ev = eventForCell(dIdx, h);
                    const sel = selectedSlot && selectedSlot.dayIdx === dIdx && h >= startH && h < endH;
                    const isPast = (d < new Date(data.now.getFullYear(), data.now.getMonth(), data.now.getDate())) || (d.toDateString() === data.now.toDateString() && h < data.now.getHours());
                    let cls = 'bf-cell';
                    if (sel) cls += ' is-selected';
                    else if (booked) cls += ' is-booked';
                    else if (isPast) cls += ' is-past';
                    else cls += ' is-open';
                    return (
                      <button key={dIdx} className={cls}
                        disabled={booked || isPast}
                        title={ev ? `${ev.title} · ${ev.host}` : booked ? 'Booked' : isPast ? 'Past' : `${formatHour(h)} · open`}
                        onClick={() => setSelectedSlot({ dayIdx: dIdx, hour: h })}>
                        {ev && h === ev.startH && <span className="bf-event-label">{ev.title}</span>}
                      </button>
                    );
                  })}
                </div>
              ))}
            </div>
          </div>

          {/* Right rail — selection summary */}
          <aside className="bf-rail">
            <div className="bf-room-card">
              <FloorPlanSketch space={space} width={150} height={94} />
              <div style={{ marginTop: 10 }}>
                <div className="h-mono" style={{ fontSize: 10, letterSpacing: '0.16em', color: 'var(--ink-mute)' }}>{space.type.toUpperCase()}</div>
                <div style={{ fontSize: 14, fontWeight: 600, marginTop: 2 }}>{space.building}</div>
              </div>
            </div>

            {selectedSlot ? (
              <>
                <div className="bf-summary">
                  <span className="kicker">Selected</span>
                  <div className="h-display" style={{ fontSize: 26, marginTop: 4 }}>{formatHour(startH)} → {formatHour(endH)}</div>
                  <div style={{ fontSize: 14, color: 'var(--ink-soft)', marginTop: 2 }}>{dayDateStr}</div>
                </div>

                <div>
                  <span className="label" style={{ fontSize: 11 }}>Duration</span>
                  <div className="row" style={{ gap: 6, marginTop: 8, flexWrap: 'wrap' }}>
                    {[1, 2, 3, 4].map(d => {
                      const fits = slotFits(selectedSlot.dayIdx, selectedSlot.hour, d);
                      return (
                        <button key={d}
                          className={`chip ${duration === d ? 'active' : ''}`}
                          disabled={!fits}
                          style={{ opacity: fits ? 1 : 0.35 }}
                          onClick={() => setDuration(d)}>{d}h</button>
                      );
                    })}
                  </div>
                  {!slotValid && <p style={{ fontSize: 12, color: 'var(--orange-deep)', marginTop: 8 }}>Doesn't fit — pick a shorter duration or another slot.</p>}
                </div>

                <label className="bf-recurring">
                  <input type="checkbox" checked={recurring} onChange={e => setRecurring(e.target.checked)} style={{ marginTop: 4, accentColor: 'var(--indigo)' }} />
                  <div>
                    <div style={{ fontWeight: 600, fontSize: 14 }}>Repeat every {selectedDate.toLocaleDateString('en', { weekday: 'long' })}</div>
                    <div style={{ color: 'var(--ink-mute)', fontSize: 12 }}>For 11 sessions</div>
                  </div>
                </label>

                <div className="bf-actions">
                  <button className="btn btn-ghost" onClick={onCancel}>Cancel</button>
                  <button className="btn btn-primary grow" disabled={!slotValid}
                    onClick={() => onConfirm({ space, startH, endH, recurring, date: selectedDate })}>
                    Confirm — {space.price === 0 ? 'Free' : `$${space.price * duration}`}
                  </button>
                </div>
              </>
            ) : (
              <div className="bf-empty">
                <div className="h-display" style={{ fontSize: 22, marginBottom: 6 }}>Tap any open slot.</div>
                <p style={{ fontSize: 13, color: 'var(--ink-mute)' }}>Green is open. Hatched is booked. Past hours are dimmed. Pick a starting hour, set the duration on the right.</p>
              </div>
            )}
          </aside>
        </div>
      </div>

      <style>{`
        .bf-overlay { position: fixed; inset: 0; z-index: 100; background: rgba(27,27,31,0.55); backdrop-filter: blur(6px); display: grid; place-items: center; padding: 24px; animation: bfFade .2s; }
        @keyframes bfFade { from { opacity: 0 } to { opacity: 1 } }
        .bf-modal { width: min(1080px, 100%); max-height: calc(100vh - 48px); padding: 0; display: flex; flex-direction: column; overflow: hidden; }
        .bf-header { padding: 24px 28px; border-bottom: 1px solid var(--line-soft); display: flex; justify-content: space-between; align-items: flex-start; gap: 16px; }
        .bf-close { font-size: 20px; color: var(--ink-mute); width: 32px; height: 32px; border-radius: 999px; }
        .bf-close:hover { background: var(--paper-2); color: var(--ink); }
        .bf-body { display: grid; grid-template-columns: minmax(0, 1.5fr) 320px; gap: 0; flex: 1; min-height: 0; }
        .bf-cal-wrap { padding: 20px 24px; overflow-y: auto; border-right: 1px solid var(--line-soft); }
        .bf-cal { display: grid; gap: 0; border: 1px solid var(--line-soft); border-radius: 10px; overflow: hidden; background: #fff; }
        .bf-cal-row { display: grid; grid-template-columns: 56px repeat(7, 1fr); }
        .bf-cal-row + .bf-cal-row { border-top: 1px solid var(--line-soft); }
        .bf-cal-headers { background: var(--paper-2); position: sticky; top: 0; z-index: 2; }
        .bf-cal-corner { border-right: 1px solid var(--line-soft); }
        .bf-cal-dayhead { padding: 10px 4px; text-align: center; display: flex; flex-direction: column; align-items: center; gap: 2px; border-left: 1px solid var(--line-soft); position: relative; }
        .bf-today { position: absolute; bottom: 2px; font-size: 8px; letter-spacing: 0.18em; color: var(--orange-deep); font-family: 'JetBrains Mono', monospace; font-weight: 700; }
        .bf-cal-hour { padding: 0 8px; display: flex; align-items: center; justify-content: flex-end; border-right: 1px solid var(--line-soft); height: 32px; }
        .bf-cell { height: 32px; border: 0; border-left: 1px solid var(--line-soft); cursor: pointer; transition: background .12s; position: relative; padding: 0; }
        .bf-cell.is-open { background: var(--green-pale); }
        .bf-cell.is-open:hover { background: rgba(123,191,99,0.4); }
        .bf-cell.is-booked { background: repeating-linear-gradient(45deg, #e4e1e7, #e4e1e7 3px, #f0edf2 3px, #f0edf2 6px); cursor: not-allowed; }
        .bf-cell.is-past { background: var(--paper-3); cursor: not-allowed; opacity: 0.5; }
        .bf-cell.is-selected { background: var(--indigo); }
        .bf-event-label { position: absolute; left: 4px; right: 4px; top: 4px; font-size: 9px; font-weight: 600; color: var(--ink); letter-spacing: 0.04em; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; pointer-events: none; }
        .bf-cell.is-booked .bf-event-label { color: var(--ink-soft); }

        .bf-rail { padding: 24px; display: flex; flex-direction: column; gap: 18px; overflow-y: auto; background: var(--paper); }
        .bf-room-card { background: linear-gradient(180deg,#f6f2f8,#fff); border: 1px solid var(--line-soft); border-radius: 12px; padding: 14px; }
        .bf-summary { padding: 14px; background: linear-gradient(135deg, rgba(75,82,167,0.08), rgba(255,130,60,0.06)); border-radius: 10px; border: 1px solid rgba(75,82,167,0.18); }
        .bf-recurring { display: flex; gap: 12px; padding: 12px; border: 1px solid var(--line-soft); border-radius: 10px; cursor: pointer; align-items: flex-start; }
        .bf-actions { display: flex; gap: 8px; margin-top: auto; padding-top: 12px; border-top: 1px solid var(--line-soft); }
        .bf-empty { padding: 24px 16px; text-align: center; }

        @media (max-width: 880px) {
          .bf-body { grid-template-columns: 1fr; }
          .bf-rail { border-top: 1px solid var(--line-soft); }
          .bf-cal-wrap { border-right: 0; }
        }
      `}</style>
    </div>
  );
}

function Legend({ swatch, swatchPattern, label, textWhite }) {
  const sw = swatchPattern
    ? { background: 'repeating-linear-gradient(45deg, #e4e1e7, #e4e1e7 3px, #f0edf2 3px, #f0edf2 6px)' }
    : { background: swatch };
  return (
    <span className="row" style={{ gap: 5, color: 'var(--ink-mute)' }}>
      <span style={{ width: 14, height: 10, borderRadius: 2, ...sw }}/>
      <span className="h-mono" style={{ letterSpacing: '0.08em', fontSize: 10 }}>{label.toUpperCase()}</span>
    </span>
  );
}

function BookingPass({ booking, onBack, onShare }) {
  const { space, startH, endH, recurring, date } = booking;
  const passDate = date || new Date(2026, 4, 2);
  const dateStr = passDate.toLocaleDateString('en', { weekday: 'long', month: 'long', day: 'numeric' }).toUpperCase();
  const passId = `RV-${space.id.toUpperCase().replace(/-/g,'')}-${String(Date.now()).slice(-4)}`;

  return (
    <div style={{ background: 'var(--paper-3)', minHeight: 'calc(100vh - 65px)', padding: '40px 24px' }}>
      <div className="container" style={{ maxWidth: 980 }}>
        <div className="row" style={{ justifyContent: 'space-between', marginBottom: 24, flexWrap: 'wrap', gap: 12 }}>
          <button className="btn btn-ghost" onClick={onBack}>← Back to spaces</button>
          <div className="row gap-3">
            <button className="btn btn-ghost" onClick={onShare}>↗ Share</button>
            <button className="btn btn-primary" onClick={() => window.print()}>↓ Download pass</button>
          </div>
        </div>

        <div style={{ display: 'grid', gridTemplateColumns: 'minmax(0, 1fr) auto', gap: 32, alignItems: 'start' }} className="pass-grid">
          {/* Pass poster */}
          <article className="pass grain" aria-label="Booking pass" style={{
            position: 'relative', width: '100%', maxWidth: 540, aspectRatio: '1080/1920',
            borderRadius: 28, overflow: 'hidden', color: '#fff', boxShadow: 'var(--shadow-lg)',
            margin: '0 auto', display: 'flex', flexDirection: 'column',
          }}>
            <div style={{ position: 'absolute', right: -60, top: -60, opacity: 0.18 }}>
              <Logomark size={360} color="#fff" accent="#fff" detail={true} />
            </div>
            <svg style={{ position: 'absolute', inset: 0, opacity: 0.18 }} aria-hidden="true">
              <defs>
                <pattern id="passgrid" width="32" height="32" patternUnits="userSpaceOnUse">
                  <path d="M 32 0 L 0 0 0 32" fill="none" stroke="#fff" strokeWidth="1" />
                </pattern>
              </defs>
              <rect width="100%" height="100%" fill="url(#passgrid)" />
            </svg>

            <header style={{ padding: '32px 32px 0', display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', position: 'relative', zIndex: 2 }}>
              <Logomark size={28} color="#fff" accent="#fff" detail={false}/>
              <span className="h-mono" style={{ fontSize: 11, letterSpacing: '0.18em', opacity: 0.9 }}>RESERVITY · PASS</span>
            </header>

            <div style={{ padding: '40px 32px 0', position: 'relative', zIndex: 2 }}>
              <span className="h-mono" style={{ fontSize: 11, letterSpacing: '0.22em', opacity: 0.85 }}>{dateStr}</span>
              <h1 className="h-display" style={{ fontSize: 'clamp(48px, 8vw, 72px)', margin: '12px 0 0', lineHeight: 0.95 }}>
                {space.name.split(' ').slice(0,-1).join(' ')}<br/>
                <span style={{ color: '#ffd9bf' }}>{space.name.split(' ').slice(-1)}</span>
              </h1>
              <p style={{ marginTop: 16, fontSize: 18, opacity: 0.95 }}>{space.blurb}</p>
            </div>

            <div style={{ flex: 1, display: 'grid', placeItems: 'center', padding: 16, position: 'relative', zIndex: 2 }}>
              <svg viewBox="0 0 360 240" style={{ width: '90%', height: 'auto' }} aria-hidden="true">
                <g fill="none" stroke="#fff" strokeWidth="1.4" strokeLinecap="round" strokeLinejoin="round" opacity="0.95">
                  <path d="M70 160 L180 100 L290 160 L180 220 Z" />
                  <path d="M70 160 L70 80 L180 20 L180 100 Z" />
                  <path d="M180 100 L180 20 L290 80 L290 160 Z" />
                  <path d="M130 130 L200 95 L250 120 L180 155 Z" />
                  <path d="M130 130 L130 138 L180 163 L180 155" />
                  <path d="M250 120 L250 128 L180 163" />
                  <path d="M170 100 L210 80 L210 60 L170 80 Z" />
                  <path d="M150 168 L185 152 L210 165 L175 181 Z" />
                </g>
                <g fill="none" stroke="#fff" strokeWidth="1" strokeDasharray="3 4" opacity="0.55">
                  <line x1="20" y1="220" x2="60" y2="170" />
                  <line x1="300" y1="170" x2="340" y2="220" />
                </g>
                <text x="20" y="232" fontFamily="JetBrains Mono" fontSize="9" fill="#fff" letterSpacing="0.18em" opacity="0.8">{space.area}M² · {space.seats} SEATS</text>
                <text x="340" y="232" fontFamily="JetBrains Mono" fontSize="9" fill="#fff" letterSpacing="0.18em" opacity="0.8" textAnchor="end">FLOOR {space.floor}</text>
              </svg>
            </div>

            <div style={{ borderTop: '1.5px dashed rgba(255,255,255,0.55)', margin: '0 24px', position: 'relative', zIndex: 2 }} />

            <footer style={{ padding: '24px 32px 32px', display: 'grid', gridTemplateColumns: '1fr auto', gap: 20, alignItems: 'flex-end', position: 'relative', zIndex: 2 }}>
              <div>
                <div className="h-mono" style={{ fontSize: 11, letterSpacing: '0.22em', opacity: 0.85 }}>WHEN</div>
                <div className="h-display" style={{ fontSize: 32, margin: '4px 0 12px' }}>{formatHour(startH)} — {formatHour(endH)}</div>
                <div className="h-mono" style={{ fontSize: 11, letterSpacing: '0.22em', opacity: 0.85 }}>WHO</div>
                <div style={{ fontSize: 18, fontWeight: 600 }}>Maya R. · maya@university.edu</div>
                {recurring && <div className="h-mono" style={{ fontSize: 10, letterSpacing: '0.18em', marginTop: 6, padding: '3px 8px', background: 'rgba(255,255,255,0.18)', borderRadius: 999, display: 'inline-block' }}>↻ EVERY {passDate.toLocaleDateString('en', { weekday: 'short' }).toUpperCase()} · 11 SESSIONS</div>}
                <div className="h-mono" style={{ fontSize: 10, letterSpacing: '0.18em', opacity: 0.7, marginTop: 14 }}>{passId}</div>
              </div>
              <QRCodePlaceholder />
            </footer>
          </article>

          <aside style={{ minWidth: 260, maxWidth: 320 }}>
            <div className="card" style={{ padding: 20, marginBottom: 12 }}>
              <span className="kicker">You're in.</span>
              <h3 className="h-headline" style={{ fontSize: 22, margin: '8px 0 8px' }}>Pass saved.</h3>
              <p style={{ color: 'var(--ink-soft)', fontSize: 14, margin: 0 }}>Find this in your dashboard. Show the QR at the door — or just walk in, your face is already on the list.</p>
            </div>
            <div className="card" style={{ padding: 20 }}>
              <span className="label">Tip</span>
              <p style={{ fontSize: 14, margin: '8px 0 0' }}>Long-press to save the pass to Photos at 1080×1920. It's screenshot-shaped on purpose.</p>
            </div>
          </aside>
        </div>
      </div>

      <style>{`
        @media print {
          @page { margin: 0 }
          body { background: #fff }
          .nav, .container > .row, aside { display: none !important }
          .pass { box-shadow: none !important; max-width: 100% !important; aspect-ratio: 1080/1920 !important; height: 100vh; }
        }
        @media (max-width: 820px) {
          .pass-grid { grid-template-columns: 1fr !important; }
        }
      `}</style>
    </div>
  );
}

function QRCodePlaceholder() {
  const cells = Array.from({ length: 49 }, (_, i) => ((i * 31 + 7) % 11) > 5);
  [0,1,2,7,8,9,14,15,16, 4,5,6,11,12,13,18,19,20, 28,29,30,35,36,37,42,43,44].forEach(i => cells[i] = (i % 3 !== 1));
  return (
    <div style={{ background: '#fff', padding: 8, borderRadius: 10, width: 90, height: 90, display: 'grid', gridTemplateColumns: 'repeat(7, 1fr)', gap: 1 }}>
      {cells.map((on, i) => (
        <div key={i} style={{ background: on ? '#1b1b1f' : 'transparent', borderRadius: 1 }}/>
      ))}
    </div>
  );
}

window.BookingFlow = BookingFlow;
window.BookingPass = BookingPass;
