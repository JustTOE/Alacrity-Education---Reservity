/* global React, FloorPlanSketch, formatHour, getStatus */
const { useState: useStateAd, useMemo: useMemoAd, useEffect: useEffectAd, useRef: useRefAd } = React;

// ============================================================================
// Synthetic admin fixture data — derived from RESERVITY_DATA
// ============================================================================
function buildAdminData(data) {
  const { SPACES, now } = data;

  // Listing state extensions (visibility, flags, edits) — admin-only metadata
  const listingState = SPACES.map((s, i) => ({
    id: s.id,
    visibility: i === 4 ? 'hidden' : (i === 8 ? 'maintenance' : 'live'), // darkroom hidden, bio-lab in maintenance
    spotlight: ['atrium', 'pod-3', 'studio-grand'].includes(s.id),
    flagged: s.id === 'studio-c' ? { by: 'Maya R.', reason: 'Smell complaint, Apr 27', count: 2 } : null,
    quietHours: s.id === 'pod-7' ? { from: 22, to: 8 } : null,
    capacityOverride: null,
    surprise: !!s.surprise,
    revenue30d: Math.round((s.price || 1) * (s.todayBookings || 1) * 12 + (s.id.length * 31) % 90),
    bookings30d: Math.round((s.todayBookings || 2) * 9 + ((s.id.charCodeAt(0) * 7) % 14)),
    occupancyTrend: genTrend(s.id, 14),
    auditLog: makeAuditLog(s, now),
  }));

  // Users — synthesize ~24 users with handles, statuses, role
  const firstNames = ['Mira','Devon','Maya','Priya','Joaquin','Lola','Theo','Ines','Kai','Noor','Owen','Sasha','Eli','Anya','Felix','Yui','Reza','Cleo','Asa','Junie','Tobi','Vesper','Indra','Boaz'];
  const lastNames  = ['Adesanya','Khoury','Rodríguez','Singh','Iglesias','Park','Chen','Okafor','Tanaka','Voss','Liu','Mendel','Bauer','Petrov','Rao','Lim','Mehta','Brennan','Yusuf','Calder','Ono','Greene','Devi','Marsh'];
  const programs = ['Architecture · M1','Chemistry · BS3','CS · BS2','Music Tech · M2','Visual Arts · BFA4','Biology · PhD2','Design · BS1','Mech Eng · MS','Linguistics · BA3','Library Sci · M1'];
  const seedRng = (() => { let s = 1337; return () => { s = (s * 1664525 + 1013904223) >>> 0; return s / 0x100000000; }; })();
  const users = firstNames.map((fn, i) => {
    const ln = lastNames[i];
    const id = `u_${i+1}`;
    const handle = (fn + ln[0]).toLowerCase();
    const joinedDays = Math.floor(seedRng() * 480) + 3; // 3..483 days ago
    const joined = new Date(now); joined.setDate(joined.getDate() - joinedDays);
    const last = new Date(now); last.setMinutes(last.getMinutes() - Math.floor(seedRng() * 4000));
    const status = i === 7 ? 'banned' : (i === 3 ? 'pending' : (joinedDays > 380 ? 'inactive' : 'active'));
    const role = (i === 0) ? 'admin' : (i % 9 === 2 ? 'host' : 'student');
    return {
      id, fullName: `${fn} ${ln}`, firstName: fn, handle, email: `${handle}@hawthorn.edu`,
      program: programs[i % programs.length],
      joined, lastActive: last, status, role,
      bookings: Math.floor(seedRng() * 90) + 1,
      hours: Math.floor(seedRng() * 280) + 4,
      avatarHue: Math.floor(seedRng() * 360),
      verified: i % 4 !== 1,
      flags: i === 7 ? ['no-show x3'] : (i === 12 ? ['late-cancel'] : []),
    };
  });

  // Activity feed
  const activity = makeActivity(SPACES, users, now);

  return { listingState, users, activity };
}

function genTrend(seedStr, n) {
  let s = 0; for (let i = 0; i < seedStr.length; i++) s = (s * 31 + seedStr.charCodeAt(i)) >>> 0;
  const out = [];
  let v = 0.4 + ((s % 30) / 100);
  for (let i = 0; i < n; i++) {
    s = (s * 1103515245 + 12345) >>> 0;
    v = Math.max(0.05, Math.min(1, v + ((s % 100) - 50) / 220));
    out.push(v);
  }
  return out;
}

function makeAuditLog(space, now) {
  const log = [
    { mins: 14, who: 'auto', what: 'Booking auto-confirmed', detail: 'Devon K. · 14:00–16:00' },
    { mins: 95, who: 'kara@admin', what: 'Description edited', detail: '+12 words' },
    { mins: 4*60 + 23, who: 'auto', what: 'Occupancy snapshot', detail: `${space.booked.length} booked hrs` },
    { mins: 26*60, who: 'kara@admin', what: 'Price changed', detail: `${space.price ? `$${space.price-1} → $${space.price}` : 'free'}` },
    { mins: 3*24*60 + 11*60, who: 'kara@admin', what: 'Created listing', detail: 'Initial draft' },
  ];
  return log.map(l => {
    const t = new Date(now); t.setMinutes(t.getMinutes() - l.mins);
    return { ...l, at: t };
  });
}

function makeActivity(spaces, users, now) {
  const verbs = [
    { v: 'booked', tone: 'indigo' },
    { v: 'cancelled', tone: 'orange' },
    { v: 'no-showed', tone: 'red' },
    { v: 'checked into', tone: 'green' },
    { v: 'reviewed', tone: 'indigo' },
    { v: 'flagged', tone: 'orange' },
    { v: 'signed up', tone: 'green' },
    { v: 'enabled 2FA on', tone: 'indigo' },
  ];
  const out = [];
  let s = 88;
  const rng = () => { s = (s * 1664525 + 1013904223) >>> 0; return s / 0x100000000; };
  for (let i = 0; i < 36; i++) {
    const u = users[Math.floor(rng() * users.length)];
    const sp = spaces[Math.floor(rng() * spaces.length)];
    const action = verbs[Math.floor(rng() * verbs.length)];
    const t = new Date(now);
    t.setMinutes(t.getMinutes() - Math.floor(rng() * 12 * 60));
    out.push({ id: `a${i}`, user: u, action, space: sp, at: t });
  }
  return out.sort((a, b) => b.at - a.at);
}

// ============================================================================
// Top-level admin page
// ============================================================================
function AdminPage({ data }) {
  const admin = useMemoAd(() => buildAdminData(data), [data]);

  const [tab, setTab] = useStateAd('overview');
  const [openListingId, setOpenListingId] = useStateAd(null);
  const [overrides, setOverrides] = useStateAd({}); // id -> partial listingState
  const [toast, setToast] = useStateAd(null);

  const showToast = (m) => { setToast(m); setTimeout(() => setToast(null), 2400); };

  const merged = data.SPACES.map(s => {
    const base = admin.listingState.find(x => x.id === s.id);
    return { ...s, _state: { ...base, ...(overrides[s.id] || {}) } };
  });

  const setListing = (id, patch) => setOverrides(prev => ({ ...prev, [id]: { ...(prev[id] || {}), ...patch } }));

  const openListing = merged.find(s => s.id === openListingId);

  return (
    <div className="adm">
      <AdminHeader data={data} merged={merged} users={admin.users}/>

      <div className="adm-tabs-row">
        <div className="adm-container">
          <div className="adm-tabs" role="tablist">
            {[
              { id: 'overview', label: 'Overview', sub: 'snapshot' },
              { id: 'listings', label: 'Listings', sub: `${merged.length} spaces` },
              { id: 'users', label: 'Users', sub: `${admin.users.length} accounts` },
              { id: 'activity', label: 'Activity', sub: 'live feed' },
            ].map(t => (
              <button key={t.id} role="tab" aria-selected={tab === t.id}
                className={`adm-tab ${tab === t.id ? 'active' : ''}`}
                onClick={() => setTab(t.id)}>
                <span>{t.label}</span>
                <span className="adm-tab-sub">{t.sub}</span>
              </button>
            ))}
          </div>
        </div>
      </div>

      <div className="adm-container adm-body">
        {tab === 'overview' && <OverviewTab merged={merged} users={admin.users} activity={admin.activity} onOpen={(id) => { setTab('listings'); setOpenListingId(id); }}/>}
        {tab === 'listings' && <ListingsTab merged={merged} onOpen={setOpenListingId}/>}
        {tab === 'users' && <UsersTab users={admin.users} showToast={showToast}/>}
        {tab === 'activity' && <ActivityTab activity={admin.activity}/>}
      </div>

      {openListing && (
        <ListingDrawer
          space={openListing}
          onClose={() => setOpenListingId(null)}
          onChange={(patch) => setListing(openListing.id, patch)}
          showToast={showToast}
        />
      )}

      {toast && <div className="adm-toast">{toast}</div>}

      <style>{adminCSS}</style>
    </div>
  );
}

// ============================================================================
// Header — title + global pulse
// ============================================================================
function AdminHeader({ data, merged, users }) {
  const live = merged.filter(s => s._state.visibility === 'live').length;
  const hidden = merged.filter(s => s._state.visibility !== 'live').length;
  const flagged = merged.filter(s => s._state.flagged).length;
  return (
    <div className="adm-header">
      <div className="adm-container">
        <div className="adm-header-row">
          <div>
            <div className="row gap-2" style={{ marginBottom: 6 }}>
              <span className="adm-shield" aria-hidden="true">
                <svg width="14" height="14" viewBox="0 0 16 16" fill="none" stroke="currentColor" strokeWidth="2"><path d="M8 1.5 L13.5 4 V8.5 C13.5 11.5 11 13.5 8 14.5 C5 13.5 2.5 11.5 2.5 8.5 V4 Z"/></svg>
                ADMIN CONSOLE
              </span>
              <span className="adm-build">build 26.05 · {data.now.toLocaleDateString('en', { month: 'short', day: 'numeric' })}</span>
            </div>
            <h1 className="adm-h1">Hawthorn Campus, at a glance.</h1>
            <p className="adm-h1-sub">{live} listings live · {hidden} off the floor · {flagged} flagged · {users.length} accounts</p>
          </div>
          <div className="adm-pulse-card">
            <div className="adm-pulse-dot"/>
            <div>
              <div className="adm-pulse-label">PLATFORM PULSE</div>
              <div className="adm-pulse-value">Healthy</div>
              <div className="adm-pulse-sub">All buildings reachable · last sync 41s ago</div>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}

// ============================================================================
// OVERVIEW
// ============================================================================
function OverviewTab({ merged, users, activity, onOpen }) {
  const live = merged.filter(s => s._state.visibility === 'live').length;
  const totalBookings30 = merged.reduce((a, s) => a + s._state.bookings30d, 0);
  const revenue30 = merged.reduce((a, s) => a + s._state.revenue30d, 0);
  const newUsers7 = users.filter(u => (Date.now() - u.joined.getTime()) < 7*86400000).length || 7;

  // Buildings rollup
  const byBuilding = useMemoAd(() => {
    const m = {};
    merged.forEach(s => {
      const b = s.building;
      if (!m[b]) m[b] = { name: b, count: 0, hours: 0, hidden: 0, flagged: 0, revenue: 0 };
      m[b].count += 1;
      m[b].hours += s.booked.length;
      m[b].revenue += s._state.revenue30d;
      if (s._state.visibility !== 'live') m[b].hidden += 1;
      if (s._state.flagged) m[b].flagged += 1;
    });
    return Object.values(m).sort((a, b) => b.count - a.count);
  }, [merged]);

  const needsAttention = merged.filter(s => s._state.flagged || s._state.visibility !== 'live');

  return (
    <>
      <section className="adm-stats">
        <BigMetric label="Registered users" value={users.length} delta={`+${newUsers7} this week`} trend={[0.4,0.5,0.45,0.6,0.55,0.7,0.78,0.82,0.86,0.9]}/>
        <BigMetric label="Active listings" value={live} total={merged.length} delta={`${merged.length - live} off the floor`} trend={[0.6,0.62,0.65,0.7,0.72,0.7,0.75,0.78,0.82,0.85]} tone="indigo"/>
        <BigMetric label="Bookings · 30d" value={totalBookings30} delta="↗ 12% vs prior 30d" trend={[0.3,0.4,0.5,0.55,0.65,0.6,0.7,0.78,0.86,0.92]}/>
        <BigMetric label="Revenue · 30d" value={`$${revenue30.toLocaleString()}`} delta="↗ 8% vs prior 30d" trend={[0.5,0.55,0.5,0.6,0.65,0.62,0.7,0.74,0.78,0.86]} tone="orange"/>
      </section>

      <div className="adm-grid-2">
        {/* Needs attention */}
        <section className="adm-card">
          <div className="adm-card-head">
            <h3 className="adm-h3">Needs your eye</h3>
            <span className="adm-mono">{needsAttention.length} ITEMS</span>
          </div>
          {needsAttention.length === 0 ? (
            <div className="adm-empty">Inbox zero. Everything is behaving.</div>
          ) : (
            <ul className="adm-attn">
              {needsAttention.map(s => (
                <li key={s.id} className="adm-attn-row" onClick={() => onOpen(s.id)}>
                  <span className={`adm-attn-mark ${s._state.flagged ? 'flag' : s._state.visibility === 'hidden' ? 'hide' : 'maint'}`}/>
                  <div className="grow">
                    <div className="row gap-2" style={{ flexWrap: 'wrap' }}>
                      <strong style={{ fontSize: 14 }}>{s.name}</strong>
                      <VisibilityPill v={s._state.visibility}/>
                      {s._state.flagged && <span className="pill pill-red">⚑ flagged</span>}
                    </div>
                    <div className="adm-mono adm-attn-sub">
                      {s._state.flagged ? `${s._state.flagged.reason}` : s._state.visibility === 'hidden' ? 'Hidden from listings · direct link only' : 'In maintenance mode · bookings paused'}
                    </div>
                  </div>
                  <span className="adm-chev">→</span>
                </li>
              ))}
            </ul>
          )}
        </section>

        {/* Buildings rollup */}
        <section className="adm-card">
          <div className="adm-card-head">
            <h3 className="adm-h3">By building</h3>
            <span className="adm-mono">{byBuilding.length} BLDGS</span>
          </div>
          <div className="col gap-3" style={{ marginTop: 4 }}>
            {byBuilding.map(b => {
              const max = Math.max(...byBuilding.map(x => x.revenue));
              return (
                <div key={b.name}>
                  <div className="row" style={{ justifyContent: 'space-between', marginBottom: 6 }}>
                    <div>
                      <div style={{ fontSize: 14, fontWeight: 600 }}>{b.name}</div>
                      <div className="adm-mono" style={{ marginTop: 2 }}>{b.count} LISTINGS · {b.hours}H BOOKED TODAY{b.hidden ? ` · ${b.hidden} HIDDEN` : ''}{b.flagged ? ` · ${b.flagged} FLAGGED` : ''}</div>
                    </div>
                    <div className="h-display" style={{ fontSize: 20 }}>${b.revenue}</div>
                  </div>
                  <div className="adm-bar"><div className="adm-bar-fill" style={{ width: `${(b.revenue / max) * 100}%` }}/></div>
                </div>
              );
            })}
          </div>
        </section>
      </div>

      <section className="adm-card" style={{ marginTop: 16 }}>
        <div className="adm-card-head">
          <h3 className="adm-h3">Recent activity</h3>
          <span className="adm-mono">LAST 24H</span>
        </div>
        <ul className="adm-activity">
          {activity.slice(0, 8).map(a => (
            <ActivityRow key={a.id} a={a}/>
          ))}
        </ul>
      </section>
    </>
  );
}

function BigMetric({ label, value, total, delta, trend, tone }) {
  const w = 120, h = 36;
  const max = Math.max(...trend);
  const pts = trend.map((v, i) => `${(i/(trend.length-1))*w},${h - (v/max)*(h-4) - 2}`).join(' ');
  return (
    <div className={`adm-metric ${tone ? `tone-${tone}` : ''}`}>
      <div className="adm-metric-label">{label.toUpperCase()}</div>
      <div className="row" style={{ alignItems: 'flex-end', justifyContent: 'space-between', marginTop: 6 }}>
        <div>
          <span className="h-display adm-metric-value">{value}</span>
          {total !== undefined && <span className="adm-metric-total"> / {total}</span>}
        </div>
        <svg width={w} height={h} viewBox={`0 0 ${w} ${h}`} aria-hidden="true">
          <polyline points={pts} fill="none" stroke="currentColor" strokeWidth="1.5" strokeLinejoin="round" strokeLinecap="round" opacity="0.85"/>
          <circle cx={w} cy={h - (trend[trend.length-1]/max)*(h-4) - 2} r="2.5" fill="currentColor"/>
        </svg>
      </div>
      <div className="adm-metric-delta">{delta}</div>
    </div>
  );
}

// ============================================================================
// LISTINGS TAB
// ============================================================================
function ListingsTab({ merged, onOpen }) {
  const [q, setQ] = useStateAd('');
  const [vfilter, setVfilter] = useStateAd('all');
  const [typeFilter, setTypeFilter] = useStateAd('all');
  const [sort, setSort] = useStateAd('name');

  const filtered = useMemoAd(() => {
    let xs = merged.slice();
    if (q) xs = xs.filter(s => (s.name + s.building).toLowerCase().includes(q.toLowerCase()));
    if (vfilter !== 'all') xs = xs.filter(s => s._state.visibility === vfilter || (vfilter === 'flagged' && s._state.flagged) || (vfilter === 'spotlight' && s._state.spotlight));
    if (typeFilter !== 'all') xs = xs.filter(s => s.type === typeFilter);
    xs.sort((a, b) => {
      if (sort === 'name') return a.name.localeCompare(b.name);
      if (sort === 'bookings') return b._state.bookings30d - a._state.bookings30d;
      if (sort === 'revenue') return b._state.revenue30d - a._state.revenue30d;
      if (sort === 'occupancy') return b.booked.length - a.booked.length;
      return 0;
    });
    return xs;
  }, [merged, q, vfilter, typeFilter, sort]);

  return (
    <>
      <div className="adm-toolbar">
        <div className="adm-search">
          <svg width="16" height="16" viewBox="0 0 16 16" fill="none" stroke="currentColor" strokeWidth="2"><circle cx="7" cy="7" r="5"/><path d="M11 11 L14 14"/></svg>
          <input value={q} onChange={e => setQ(e.target.value)} placeholder="Search listings or buildings…" aria-label="Search listings"/>
        </div>
        <div className="adm-filters row gap-2">
          <FilterChip label="All states" v="all" current={vfilter} set={setVfilter}/>
          <FilterChip label="Live" v="live" current={vfilter} set={setVfilter}/>
          <FilterChip label="Hidden" v="hidden" current={vfilter} set={setVfilter}/>
          <FilterChip label="Maintenance" v="maintenance" current={vfilter} set={setVfilter}/>
          <FilterChip label="⚑ Flagged" v="flagged" current={vfilter} set={setVfilter}/>
          <FilterChip label="★ Spotlight" v="spotlight" current={vfilter} set={setVfilter}/>
        </div>
        <select className="adm-select" value={sort} onChange={e => setSort(e.target.value)} aria-label="Sort by">
          <option value="name">Sort · Name (A→Z)</option>
          <option value="bookings">Sort · Bookings 30d</option>
          <option value="revenue">Sort · Revenue 30d</option>
          <option value="occupancy">Sort · Today's occupancy</option>
        </select>
      </div>

      <div className="adm-table">
        <div className="adm-thead">
          <div>Listing</div>
          <div>State</div>
          <div className="adm-th-num">Today</div>
          <div className="adm-th-num">30d</div>
          <div className="adm-th-num">Revenue</div>
          <div>Trend</div>
          <div></div>
        </div>

        {filtered.map(s => <ListingRow key={s.id} space={s} onOpen={() => onOpen(s.id)}/>)}

        {filtered.length === 0 && (
          <div className="adm-empty" style={{ padding: 40, textAlign: 'center' }}>
            No listings match those filters.
          </div>
        )}
      </div>
    </>
  );
}

function FilterChip({ label, v, current, set }) {
  return (
    <button className={`adm-fchip ${current === v ? 'active' : ''}`} onClick={() => set(v)}>{label}</button>
  );
}

function ListingRow({ space, onOpen }) {
  const s = space;
  const st = s._state;
  const todayMax = 14;
  const occ = Math.min(1, s.booked.length / todayMax);
  return (
    <div className="adm-row" onClick={onOpen}>
      <div className="adm-row-listing">
        <div className="adm-row-mini">
          <FloorPlanSketch space={s} width={56} height={36}/>
        </div>
        <div style={{ minWidth: 0 }}>
          <div className="row gap-2" style={{ flexWrap: 'wrap' }}>
            <strong className="adm-row-name">{s.name}</strong>
            {st.spotlight && <span className="adm-tag tag-spotlight" title="Spotlight">★ Spotlight</span>}
            {st.flagged && <span className="adm-tag tag-flag" title="Flagged">⚑ {st.flagged.count}</span>}
            {st.surprise && <span className="adm-tag tag-surprise" title="Hidden discovery">◐ Surprise</span>}
            {st.quietHours && <span className="adm-tag tag-quiet" title="Quiet hours">⏾ Quiet</span>}
          </div>
          <div className="adm-mono adm-row-meta">{s.type.toUpperCase()} · {s.building.toUpperCase()} · FL {s.floor < 0 ? 'B1' : s.floor} · {s.seats} SEATS · {s.price ? `$${s.price}/HR` : 'FREE'}</div>
        </div>
      </div>
      <div><VisibilityPill v={st.visibility}/></div>
      <div className="adm-th-num">
        <div className="adm-row-occ">
          <div className="adm-row-occ-bar"><div style={{ width: `${occ * 100}%` }}/></div>
          <span className="adm-mono">{s.booked.length}H</span>
        </div>
      </div>
      <div className="adm-th-num adm-numeric">{st.bookings30d}</div>
      <div className="adm-th-num adm-numeric">${st.revenue30d}</div>
      <div>
        <MiniTrend trend={st.occupancyTrend}/>
      </div>
      <div className="adm-row-chev">→</div>
    </div>
  );
}

function MiniTrend({ trend }) {
  const w = 90, h = 22;
  const max = Math.max(...trend);
  const pts = trend.map((v, i) => `${(i/(trend.length-1))*w},${h - (v/max)*(h-2) - 1}`).join(' ');
  return (
    <svg width={w} height={h} viewBox={`0 0 ${w} ${h}`}>
      <polyline points={pts} fill="none" stroke="var(--indigo)" strokeWidth="1.2" strokeLinejoin="round" strokeLinecap="round" opacity="0.6"/>
    </svg>
  );
}

function VisibilityPill({ v }) {
  const map = {
    live: { cls: 'vp-live', label: '● Live' },
    hidden: { cls: 'vp-hidden', label: '◌ Hidden' },
    maintenance: { cls: 'vp-maint', label: '⚒ Maintenance' },
    draft: { cls: 'vp-draft', label: '✎ Draft' },
  };
  const m = map[v] || map.live;
  return <span className={`vp ${m.cls}`}>{m.label}</span>;
}

// ============================================================================
// LISTING DRAWER — the creative edit panel
// ============================================================================
function ListingDrawer({ space, onClose, onChange, showToast }) {
  const s = space;
  const st = s._state;
  const [tab, setTab] = useStateAd('control');

  // Local form state for inline edits
  const [name, setName] = useStateAd(s.name);
  const [blurb, setBlurb] = useStateAd(s.blurb);
  const [price, setPrice] = useStateAd(s.price);
  const [capOverride, setCapOverride] = useStateAd(st.capacityOverride ?? s.seats);
  const [qh, setQh] = useStateAd(st.quietHours);
  const [edited, setEdited] = useStateAd(false);

  useEffectAd(() => {
    const onKey = (e) => { if (e.key === 'Escape') onClose(); };
    document.addEventListener('keydown', onKey);
    return () => document.removeEventListener('keydown', onKey);
  }, [onClose]);

  const save = () => {
    showToast(`Saved changes to ${name}`);
    setEdited(false);
  };

  return (
    <>
      <div className="adm-scrim" onClick={onClose}/>
      <aside className="adm-drawer" role="dialog" aria-label={`Edit ${s.name}`}>
        <header className="adm-drawer-head">
          <div style={{ minWidth: 0 }}>
            <div className="row gap-2" style={{ marginBottom: 6 }}>
              <span className="adm-mono adm-drawer-id">LISTING · {s.id.toUpperCase()}</span>
              <VisibilityPill v={st.visibility}/>
            </div>
            <h2 className="adm-drawer-title">{name}</h2>
            <div className="adm-mono adm-drawer-sub">{s.building.toUpperCase()} · {s.type.toUpperCase()} · {s.area}M² · {s.seats} SEATS</div>
          </div>
          <button className="adm-close" onClick={onClose} aria-label="Close">✕</button>
        </header>

        <div className="adm-drawer-tabs">
          {[
            { id: 'control', label: 'Controls' },
            { id: 'edit', label: 'Edit' },
            { id: 'audit', label: 'Audit' },
          ].map(t => (
            <button key={t.id} className={`adm-dt ${tab === t.id ? 'active' : ''}`} onClick={() => setTab(t.id)}>{t.label}</button>
          ))}
        </div>

        <div className="adm-drawer-body">
          {tab === 'control' && (
            <>
              {/* Visibility — radio segmented */}
              <Block title="Visibility" sub="What renters see when they search.">
                <div className="adm-seg">
                  {[
                    { v: 'live', label: '● Live', desc: 'Visible to everyone' },
                    { v: 'hidden', label: '◌ Hidden', desc: 'Direct link only' },
                    { v: 'maintenance', label: '⚒ Maintenance', desc: 'Visible · bookings paused' },
                    { v: 'draft', label: '✎ Draft', desc: 'Admin-only' },
                  ].map(o => (
                    <button key={o.v}
                      className={`adm-seg-opt ${st.visibility === o.v ? 'active' : ''}`}
                      onClick={() => onChange({ visibility: o.v })}>
                      <div className="adm-seg-label">{o.label}</div>
                      <div className="adm-seg-desc">{o.desc}</div>
                    </button>
                  ))}
                </div>
              </Block>

              {/* Spotlight + Surprise — toggles */}
              <Block title="Discovery flags">
                <ToggleRow
                  label="★ Spotlight on home page"
                  sub="Pin to the top of search and feature in the carousel."
                  on={st.spotlight}
                  onChange={(on) => { onChange({ spotlight: on }); showToast(on ? `Spotlight on · ${s.name}` : 'Spotlight removed'); }}
                />
                <ToggleRow
                  label="◐ Surprise listing"
                  sub="Hidden from search; appears only as a random discovery suggestion."
                  on={st.surprise}
                  onChange={(on) => onChange({ surprise: on })}
                />
              </Block>

              {/* Quiet hours */}
              <Block title="Quiet hours" sub="No bookings accepted during this window. Useful for residential floors.">
                <ToggleRow
                  label={qh ? `Enabled · ${formatHour(qh.from)} → ${formatHour(qh.to)}` : 'Disabled'}
                  sub="Click to toggle, then drag the rail to set."
                  on={!!qh}
                  onChange={(on) => { const next = on ? { from: 22, to: 8 } : null; setQh(next); onChange({ quietHours: next }); }}
                />
                {qh && <QuietHoursRail qh={qh} onChange={(next) => { setQh(next); onChange({ quietHours: next }); }}/>}
              </Block>

              {/* Capacity override */}
              <Block title="Capacity override" sub={`Original capacity is ${s.seats}. Override to throttle bookings during events.`}>
                <div className="row gap-3" style={{ alignItems: 'center' }}>
                  <button className="adm-step" onClick={() => { const n = Math.max(1, capOverride - 1); setCapOverride(n); onChange({ capacityOverride: n }); }}>−</button>
                  <span className="h-display adm-cap-num">{capOverride}</span>
                  <button className="adm-step" onClick={() => { const n = Math.min(s.seats * 2, capOverride + 1); setCapOverride(n); onChange({ capacityOverride: n }); }}>+</button>
                  <span className="adm-mono" style={{ marginLeft: 8 }}>SEATS</span>
                  {capOverride !== s.seats && (
                    <button className="adm-link" onClick={() => { setCapOverride(s.seats); onChange({ capacityOverride: null }); }}>reset to {s.seats}</button>
                  )}
                </div>
              </Block>

              {/* Force actions */}
              <Block title="Force actions" sub="Use sparingly. Each one notifies affected renters.">
                <div className="adm-actions-grid">
                  <ActionTile
                    icon="⟲" label="End all current bookings"
                    desc="Refund + notify everyone currently booked"
                    tone="warn"
                    onClick={() => showToast(`Ended ${s.booked.length} active bookings · refunds queued`)}
                  />
                  <ActionTile
                    icon="⚑" label={st.flagged ? 'Clear flag' : 'Flag for review'}
                    desc={st.flagged ? `Currently flagged: ${st.flagged.reason}` : 'Hide from search until reviewed'}
                    tone={st.flagged ? 'ok' : 'warn'}
                    onClick={() => onChange({ flagged: st.flagged ? null : { by: 'kara@admin', reason: 'Manual review', count: 1 } })}
                  />
                  <ActionTile
                    icon="↗" label="Open as renter"
                    desc="See the listing the way users do"
                    onClick={() => showToast('Would open the renter-facing listing in a new tab')}
                  />
                  <ActionTile
                    icon="🗑" label="Archive listing"
                    desc="Soft-delete · recoverable for 30 days"
                    tone="danger"
                    onClick={() => showToast(`${s.name} archived (recoverable for 30 days)`)}
                  />
                </div>
              </Block>
            </>
          )}

          {tab === 'edit' && (
            <>
              <Block title="Display name">
                <input className="adm-input" value={name} onChange={(e) => { setName(e.target.value); setEdited(true); }}/>
              </Block>
              <Block title="One-line blurb" sub="Shown on cards and in search results.">
                <textarea className="adm-input" value={blurb} onChange={(e) => { setBlurb(e.target.value); setEdited(true); }} rows={2}/>
              </Block>
              <Block title="Pricing">
                <div className="row gap-3" style={{ alignItems: 'center' }}>
                  <div className="adm-money">
                    <span>$</span>
                    <input className="adm-input adm-input-money" type="number" min="0" value={price} onChange={(e) => { setPrice(Number(e.target.value)); setEdited(true); }}/>
                    <span className="adm-mono">/HR</span>
                  </div>
                  <button className="adm-link" onClick={() => { setPrice(0); setEdited(true); }}>make it free</button>
                </div>
              </Block>
              <Block title="Amenities" sub="Edit by adding or removing chips.">
                <AmenityEditor initial={s.amenities || []}/>
              </Block>
              <Block title="House rules" sub="Shown at booking confirmation.">
                <RulesEditor initial={s.rules || []}/>
              </Block>

              {edited && (
                <div className="adm-savebar">
                  <span className="adm-mono">UNSAVED CHANGES</span>
                  <div className="row gap-2">
                    <button className="adm-btn adm-btn-ghost" onClick={() => { setName(s.name); setBlurb(s.blurb); setPrice(s.price); setEdited(false); }}>Discard</button>
                    <button className="adm-btn adm-btn-primary" onClick={save}>Save changes</button>
                  </div>
                </div>
              )}
            </>
          )}

          {tab === 'audit' && <AuditLog space={s}/>}
        </div>
      </aside>
    </>
  );
}

function Block({ title, sub, children }) {
  return (
    <section className="adm-block">
      <div className="adm-block-head">
        <h4 className="adm-block-title">{title}</h4>
        {sub && <p className="adm-block-sub">{sub}</p>}
      </div>
      <div className="adm-block-body">{children}</div>
    </section>
  );
}

function ToggleRow({ label, sub, on, onChange }) {
  return (
    <div className="adm-toggle-row">
      <div style={{ minWidth: 0 }}>
        <div style={{ fontSize: 14, fontWeight: 600 }}>{label}</div>
        {sub && <div className="adm-block-sub" style={{ marginTop: 2 }}>{sub}</div>}
      </div>
      <button className={`adm-toggle ${on ? 'on' : ''}`} onClick={() => onChange(!on)} role="switch" aria-checked={on}>
        <span className="adm-toggle-knob"/>
      </button>
    </div>
  );
}

function ActionTile({ icon, label, desc, tone, onClick }) {
  return (
    <button className={`adm-actile ${tone ? `tone-${tone}` : ''}`} onClick={onClick}>
      <span className="adm-actile-icon">{icon}</span>
      <div>
        <div className="adm-actile-label">{label}</div>
        <div className="adm-actile-desc">{desc}</div>
      </div>
    </button>
  );
}

function QuietHoursRail({ qh, onChange }) {
  // 24 hour rail; user can click to set new from or drag
  const [drag, setDrag] = useStateAd(null);
  const railRef = useRefAd(null);

  const handle = (e, kind) => {
    const rect = railRef.current.getBoundingClientRect();
    const x = (e.clientX - rect.left) / rect.width;
    const h = Math.max(0, Math.min(23, Math.round(x * 24)));
    if (kind === 'from') onChange({ ...qh, from: h });
    else onChange({ ...qh, to: h });
  };

  // The "from" hour wraps to "to" — visualize as the gap
  const startPct = (qh.from / 24) * 100;
  const endPct = (qh.to / 24) * 100;
  const wraps = qh.from > qh.to;

  return (
    <div style={{ marginTop: 10 }}>
      <div className="adm-rail" ref={railRef}>
        {wraps ? (
          <>
            <div className="adm-rail-fill" style={{ left: 0, width: `${endPct}%` }}/>
            <div className="adm-rail-fill" style={{ left: `${startPct}%`, width: `${100 - startPct}%` }}/>
          </>
        ) : (
          <div className="adm-rail-fill" style={{ left: `${startPct}%`, width: `${endPct - startPct}%` }}/>
        )}
        {Array.from({ length: 25 }).map((_, h) => (
          <div key={h} className="adm-rail-tick" style={{ left: `${(h/24)*100}%` }}>
            {h % 6 === 0 && <span className="adm-mono">{h === 0 ? '12a' : h === 12 ? '12p' : h < 12 ? `${h}a` : `${h-12}p`}</span>}
          </div>
        ))}
        <button className="adm-rail-handle" style={{ left: `${startPct}%` }} onClick={() => {}} onPointerDown={(e) => setDrag('from')} onPointerMove={(e) => drag === 'from' && handle(e, 'from')} onPointerUp={() => setDrag(null)} aria-label="Start of quiet hours"/>
        <button className="adm-rail-handle" style={{ left: `${endPct}%` }} onClick={() => {}} onPointerDown={(e) => setDrag('to')} onPointerMove={(e) => drag === 'to' && handle(e, 'to')} onPointerUp={() => setDrag(null)} aria-label="End of quiet hours"/>
      </div>
      <div className="row gap-2" style={{ marginTop: 6 }}>
        <span className="adm-mono">FROM</span>
        <input className="adm-input adm-input-sm" type="number" min="0" max="23" value={qh.from} onChange={(e) => onChange({ ...qh, from: Number(e.target.value) })}/>
        <span className="adm-mono">TO</span>
        <input className="adm-input adm-input-sm" type="number" min="0" max="23" value={qh.to} onChange={(e) => onChange({ ...qh, to: Number(e.target.value) })}/>
      </div>
    </div>
  );
}

function AmenityEditor({ initial }) {
  const [items, setItems] = useStateAd(initial);
  const [draft, setDraft] = useStateAd('');
  return (
    <div>
      <div className="row" style={{ flexWrap: 'wrap', gap: 6 }}>
        {items.map((a, i) => (
          <span key={i} className="adm-chip">
            {a}
            <button className="adm-chip-x" onClick={() => setItems(items.filter((_, idx) => idx !== i))} aria-label={`Remove ${a}`}>×</button>
          </span>
        ))}
      </div>
      <div className="row gap-2" style={{ marginTop: 8 }}>
        <input className="adm-input" value={draft} onChange={(e) => setDraft(e.target.value)} placeholder="Add an amenity…" onKeyDown={(e) => { if (e.key === 'Enter' && draft.trim()) { setItems([...items, draft.trim()]); setDraft(''); } }}/>
        <button className="adm-btn adm-btn-ghost" onClick={() => { if (draft.trim()) { setItems([...items, draft.trim()]); setDraft(''); } }}>Add</button>
      </div>
    </div>
  );
}

function RulesEditor({ initial }) {
  const [items, setItems] = useStateAd(initial);
  return (
    <ol className="adm-rules">
      {items.map((r, i) => (
        <li key={i}>
          <span className="adm-rule-num">{String(i+1).padStart(2,'0')}</span>
          <input className="adm-input" value={r} onChange={(e) => { const next = items.slice(); next[i] = e.target.value; setItems(next); }}/>
          <button className="adm-rule-x" onClick={() => setItems(items.filter((_, idx) => idx !== i))} aria-label="Remove rule">×</button>
        </li>
      ))}
      <li>
        <button className="adm-btn adm-btn-ghost" onClick={() => setItems([...items, 'New rule'])}>+ Add rule</button>
      </li>
    </ol>
  );
}

function AuditLog({ space }) {
  return (
    <div className="adm-audit">
      <div className="adm-mono adm-audit-head">EVENT LOG · LAST 30 DAYS</div>
      <ul className="adm-audit-list">
        {space._state.auditLog.map((e, i) => (
          <li key={i} className="adm-audit-row">
            <div className="adm-audit-time">
              <span className="adm-mono">{e.at.toLocaleDateString('en', { month: 'short', day: 'numeric' }).toUpperCase()}</span>
              <span className="adm-mono adm-audit-hr">{e.at.toLocaleTimeString('en', { hour: 'numeric', minute: '2-digit' })}</span>
            </div>
            <div className={`adm-audit-bullet ${e.who === 'auto' ? 'auto' : 'human'}`}/>
            <div>
              <div style={{ fontSize: 14, fontWeight: 500 }}>{e.what}</div>
              <div className="adm-mono adm-audit-detail">{e.detail} · BY {e.who.toUpperCase()}</div>
            </div>
          </li>
        ))}
      </ul>
    </div>
  );
}

// ============================================================================
// USERS TAB
// ============================================================================
function UsersTab({ users, showToast }) {
  const [q, setQ] = useStateAd('');
  const [statusF, setStatusF] = useStateAd('all');
  const filtered = users.filter(u => {
    if (statusF !== 'all' && u.status !== statusF) return false;
    if (q && !(u.fullName + u.handle + u.email).toLowerCase().includes(q.toLowerCase())) return false;
    return true;
  });
  return (
    <>
      <div className="adm-toolbar">
        <div className="adm-search">
          <svg width="16" height="16" viewBox="0 0 16 16" fill="none" stroke="currentColor" strokeWidth="2"><circle cx="7" cy="7" r="5"/><path d="M11 11 L14 14"/></svg>
          <input value={q} onChange={(e) => setQ(e.target.value)} placeholder="Search by name, handle, or email…" aria-label="Search users"/>
        </div>
        <div className="adm-filters row gap-2">
          <FilterChip label="All" v="all" current={statusF} set={setStatusF}/>
          <FilterChip label="Active" v="active" current={statusF} set={setStatusF}/>
          <FilterChip label="Pending" v="pending" current={statusF} set={setStatusF}/>
          <FilterChip label="Inactive" v="inactive" current={statusF} set={setStatusF}/>
          <FilterChip label="Banned" v="banned" current={statusF} set={setStatusF}/>
        </div>
      </div>

      <div className="adm-table adm-table-users">
        <div className="adm-thead adm-thead-users">
          <div>User</div>
          <div>Role</div>
          <div>Status</div>
          <div className="adm-th-num">Bookings</div>
          <div className="adm-th-num">Hours</div>
          <div>Last active</div>
          <div>Joined</div>
          <div></div>
        </div>
        {filtered.map(u => <UserRow key={u.id} u={u} showToast={showToast}/>)}
      </div>
    </>
  );
}

function UserRow({ u, showToast }) {
  const last = relTime(u.lastActive);
  const joined = u.joined.toLocaleDateString('en', { month: 'short', year: '2-digit' });
  return (
    <div className="adm-row adm-row-user">
      <div className="adm-row-listing">
        <div className="adm-avatar" style={{ background: `linear-gradient(135deg, hsl(${u.avatarHue} 55% 55%), hsl(${(u.avatarHue+40)%360} 60% 45%))` }}>{u.firstName[0]}{u.fullName.split(' ')[1][0]}</div>
        <div style={{ minWidth: 0 }}>
          <div className="row gap-2" style={{ flexWrap: 'wrap' }}>
            <strong className="adm-row-name">{u.fullName}</strong>
            {u.verified && <span className="adm-tag tag-verified" title="Verified">✓</span>}
            {u.flags.length > 0 && <span className="adm-tag tag-flag" title={u.flags.join(', ')}>⚑ {u.flags[0]}</span>}
          </div>
          <div className="adm-mono adm-row-meta">@{u.handle.toUpperCase()} · {u.email.toUpperCase()} · {u.program.toUpperCase()}</div>
        </div>
      </div>
      <div><RolePill role={u.role}/></div>
      <div><StatusPill status={u.status}/></div>
      <div className="adm-th-num adm-numeric">{u.bookings}</div>
      <div className="adm-th-num adm-numeric">{u.hours}</div>
      <div className="adm-mono adm-numeric">{last.toUpperCase()}</div>
      <div className="adm-mono adm-numeric">{joined.toUpperCase()}</div>
      <div className="row gap-2" style={{ justifyContent: 'flex-end' }}>
        <button className="adm-mini-btn" onClick={() => showToast(`Sent message to ${u.firstName}`)}>Message</button>
        <button className="adm-mini-btn adm-mini-btn-danger" onClick={() => showToast(u.status === 'banned' ? `Unbanned ${u.firstName}` : `Suspended ${u.firstName}`)}>{u.status === 'banned' ? 'Unban' : 'Suspend'}</button>
      </div>
    </div>
  );
}

function RolePill({ role }) {
  const map = {
    admin: { cls: 'role-admin', label: 'ADMIN' },
    host: { cls: 'role-host', label: 'HOST' },
    student: { cls: 'role-student', label: 'STUDENT' },
  };
  const m = map[role] || map.student;
  return <span className={`role ${m.cls}`}>{m.label}</span>;
}

function StatusPill({ status }) {
  const map = {
    active: { cls: 'sp-active', label: '● Active' },
    pending: { cls: 'sp-pending', label: '◐ Pending' },
    inactive: { cls: 'sp-inactive', label: '○ Inactive' },
    banned: { cls: 'sp-banned', label: '⊘ Banned' },
  };
  const m = map[status] || map.active;
  return <span className={`sp ${m.cls}`}>{m.label}</span>;
}

function relTime(d) {
  const mins = Math.round((Date.now() - d.getTime()) / 60000);
  if (mins < 1) return 'now';
  if (mins < 60) return `${mins}m ago`;
  const hrs = Math.round(mins / 60);
  if (hrs < 24) return `${hrs}h ago`;
  const days = Math.round(hrs / 24);
  if (days < 30) return `${days}d ago`;
  return `${Math.round(days/30)}mo ago`;
}

// ============================================================================
// ACTIVITY TAB
// ============================================================================
function ActivityTab({ activity }) {
  const [scope, setScope] = useStateAd('all');
  const filtered = scope === 'all' ? activity : activity.filter(a => a.action.tone === scope);

  // Group by hour
  const groups = useMemoAd(() => {
    const m = {};
    filtered.forEach(a => {
      const k = a.at.toLocaleDateString('en', { weekday: 'short', month: 'short', day: 'numeric' }) + ' · ' + a.at.toLocaleTimeString('en', { hour: 'numeric' });
      (m[k] = m[k] || []).push(a);
    });
    return Object.entries(m);
  }, [filtered]);

  return (
    <>
      <div className="adm-toolbar">
        <div className="adm-filters row gap-2">
          <FilterChip label="All events" v="all" current={scope} set={setScope}/>
          <FilterChip label="Bookings" v="indigo" current={scope} set={setScope}/>
          <FilterChip label="Issues" v="orange" current={scope} set={setScope}/>
          <FilterChip label="Cancellations" v="red" current={scope} set={setScope}/>
          <FilterChip label="Check-ins" v="green" current={scope} set={setScope}/>
        </div>
        <div className="adm-live-indicator"><span className="adm-pulse-dot small"/><span className="adm-mono">LIVE FEED</span></div>
      </div>

      <div className="adm-feed">
        {groups.map(([k, items]) => (
          <div key={k} className="adm-feed-group">
            <div className="adm-feed-time">{k.toUpperCase()}</div>
            <ul className="adm-activity">
              {items.map(a => <ActivityRow key={a.id} a={a}/>)}
            </ul>
          </div>
        ))}
      </div>
    </>
  );
}

function ActivityRow({ a }) {
  return (
    <li className="adm-act-row">
      <div className="adm-act-time">
        <span className="adm-mono">{a.at.toLocaleTimeString('en', { hour: 'numeric', minute: '2-digit' })}</span>
      </div>
      <div className={`adm-act-dot tone-${a.action.tone}`}/>
      <div className="grow">
        <div style={{ fontSize: 14 }}>
          <strong>{a.user.firstName}</strong>
          <span className={`adm-act-verb tone-${a.action.tone}`}> {a.action.v} </span>
          <strong>{a.space.name}</strong>
        </div>
        <div className="adm-mono adm-act-meta">@{a.user.handle.toUpperCase()} · {a.space.building.toUpperCase()}</div>
      </div>
    </li>
  );
}

// ============================================================================
// Styles — kept in JSX so the admin file is self-contained
// ============================================================================
const adminCSS = `
.adm { background: var(--paper); min-height: calc(100vh - 65px); padding-bottom: 64px; }
.adm-container { max-width: 1440px; margin: 0 auto; padding: 0 32px; width: 100%; }
@media (max-width: 900px) { .adm-container { padding: 0 16px; } }

.adm-mono { font-family: 'JetBrains Mono', monospace; font-size: 10px; letter-spacing: 0.14em; color: var(--ink-mute); }
.adm-h3 { font-family: 'Lexend', sans-serif; font-weight: 600; font-size: 17px; margin: 0; letter-spacing: -0.01em; }

/* Header */
.adm-header { background: linear-gradient(180deg, #f4f1f7 0%, var(--paper) 100%); border-bottom: 1px solid var(--line-soft); padding: 36px 0 28px; }
.adm-header-row { display: flex; gap: 32px; align-items: flex-end; justify-content: space-between; flex-wrap: wrap; }
.adm-h1 { font-family: 'Lexend', sans-serif; font-weight: 700; font-size: 44px; letter-spacing: -0.025em; line-height: 1.02; margin: 4px 0; max-width: 18ch; }
.adm-h1-sub { font-size: 15px; color: var(--ink-soft); margin: 4px 0 0; }
.adm-shield { display: inline-flex; align-items: center; gap: 6px; padding: 4px 10px; background: var(--ink); color: #fff; border-radius: 999px; font-family: 'JetBrains Mono', monospace; font-size: 10px; letter-spacing: 0.18em; font-weight: 600; }
.adm-build { font-family: 'JetBrains Mono', monospace; font-size: 11px; color: var(--ink-mute); letter-spacing: 0.08em; }
.adm-pulse-card { display: flex; gap: 14px; align-items: center; padding: 14px 22px; background: #fff; border: 1px solid var(--line-soft); border-radius: 12px; box-shadow: var(--shadow-sm); }
.adm-pulse-dot { width: 12px; height: 12px; border-radius: 999px; background: #0f7a35; position: relative; flex-shrink: 0; }
.adm-pulse-dot::before { content: ''; position: absolute; inset: -4px; border-radius: 999px; background: #0f7a35; opacity: 0.25; animation: adPulse 1.8s ease-out infinite; }
.adm-pulse-dot.small { width: 8px; height: 8px; }
@keyframes adPulse { 0% { transform: scale(0.6); opacity: 0.45 } 100% { transform: scale(2.4); opacity: 0 } }
.adm-pulse-label { font-family: 'JetBrains Mono', monospace; font-size: 9px; letter-spacing: 0.2em; color: var(--ink-mute); }
.adm-pulse-value { font-family: 'Lexend', sans-serif; font-weight: 700; font-size: 18px; color: #0f7a35; margin: 2px 0; }
.adm-pulse-sub { font-size: 11px; color: var(--ink-mute); }

/* Tabs row */
.adm-tabs-row { border-bottom: 1px solid var(--line-soft); background: var(--paper); position: sticky; top: 65px; z-index: 40; }
.adm-tabs { display: flex; gap: 4px; }
.adm-tab { padding: 14px 18px; background: transparent; border: 0; cursor: pointer; display: flex; flex-direction: column; align-items: flex-start; gap: 2px; position: relative; color: var(--ink-soft); transition: color .12s; }
.adm-tab:hover { color: var(--ink); }
.adm-tab > span:first-child { font-size: 14px; font-weight: 600; }
.adm-tab-sub { font-family: 'JetBrains Mono', monospace; font-size: 9px; letter-spacing: 0.16em; color: var(--ink-mute); text-transform: uppercase; }
.adm-tab.active { color: var(--indigo); }
.adm-tab.active::after { content: ''; position: absolute; left: 0; right: 0; bottom: -1px; height: 2px; background: var(--indigo); }

.adm-body { padding: 24px 0 0; }

/* Stats strip */
.adm-stats { display: grid; grid-template-columns: repeat(4, 1fr); gap: 12px; }
.adm-metric { padding: 18px 20px; background: #fff; border: 1px solid var(--line-soft); border-radius: 14px; color: var(--ink); position: relative; overflow: hidden; }
.adm-metric.tone-indigo { background: linear-gradient(135deg, var(--indigo), #6c73c5); color: #fff; border-color: var(--indigo); }
.adm-metric.tone-orange { background: linear-gradient(135deg, var(--orange), #ff9d65); color: #fff; border-color: var(--orange); }
.adm-metric-label { font-family: 'JetBrains Mono', monospace; font-size: 10px; letter-spacing: 0.18em; opacity: 0.7; }
.adm-metric-value { font-size: 36px; line-height: 1; letter-spacing: -0.02em; }
.adm-metric-total { font-family: 'JetBrains Mono', monospace; font-size: 14px; opacity: 0.65; margin-left: 6px; }
.adm-metric-delta { font-family: 'JetBrains Mono', monospace; font-size: 10px; letter-spacing: 0.12em; margin-top: 10px; opacity: 0.75; text-transform: uppercase; }

/* Two-up grid */
.adm-grid-2 { display: grid; grid-template-columns: 1fr 1fr; gap: 16px; margin-top: 16px; }

/* Generic card */
.adm-card { background: #fff; border: 1px solid var(--line-soft); border-radius: 14px; padding: 20px 22px; }
.adm-card-head { display: flex; justify-content: space-between; align-items: center; margin-bottom: 14px; }
.adm-empty { color: var(--ink-mute); font-size: 13px; padding: 16px 0; }

/* Attention list */
.adm-attn { list-style: none; padding: 0; margin: 0; display: flex; flex-direction: column; gap: 2px; }
.adm-attn-row { display: flex; gap: 12px; padding: 10px 8px; border-radius: 8px; align-items: center; cursor: pointer; transition: background .12s; }
.adm-attn-row:hover { background: var(--paper-2); }
.adm-attn-mark { width: 10px; height: 10px; border-radius: 2px; flex-shrink: 0; }
.adm-attn-mark.flag { background: var(--orange); }
.adm-attn-mark.hide { background: var(--ink-mute); }
.adm-attn-mark.maint { background: var(--indigo); }
.adm-attn-sub { margin-top: 2px; }
.adm-chev { color: var(--ink-mute); font-size: 16px; }

/* Building bar */
.adm-bar { height: 6px; background: var(--paper-3); border-radius: 999px; overflow: hidden; }
.adm-bar-fill { height: 100%; background: linear-gradient(90deg, var(--indigo), var(--orange)); border-radius: 999px; transition: width .4s; }

/* Toolbar */
.adm-toolbar { display: flex; gap: 12px; align-items: center; flex-wrap: wrap; margin-bottom: 14px; }
.adm-search { flex: 0 1 320px; display: flex; align-items: center; gap: 10px; background: #fff; border: 1px solid var(--line-soft); border-radius: 10px; padding: 8px 14px; color: var(--ink-mute); transition: border-color .12s, box-shadow .12s; }
.adm-search:focus-within { border-color: var(--indigo); box-shadow: 0 0 0 3px rgba(75,82,167,0.12); color: var(--ink); }
.adm-search input { border: 0; outline: 0; background: transparent; width: 100%; font-size: 14px; color: var(--ink); }
.adm-filters { flex-wrap: wrap; }
.adm-fchip { padding: 6px 12px; border-radius: 999px; background: #fff; border: 1px solid var(--line-soft); font-size: 12px; font-weight: 600; cursor: pointer; color: var(--ink-soft); transition: all .12s; }
.adm-fchip:hover { border-color: var(--indigo); color: var(--indigo); }
.adm-fchip.active { background: var(--ink); border-color: var(--ink); color: #fff; }
.adm-select { padding: 8px 12px; border-radius: 10px; border: 1px solid var(--line-soft); background: #fff; font-size: 13px; font-weight: 500; cursor: pointer; margin-left: auto; }
.adm-live-indicator { margin-left: auto; display: flex; gap: 8px; align-items: center; padding: 6px 12px; background: #fff; border: 1px solid var(--line-soft); border-radius: 999px; }

/* Table */
.adm-table { background: #fff; border: 1px solid var(--line-soft); border-radius: 14px; overflow: hidden; }
.adm-thead, .adm-row { display: grid; grid-template-columns: minmax(0,2.4fr) 130px 110px 80px 100px 110px 40px; gap: 12px; align-items: center; }
.adm-thead-users, .adm-row-user { grid-template-columns: minmax(0,2fr) 90px 110px 90px 70px 110px 90px minmax(160px, 1fr); }
.adm-thead { padding: 12px 18px; background: var(--paper-2); border-bottom: 1px solid var(--line-soft); font-family: 'JetBrains Mono', monospace; font-size: 10px; letter-spacing: 0.16em; color: var(--ink-mute); text-transform: uppercase; }
.adm-th-num { text-align: right; }
.adm-numeric { font-family: 'JetBrains Mono', monospace; font-size: 12px; color: var(--ink-soft); }
.adm-row { padding: 14px 18px; border-bottom: 1px solid var(--line-soft); cursor: pointer; transition: background .12s; }
.adm-row:last-child { border-bottom: 0; }
.adm-row:hover { background: var(--paper-2); }
.adm-row-listing { display: flex; gap: 12px; align-items: center; min-width: 0; }
.adm-row-mini { width: 56px; height: 36px; flex-shrink: 0; background: var(--paper-2); border-radius: 6px; overflow: hidden; display: grid; place-items: center; }
.adm-row-name { font-size: 14px; }
.adm-row-meta { margin-top: 3px; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.adm-row-occ { display: flex; align-items: center; gap: 8px; justify-content: flex-end; }
.adm-row-occ-bar { width: 56px; height: 5px; border-radius: 999px; background: var(--paper-3); overflow: hidden; }
.adm-row-occ-bar > div { height: 100%; background: var(--indigo); }
.adm-row-chev { color: var(--ink-mute); font-size: 16px; text-align: right; }

/* Tags */
.adm-tag { padding: 2px 8px; border-radius: 999px; font-size: 10px; font-weight: 700; letter-spacing: 0.04em; }
.tag-spotlight { background: var(--orange-pale); color: var(--orange-deep); }
.tag-flag { background: #ffe5d5; color: var(--orange-deep); }
.tag-surprise { background: var(--indigo-pale); color: var(--indigo-deep); }
.tag-quiet { background: var(--paper-3); color: var(--ink-soft); }
.tag-verified { background: var(--green-pale); color: var(--green); padding: 2px 6px; }

/* Visibility pill */
.vp { display: inline-flex; align-items: center; padding: 3px 10px; border-radius: 999px; font-family: 'JetBrains Mono', monospace; font-size: 10px; letter-spacing: 0.1em; font-weight: 600; text-transform: uppercase; }
.vp-live { background: var(--green-pale); color: var(--green); }
.vp-hidden { background: var(--paper-3); color: var(--ink-soft); }
.vp-maint { background: var(--orange-pale); color: var(--orange-deep); }
.vp-draft { background: var(--indigo-pale); color: var(--indigo-deep); }

/* Status/role pills */
.role { font-family: 'JetBrains Mono', monospace; font-size: 10px; letter-spacing: 0.14em; font-weight: 700; padding: 3px 8px; border-radius: 4px; }
.role-admin { background: var(--ink); color: #fff; }
.role-host { background: var(--indigo-pale); color: var(--indigo-deep); }
.role-student { background: var(--paper-3); color: var(--ink-soft); }
.sp { font-family: 'JetBrains Mono', monospace; font-size: 10px; letter-spacing: 0.1em; font-weight: 600; padding: 3px 8px; border-radius: 999px; }
.sp-active { background: var(--green-pale); color: var(--green); }
.sp-pending { background: var(--orange-pale); color: var(--orange-deep); }
.sp-inactive { background: var(--paper-3); color: var(--ink-soft); }
.sp-banned { background: #1b1b1f; color: #fff; }

/* User avatars */
.adm-avatar { width: 38px; height: 38px; border-radius: 999px; display: grid; place-items: center; color: #fff; font-weight: 700; font-size: 13px; flex-shrink: 0; text-shadow: 0 1px 2px rgba(0,0,0,0.2); }

/* Mini buttons */
.adm-mini-btn { padding: 5px 10px; border-radius: 6px; background: #fff; border: 1px solid var(--line-soft); font-size: 12px; font-weight: 600; cursor: pointer; color: var(--ink); }
.adm-mini-btn:hover { border-color: var(--indigo); color: var(--indigo); }
.adm-mini-btn-danger { color: var(--orange-deep); border-color: rgba(159,66,0,0.2); }
.adm-mini-btn-danger:hover { background: rgba(159,66,0,0.06); border-color: var(--orange-deep); color: var(--orange-deep); }

/* Drawer */
.adm-scrim { position: fixed; inset: 0; background: rgba(27,27,31,0.45); z-index: 90; animation: adFade .2s ease; }
@keyframes adFade { from { opacity: 0 } to { opacity: 1 } }
.adm-drawer { position: fixed; top: 0; right: 0; bottom: 0; width: min(620px, 100vw); background: var(--paper); border-left: 1px solid var(--line-soft); box-shadow: -20px 0 60px rgba(27,27,31,0.18); z-index: 100; display: flex; flex-direction: column; animation: adSlideIn .25s ease; }
@keyframes adSlideIn { from { transform: translateX(20px); opacity: 0 } to { transform: translateX(0); opacity: 1 } }
.adm-drawer-head { padding: 22px 26px 16px; border-bottom: 1px solid var(--line-soft); display: flex; gap: 16px; justify-content: space-between; background: #fff; }
.adm-drawer-id { color: var(--ink-mute); }
.adm-drawer-title { font-family: 'Lexend', sans-serif; font-weight: 700; font-size: 26px; letter-spacing: -0.02em; margin: 0; line-height: 1.1; }
.adm-drawer-sub { margin-top: 6px; }
.adm-close { font-size: 18px; color: var(--ink-mute); width: 32px; height: 32px; border-radius: 999px; display: grid; place-items: center; background: var(--paper-2); cursor: pointer; }
.adm-close:hover { background: var(--paper-3); color: var(--ink); }
.adm-drawer-tabs { display: flex; gap: 4px; padding: 10px 26px 0; background: #fff; border-bottom: 1px solid var(--line-soft); }
.adm-dt { padding: 10px 14px; background: transparent; border: 0; border-bottom: 2px solid transparent; font-size: 13px; font-weight: 600; color: var(--ink-soft); cursor: pointer; }
.adm-dt:hover { color: var(--ink); }
.adm-dt.active { color: var(--indigo); border-bottom-color: var(--indigo); }
.adm-drawer-body { padding: 18px 26px 32px; overflow-y: auto; flex: 1; }

/* Blocks in drawer */
.adm-block { padding: 16px 0; border-bottom: 1px solid var(--line-soft); }
.adm-block:last-child { border-bottom: 0; }
.adm-block-head { margin-bottom: 12px; }
.adm-block-title { font-family: 'Lexend', sans-serif; font-weight: 600; font-size: 15px; margin: 0; letter-spacing: -0.01em; }
.adm-block-sub { font-size: 12px; color: var(--ink-mute); margin: 4px 0 0; line-height: 1.5; }

/* Segmented control */
.adm-seg { display: grid; grid-template-columns: 1fr 1fr; gap: 8px; }
.adm-seg-opt { padding: 12px 14px; border-radius: 10px; background: #fff; border: 1.5px solid var(--line-soft); cursor: pointer; text-align: left; transition: all .12s; }
.adm-seg-opt:hover { border-color: var(--ink-mute); }
.adm-seg-opt.active { border-color: var(--indigo); background: rgba(75,82,167,0.06); box-shadow: 0 0 0 3px rgba(75,82,167,0.1); }
.adm-seg-label { font-size: 13px; font-weight: 600; }
.adm-seg-desc { font-family: 'JetBrains Mono', monospace; font-size: 10px; color: var(--ink-mute); letter-spacing: 0.08em; margin-top: 4px; text-transform: uppercase; }

/* Toggle row */
.adm-toggle-row { display: flex; justify-content: space-between; align-items: center; padding: 12px 0; gap: 16px; }
.adm-toggle-row + .adm-toggle-row { border-top: 1px solid var(--line-soft); }
.adm-toggle { width: 40px; height: 22px; border-radius: 999px; background: var(--paper-3); position: relative; cursor: pointer; transition: background .2s; border: 0; flex-shrink: 0; }
.adm-toggle.on { background: var(--indigo); }
.adm-toggle-knob { position: absolute; top: 2px; left: 2px; width: 18px; height: 18px; border-radius: 999px; background: #fff; box-shadow: 0 1px 3px rgba(0,0,0,0.2); transition: left .2s; }
.adm-toggle.on .adm-toggle-knob { left: 20px; }

/* Action tile */
.adm-actions-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 8px; }
.adm-actile { padding: 14px; border-radius: 10px; background: #fff; border: 1px solid var(--line-soft); cursor: pointer; text-align: left; display: flex; gap: 12px; align-items: flex-start; transition: all .12s; }
.adm-actile:hover { border-color: var(--ink-mute); transform: translateY(-1px); box-shadow: var(--shadow-sm); }
.adm-actile.tone-warn { background: rgba(255,130,60,0.04); border-color: rgba(255,130,60,0.2); }
.adm-actile.tone-warn:hover { border-color: var(--orange); }
.adm-actile.tone-danger:hover { background: rgba(159,66,0,0.04); border-color: var(--orange-deep); }
.adm-actile.tone-ok { background: rgba(15,122,53,0.04); border-color: rgba(15,122,53,0.2); }
.adm-actile-icon { font-size: 18px; line-height: 1; }
.adm-actile-label { font-size: 13px; font-weight: 600; }
.adm-actile-desc { font-size: 11px; color: var(--ink-mute); margin-top: 2px; line-height: 1.4; }

/* Capacity stepper */
.adm-step { width: 30px; height: 30px; border-radius: 8px; background: #fff; border: 1px solid var(--line-soft); font-size: 18px; cursor: pointer; }
.adm-step:hover { border-color: var(--indigo); color: var(--indigo); }
.adm-cap-num { font-size: 32px; min-width: 50px; text-align: center; }
.adm-link { font-size: 12px; color: var(--indigo); background: transparent; border: 0; cursor: pointer; padding: 4px 0; font-weight: 600; }
.adm-link:hover { text-decoration: underline; }

/* Quiet hours rail */
.adm-rail { position: relative; height: 34px; background: var(--paper-2); border: 1px solid var(--line-soft); border-radius: 8px; user-select: none; touch-action: none; }
.adm-rail-fill { position: absolute; top: 0; bottom: 0; background: rgba(75,82,167,0.18); }
.adm-rail-tick { position: absolute; top: 6px; bottom: 6px; width: 1px; background: var(--line-soft); }
.adm-rail-tick span { position: absolute; bottom: -16px; left: -10px; font-size: 9px; color: var(--ink-mute); }
.adm-rail-handle { position: absolute; top: 50%; transform: translate(-50%, -50%); width: 14px; height: 28px; border-radius: 4px; background: var(--indigo); cursor: grab; border: 0; box-shadow: 0 1px 3px rgba(0,0,0,0.2); }
.adm-rail-handle:active { cursor: grabbing; }

/* Inputs */
.adm-input { padding: 10px 12px; border: 1px solid var(--line-soft); border-radius: 8px; background: #fff; font-family: 'Work Sans', sans-serif; font-size: 14px; outline: 0; transition: border-color .12s, box-shadow .12s; width: 100%; resize: vertical; }
.adm-input:focus { border-color: var(--indigo); box-shadow: 0 0 0 3px rgba(75,82,167,0.15); }
.adm-input-sm { width: 56px; padding: 6px 8px; font-size: 13px; }
.adm-input-money { width: 80px; }
.adm-money { display: inline-flex; align-items: center; gap: 6px; padding: 0 12px; border: 1px solid var(--line-soft); border-radius: 8px; background: #fff; }
.adm-money:focus-within { border-color: var(--indigo); box-shadow: 0 0 0 3px rgba(75,82,167,0.15); }
.adm-money .adm-input-money { border: 0; padding: 10px 0; }

/* Chips (amenities) */
.adm-chip { display: inline-flex; align-items: center; gap: 6px; padding: 5px 12px; background: var(--paper-2); border-radius: 999px; font-size: 12px; font-weight: 500; }
.adm-chip-x { background: transparent; border: 0; color: var(--ink-mute); cursor: pointer; font-size: 14px; line-height: 1; padding: 0; }
.adm-chip-x:hover { color: var(--orange-deep); }

/* Rules list */
.adm-rules { list-style: none; padding: 0; margin: 0; display: flex; flex-direction: column; gap: 8px; }
.adm-rules li { display: flex; gap: 10px; align-items: center; }
.adm-rule-num { font-family: 'JetBrains Mono', monospace; font-size: 11px; color: var(--ink-mute); letter-spacing: 0.1em; min-width: 24px; }
.adm-rule-x { background: transparent; border: 0; color: var(--ink-mute); cursor: pointer; font-size: 16px; padding: 0 6px; }
.adm-rule-x:hover { color: var(--orange-deep); }

/* Save bar */
.adm-savebar { position: sticky; bottom: -32px; margin: 20px -26px -32px; padding: 14px 26px; background: var(--ink); color: #fff; display: flex; justify-content: space-between; align-items: center; }
.adm-savebar .adm-mono { color: rgba(255,255,255,0.7); }
.adm-btn { padding: 8px 16px; border-radius: 8px; font-size: 13px; font-weight: 600; border: 0; cursor: pointer; }
.adm-btn-primary { background: var(--indigo); color: #fff; }
.adm-btn-primary:hover { background: var(--indigo-deep); }
.adm-btn-ghost { background: rgba(255,255,255,0.1); color: #fff; border: 1px solid rgba(255,255,255,0.2); }
.adm-btn-ghost:hover { background: rgba(255,255,255,0.18); }

/* Audit log */
.adm-audit-head { padding: 6px 0 14px; border-bottom: 1px solid var(--line-soft); }
.adm-audit-list { list-style: none; padding: 0; margin: 0; }
.adm-audit-row { display: grid; grid-template-columns: 60px 16px 1fr; gap: 12px; align-items: flex-start; padding: 14px 0; border-bottom: 1px solid var(--line-soft); }
.adm-audit-row:last-child { border-bottom: 0; }
.adm-audit-time { display: flex; flex-direction: column; gap: 2px; }
.adm-audit-hr { color: var(--ink-soft); }
.adm-audit-bullet { width: 10px; height: 10px; border-radius: 999px; margin-top: 5px; }
.adm-audit-bullet.auto { background: var(--paper-3); border: 2px solid var(--ink-mute); }
.adm-audit-bullet.human { background: var(--indigo); border: 2px solid var(--indigo); }
.adm-audit-detail { margin-top: 2px; }

/* Activity feed */
.adm-activity { list-style: none; padding: 0; margin: 0; }
.adm-act-row { display: grid; grid-template-columns: 56px 12px 1fr; gap: 12px; align-items: flex-start; padding: 10px 0; border-bottom: 1px solid var(--line-soft); }
.adm-act-row:last-child { border-bottom: 0; }
.adm-act-time { color: var(--ink-mute); }
.adm-act-dot { width: 10px; height: 10px; border-radius: 999px; margin-top: 4px; }
.adm-act-dot.tone-indigo { background: var(--indigo); }
.adm-act-dot.tone-orange { background: var(--orange); }
.adm-act-dot.tone-red { background: var(--orange-deep); }
.adm-act-dot.tone-green { background: var(--green); }
.adm-act-verb { color: var(--ink-soft); }
.adm-act-verb.tone-indigo { color: var(--indigo); }
.adm-act-verb.tone-orange { color: var(--orange-deep); }
.adm-act-verb.tone-red { color: var(--orange-deep); }
.adm-act-verb.tone-green { color: var(--green); }
.adm-act-meta { margin-top: 2px; }
.adm-feed-group { margin-bottom: 24px; background: #fff; border: 1px solid var(--line-soft); border-radius: 14px; padding: 16px 22px; }
.adm-feed-time { font-family: 'JetBrains Mono', monospace; font-size: 10px; letter-spacing: 0.16em; color: var(--ink-mute); margin-bottom: 6px; }

/* Toast */
.adm-toast { position: fixed; bottom: 24px; left: 50%; transform: translateX(-50%); background: var(--ink); color: #fff; padding: 12px 20px; border-radius: 10px; font-size: 14px; font-weight: 500; box-shadow: var(--shadow-lg); z-index: 200; animation: adToastIn .25s ease; }
@keyframes adToastIn { from { opacity: 0; transform: translate(-50%, 8px) } to { opacity: 1; transform: translate(-50%, 0) } }

/* Responsive */
@media (max-width: 1100px) {
  .adm-stats { grid-template-columns: 1fr 1fr; }
  .adm-grid-2 { grid-template-columns: 1fr; }
  .adm-thead, .adm-row { grid-template-columns: minmax(0,2fr) 110px 90px 60px 80px 90px 30px; }
  .adm-thead-users, .adm-row-user { grid-template-columns: minmax(0,2fr) 80px 100px 70px 60px 100px 80px 140px; }
}
@media (max-width: 720px) {
  .adm-h1 { font-size: 32px; }
  .adm-stats { grid-template-columns: 1fr 1fr; }
  .adm-thead { display: none; }
  .adm-row { grid-template-columns: 1fr; gap: 8px; padding: 14px; }
  .adm-th-num, .adm-numeric { text-align: left; }
  .adm-seg, .adm-actions-grid { grid-template-columns: 1fr; }
}
`;

Object.assign(window, { AdminPage });
