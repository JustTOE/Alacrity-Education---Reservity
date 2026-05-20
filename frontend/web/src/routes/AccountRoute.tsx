import { useEffect, useMemo, useState } from "react";
import { useNavigate } from "react-router-dom";
import { formatHour } from "@/data/booking";
import { useApp } from "@/state/AppStore";
import type { SavedPass, Space } from "@/types";
import FloorPlanSketch from "@/components/viz/FloorPlanSketch";
import { useNow } from "@/hooks/useNow";
import { useSpaces } from "@/hooks/useSpaces";
import { useReservations } from "@/hooks/useReservations";
import { useAuth } from "@/auth/AuthContext";

type Section = "bookings" | "profile" | "history" | "stats" | "auth" | "appearance";

interface Profile {
  displayName: string;
  realName: string;
  handle: string;
  initials: string;
  email: string;
  bio: string;
  memberSince: string;
  coverGradient: string;
  socials: { kind: string; glyph: string; handle: string }[];
  vibes: { label: string; icon: string; percent: number }[];
}

interface Upcoming {
  space: Space;
  startsAt: Date;
  startH: number;
  endH: number;
  duration: number;
  tag: string;
  recurring: boolean;
  dayLabel: string;
}

interface HistoryItem {
  space: Space;
  startsAt: Date;
  startH: number;
  endH: number;
  duration: number;
  tag: string;
}

export default function AccountRoute() {
  useApp();
  const navigate = useNavigate();
  const auth = useAuth();
  const { now } = useNow();
  const [section, setSection] = useState<Section>("bookings");

  const { data: spacesData } = useSpaces({ size: 50 });
  const spaceLookup = useMemo<Map<string, Space>>(() => {
    const m = new Map<string, Space>();
    for (const s of spacesData?.spaces ?? []) m.set(s.id, s);
    return m;
  }, [spacesData]);

  const upcoming = useMemo(() => buildUpcoming(now, spaceLookup), [now, spaceLookup]);
  const fullHistory = useMemo(() => buildHistory(now, spaceLookup), [now, spaceLookup]);

  const apiUser = auth.user;
  const profile = useMemo<Profile>(() => {
    const fallback = buildProfile();
    if (!apiUser) return fallback;
    return {
      ...fallback,
      displayName: apiUser.displayName,
      realName: apiUser.realName ?? apiUser.displayName,
      handle: apiUser.handle,
      initials: apiUser.initials,
      email: apiUser.email,
      bio: apiUser.bio ?? fallback.bio,
      coverGradient: apiUser.coverGradient ?? fallback.coverGradient,
      memberSince: formatMemberSince(apiUser.memberSince) ?? fallback.memberSince,
    };
  }, [apiUser]);

  const { data: reservations } = useReservations({ upcoming: false });
  const savedPasses = useMemo<SavedPass[]>(() => {
    const list = reservations?.content ?? [];
    return list.map((r) => {
      const start = new Date(r.startsAt);
      const end = new Date(r.endsAt);
      const space = spaceLookup.get(r.space.id);
      return {
        id: r.id,
        space: space ?? minimalSpaceFromSummary(r.space),
        startH: start.getHours(),
        endH: end.getHours(),
        date: new Date(start.getFullYear(), start.getMonth(), start.getDate()),
        recurring: r.recurring === "weekly" ? "weekly" : "none",
        confirmedAt: new Date(r.createdAt),
        reservationCode: r.id,
        reservationId: r.reservationId ?? undefined,
        passToken: r.passToken ?? undefined,
        status: r.status,
      } satisfies SavedPass;
    });
  }, [reservations, spaceLookup]);

  async function handleLogout() {
    await auth.logout();
    navigate("/", { replace: true });
  }

  return (
    <div className="acct">
      <aside className="acct-side">
        <div className="acct-side-head">
          <div className="acct-avatar">
            <div className="acct-avatar-bg" style={{ background: profile.coverGradient }} />
            <span>{profile.initials}</span>
          </div>
          <div style={{ minWidth: 0 }}>
            <div className="h-display" style={{ fontSize: 18, lineHeight: 1.1, marginTop: 2 }}>
              {profile.displayName}
            </div>
            <div
              className="h-mono"
              style={{ fontSize: 10, color: "var(--ink-mute)", letterSpacing: "0.14em", marginTop: 4 }}
            >
              @{profile.handle}
            </div>
          </div>
        </div>

        <nav className="acct-nav" aria-label="Account sections">
          <SideItem id="bookings" current={section} setSection={setSection} label="Bookings" sub="What's next" icon={IconCalendar} />
          <SideItem id="profile" current={section} setSection={setSection} label="Profile" sub="How others see you" icon={IconUser} />
          <SideItem id="history" current={section} setSection={setSection} label="History" sub="Where you've been" icon={IconArchive} />
          <SideItem id="stats" current={section} setSection={setSection} label="Stats" sub="The numbers" icon={IconBars} />
          <div className="acct-nav-sep">SETTINGS</div>
          <SideItem id="auth" current={section} setSection={setSection} label="Authentication" sub="Email, password, 2FA" icon={IconKey} />
          <SideItem id="appearance" current={section} setSection={setSection} label="Appearance" sub="Theme, accent, density" icon={IconSparkle} />
        </nav>

        <button className="acct-logout" onClick={handleLogout}>
          <IconLogout />
          <span>Log out</span>
        </button>
      </aside>

      <main className="acct-main">
        {section === "bookings" && (
          <BookingsSection
            upcoming={upcoming}
            savedPasses={savedPasses}
            profile={profile}
            onOpenPass={(p) => p.reservationId && navigate(`/pass/${p.reservationId}`)}
          />
        )}
        {section === "profile" && <ProfileSection profile={profile} fullHistory={fullHistory} />}
        {section === "history" && <HistorySection fullHistory={fullHistory} />}
        {section === "stats" && <StatsSection fullHistory={fullHistory} />}
        {section === "auth" && <AuthSection profile={profile} />}
        {section === "appearance" && <AppearanceSection />}
      </main>

      <style>{`
        .acct { display: grid; grid-template-columns: 280px minmax(0, 1fr); gap: 0; min-height: calc(100vh - 65px); background: var(--paper); }
        .acct-side { border-right: 1px solid var(--line-soft); padding: 28px 16px 20px; display: flex; flex-direction: column; gap: 12px; position: sticky; top: 65px; height: calc(100vh - 65px); }
        .acct-side-head { display: flex; gap: 12px; align-items: center; padding: 4px 8px 14px; border-bottom: 1px solid var(--line-soft); }
        .acct-avatar { width: 48px; height: 48px; border-radius: 999px; position: relative; overflow: hidden; flex-shrink: 0; display: grid; place-items: center; color: #fff; font-weight: 700; font-size: 18px; }
        .acct-avatar-bg { position: absolute; inset: 0; }
        .acct-avatar span { position: relative; z-index: 1; text-shadow: 0 1px 2px rgba(0,0,0,0.2); }
        .acct-nav { display: flex; flex-direction: column; gap: 2px; flex: 1; overflow-y: auto; padding: 4px 0; }
        .acct-nav-sep { font-family: 'JetBrains Mono', monospace; font-size: 10px; letter-spacing: 0.18em; color: var(--ink-mute); padding: 18px 12px 8px; }
        .side-item { display: flex; gap: 12px; padding: 10px 12px; border-radius: 10px; cursor: pointer; align-items: center; border: 0; background: transparent; color: var(--ink-soft); text-align: left; transition: background .12s, color .12s; }
        .side-item:hover { background: var(--paper-2); color: var(--ink); }
        .side-item.active { background: var(--indigo); color: #fff; }
        .side-item.active .si-sub { color: rgba(255,255,255,0.78); }
        .si-icon { width: 20px; height: 20px; flex-shrink: 0; opacity: 0.85; }
        .side-item.active .si-icon { opacity: 1; }
        .si-label { font-size: 14px; font-weight: 600; line-height: 1.1; }
        .si-sub { font-size: 11px; color: var(--ink-mute); margin-top: 2px; line-height: 1.2; }
        .acct-logout { display: flex; align-items: center; gap: 10px; padding: 10px 12px; border-radius: 10px; color: var(--ink-soft); background: transparent; border: 0; cursor: pointer; font-size: 14px; font-weight: 600; }
        .acct-logout:hover { background: var(--paper-2); color: var(--orange-deep); }
        .acct-main { padding: 32px 40px 64px; max-width: 1100px; }
        .acct-h1 { font-family: 'Lexend', sans-serif; font-weight: 700; font-size: 44px; letter-spacing: -0.02em; margin: 0 0 4px; }
        .acct-sub { color: var(--ink-soft); font-size: 16px; margin: 0 0 28px; }
        @media (max-width: 900px) {
          .acct { grid-template-columns: 1fr; }
          .acct-side { position: static; height: auto; flex-direction: row; flex-wrap: wrap; padding: 16px; }
          .acct-side-head { width: 100%; padding-bottom: 12px; }
          .acct-nav { flex-direction: row; overflow-x: auto; flex-wrap: nowrap; }
          .acct-nav-sep { display: none; }
          .acct-main { padding: 24px 20px 48px; }
        }
      `}</style>
    </div>
  );
}

interface SideItemProps {
  id: Section;
  current: Section;
  setSection: (s: Section) => void;
  label: string;
  sub: string;
  icon: () => JSX.Element;
}

function SideItem({ id, current, setSection, label, sub, icon: Icon }: SideItemProps) {
  return (
    <button className={`side-item ${current === id ? "active" : ""}`} onClick={() => setSection(id)}>
      <Icon />
      <div style={{ minWidth: 0 }}>
        <div className="si-label">{label}</div>
        <div className="si-sub">{sub}</div>
      </div>
    </button>
  );
}

// ============================================================================
// BOOKINGS
// ============================================================================
interface BookingsSectionProps {
  upcoming: Upcoming[];
  savedPasses: SavedPass[];
  profile: Profile;
  onOpenPass: (pass: SavedPass) => void;
}

function BookingsSection({ upcoming, savedPasses, profile, onOpenPass }: BookingsSectionProps) {
  const next = upcoming[0];
  const minutesUntil = next ? Math.round((next.startsAt.getTime() - Date.now()) / 60000) : 9999;
  const checkingIn = minutesUntil > 0 && minutesUntil < 60;
  // Bind a no-arg handler that opens the most-recently saved pass (if any).
  const openLatest = () => {
    const latest = savedPasses[0];
    if (latest) onOpenPass(latest);
  };

  return (
    <>
      <header style={{ marginBottom: 24 }}>
        <span className="kicker">Hello, {profile.displayName.split(" ")[0]}</span>
        <h1 className="acct-h1">Upcoming</h1>
        <p className="acct-sub">
          {upcoming.length} {upcoming.length === 1 ? "booking" : "bookings"} on the calendar.
        </p>
      </header>

      {checkingIn && next && (
        <div className="checkin-callout">
          <div className="checkin-pulse" />
          <div className="grow">
            <div
              className="h-mono"
              style={{ fontSize: 11, letterSpacing: "0.18em", color: "var(--orange-deep)" }}
            >
              STARTS IN {minutesUntil} MIN
            </div>
            <div className="h-display" style={{ fontSize: 26, margin: "4px 0 0" }}>
              {next.space.name}
            </div>
            <div style={{ fontSize: 13, color: "var(--ink-soft)" }}>
              {next.space.building} · Floor {next.space.floor < 0 ? "B1" : next.space.floor}
            </div>
          </div>
          <button className="btn btn-primary" onClick={openLatest}>
            Open pass →
          </button>
        </div>
      )}

      <div className="bk-stats">
        <Stat n={String(upcoming.length)} unit="upcoming" sub="this week" tone="indigo" />
        <Stat n={`${upcoming.reduce((a, b) => a + b.duration, 0)}h`} unit="reserved" sub="total time" />
        <Stat
          n={String(new Set(upcoming.map((u) => u.space.id)).size)}
          unit="spaces"
          sub="across bookings"
        />
      </div>

      <div className="col gap-3" style={{ marginTop: 28 }}>
        {upcoming.map((u, i) => (
          <UpcomingCard key={i} u={u} onOpenPass={openLatest} />
        ))}
      </div>

      <h2 className="h-headline" style={{ fontSize: 22, margin: "40px 0 12px" }}>
        Recent passes
      </h2>
      <div className="passes-row">
        {savedPasses.slice(0, 3).map((p, i) => (
          <article key={i} className="mini-pass grain">
            <div className="row" style={{ justifyContent: "space-between", alignItems: "flex-start" }}>
              <div>
                <div
                  className="h-mono"
                  style={{ fontSize: 9, letterSpacing: "0.2em", opacity: 0.85 }}
                >
                  PASS
                </div>
                <div
                  className="h-display"
                  style={{ fontSize: 18, margin: "4px 0 0", lineHeight: 1.05 }}
                >
                  {p.space.name}
                </div>
                <div style={{ fontSize: 12, opacity: 0.92, marginTop: 2 }}>
                  {formatHour(p.startH)} — {formatHour(p.endH)}
                </div>
              </div>
              <FloorPlanSketch space={p.space} width={64} height={40} />
            </div>
            <div
              className="h-mono"
              style={{ fontSize: 9, letterSpacing: "0.18em", opacity: 0.7, marginTop: 12 }}
            >
              SAVED
            </div>
          </article>
        ))}
        {savedPasses.length === 0 && (
          <div
            className="card"
            style={{ padding: 24, gridColumn: "1 / -1", textAlign: "center", color: "var(--ink-mute)" }}
          >
            No saved passes yet — every booking pins one here automatically.
          </div>
        )}
      </div>

      <style>{`
        .checkin-callout {
          display: flex; gap: 16px; align-items: center;
          padding: 18px 22px; border-radius: 14px; margin-bottom: 24px;
          background: linear-gradient(135deg, rgba(255,130,60,0.12), rgba(255,130,60,0.04));
          border: 1px solid rgba(255,130,60,0.3);
          position: relative; overflow: hidden;
        }
        .checkin-pulse { width: 14px; height: 14px; border-radius: 999px; background: var(--orange); flex-shrink: 0; position: relative; }
        .checkin-pulse::before { content: ''; position: absolute; inset: -6px; border-radius: 999px; background: var(--orange); opacity: 0.3; animation: ckPulse 1.6s ease-out infinite; }
        @keyframes ckPulse { 0% { transform: scale(0.6); opacity: 0.5 } 100% { transform: scale(2.2); opacity: 0 } }
        .bk-stats { display: grid; grid-template-columns: repeat(3, 1fr); gap: 12px; }
        .passes-row { display: grid; grid-template-columns: repeat(3, 1fr); gap: 12px; }
        .mini-pass { background: linear-gradient(135deg, var(--indigo) 0%, #6c73c5 60%, var(--orange) 130%); color: #fff; border-radius: 14px; padding: 16px; position: relative; overflow: hidden; box-shadow: var(--shadow-sm); }
        @media (max-width: 720px) { .bk-stats, .passes-row { grid-template-columns: 1fr 1fr; } }
      `}</style>
    </>
  );
}

interface UpcomingCardProps {
  u: Upcoming;
  onOpenPass: () => void;
}

function UpcomingCard({ u, onOpenPass }: UpcomingCardProps) {
  return (
    <div className="card upcoming-card" onClick={onOpenPass}>
      <div className="uc-date">
        <span
          className="h-mono"
          style={{ fontSize: 10, letterSpacing: "0.18em", color: "var(--ink-mute)" }}
        >
          {u.startsAt.toLocaleDateString("en", { month: "short" }).toUpperCase()}
        </span>
        <span className="h-display" style={{ fontSize: 30, lineHeight: 1, color: "var(--indigo)" }}>
          {u.startsAt.getDate()}
        </span>
        <span
          className="h-mono"
          style={{ fontSize: 9, letterSpacing: "0.16em", color: "var(--ink-mute)" }}
        >
          {u.dayLabel.toUpperCase()}
        </span>
      </div>
      <div className="uc-bar" />
      <div className="grow" style={{ minWidth: 0 }}>
        <div className="h-headline" style={{ fontSize: 18, margin: 0 }}>
          {u.space.name}
        </div>
        <div
          className="h-mono"
          style={{ fontSize: 11, color: "var(--ink-mute)", letterSpacing: "0.1em", marginTop: 4 }}
        >
          {formatHour(u.startH)} — {formatHour(u.endH)} · {u.space.building.toUpperCase()}
        </div>
        <div className="row" style={{ gap: 6, flexWrap: "wrap", marginTop: 8 }}>
          <span className="pill pill-indigo">{u.tag}</span>
          {u.recurring && (
            <span
              className="pill"
              style={{ background: "var(--orange-pale)", color: "var(--orange-deep)" }}
            >
              ↻ Recurring
            </span>
          )}
        </div>
      </div>
      <FloorPlanSketch space={u.space} width={84} height={56} />
      <style>{`
        .upcoming-card { display: flex; gap: 16px; align-items: center; padding: 16px 18px; cursor: pointer; transition: transform .15s, box-shadow .15s; }
        .upcoming-card:hover { transform: translateY(-1px); box-shadow: var(--shadow-md); }
        .uc-date { display: flex; flex-direction: column; align-items: center; gap: 2px; min-width: 52px; }
        .uc-bar { width: 4px; align-self: stretch; border-radius: 999px; background: var(--indigo); }
      `}</style>
    </div>
  );
}

// ============================================================================
// PROFILE
// ============================================================================
function ProfileSection({ profile, fullHistory }: { profile: Profile; fullHistory: HistoryItem[] }) {
  const [editing, setEditing] = useState(false);
  const stats = profileStats(fullHistory);
  return (
    <>
      <header
        className="row"
        style={{ justifyContent: "space-between", alignItems: "flex-end", marginBottom: 20 }}
      >
        <div>
          <span className="kicker">Profile</span>
          <h1 className="acct-h1">How others see you</h1>
          <p className="acct-sub">Renters and space hosts see this when they look you up.</p>
        </div>
        <button className="btn btn-ghost" onClick={() => setEditing((e) => !e)}>
          {editing ? "✓ Done" : "✎ Edit profile"}
        </button>
      </header>

      <article className="profile-card">
        <div className="profile-cover" style={{ background: profile.coverGradient }}>
          <svg
            style={{ position: "absolute", inset: 0, width: "100%", height: "100%", opacity: 0.18 }}
            aria-hidden="true"
          >
            <defs>
              <pattern id="profile-grid" width="40" height="40" patternUnits="userSpaceOnUse">
                <path d="M 40 0 L 0 0 0 40" fill="none" stroke="#fff" strokeWidth="1" />
              </pattern>
            </defs>
            <rect width="100%" height="100%" fill="url(#profile-grid)" />
          </svg>
          {editing && (
            <button className="cover-edit-btn" title="Change cover">
              🎨 Cover
            </button>
          )}
        </div>

        <div className="profile-body">
          <div className="profile-id-row">
            <div className="profile-pic">
              <div className="profile-pic-bg" style={{ background: profile.coverGradient }} />
              <span>{profile.initials}</span>
              {editing && <button className="pic-edit-btn">📷</button>}
            </div>
            <div className="grow" style={{ minWidth: 0 }}>
              {editing ? (
                <input className="acct-input acct-input-lg" defaultValue={profile.displayName} />
              ) : (
                <h2 className="h-display" style={{ fontSize: 30, margin: 0, lineHeight: 1.1 }}>
                  {profile.displayName}
                </h2>
              )}
              <div className="row" style={{ gap: 10, marginTop: 6, flexWrap: "wrap" }}>
                <span
                  className="h-mono"
                  style={{ fontSize: 11, color: "var(--ink-mute)", letterSpacing: "0.12em" }}
                >
                  @{profile.handle}
                </span>
                <span style={{ color: "var(--ink-mute)" }}>·</span>
                <span style={{ fontSize: 13, color: "var(--ink-soft)" }}>{profile.realName}</span>
                <span className="profile-verified" title="Identity verified">
                  <svg width="14" height="14" viewBox="0 0 16 16" fill="none" stroke="currentColor" strokeWidth="2">
                    <path d="M3 8l3 3 7-7" />
                  </svg>
                  Verified student
                </span>
              </div>
            </div>
          </div>

          {editing ? (
            <textarea
              className="acct-input"
              rows={3}
              defaultValue={profile.bio}
              style={{ marginTop: 16, width: "100%", resize: "vertical" }}
            />
          ) : (
            <p
              style={{ fontSize: 15, color: "var(--ink-soft)", margin: "16px 0 0", lineHeight: 1.55 }}
            >
              {profile.bio}
            </p>
          )}

          <div className="profile-stats">
            <ProfileStat n={String(stats.total)} l="bookings" />
            <ProfileStat n={`${stats.hours}h`} l="hours hosted" />
            <ProfileStat n={profile.memberSince} l="member since" />
            <ProfileStat n={String(stats.uniqueSpaces)} l="spaces tried" />
          </div>

          <div className="profile-section">
            <h3 className="profile-h3">Vibe profile</h3>
            <p className="profile-section-sub">
              Based on the last 30 bookings — what {profile.displayName.split(" ")[0]} usually goes for.
            </p>
            <div className="vibe-grid">
              {profile.vibes.map((v) => (
                <div key={v.label} className="vibe-row">
                  <div className="row" style={{ justifyContent: "space-between", marginBottom: 4 }}>
                    <span style={{ fontSize: 14, fontWeight: 600 }}>
                      {v.icon} {v.label}
                    </span>
                    <span
                      className="h-mono"
                      style={{ fontSize: 11, color: "var(--ink-mute)", letterSpacing: "0.08em" }}
                    >
                      {v.percent}%
                    </span>
                  </div>
                  <div className="vibe-track">
                    <div
                      className="vibe-fill"
                      style={{
                        width: `${v.percent}%`,
                        background: v.percent > 60 ? "var(--orange)" : "var(--indigo)",
                      }}
                    />
                  </div>
                </div>
              ))}
            </div>
          </div>

          <div className="profile-section">
            <h3 className="profile-h3">Socials</h3>
            <div className="social-row">
              {profile.socials.map((s) => (
                <a key={s.kind} href="#" className="social-chip">
                  <span className="social-icon">{s.glyph}</span>
                  <span>{s.handle}</span>
                </a>
              ))}
              {editing && <button className="social-chip social-add">+ Add link</button>}
            </div>
          </div>
        </div>
      </article>

      <style>{`
        .profile-card { background: #fff; border: 1px solid var(--line-soft); border-radius: 16px; overflow: hidden; box-shadow: var(--shadow-sm); }
        .profile-cover { height: 160px; position: relative; }
        .cover-edit-btn { position: absolute; right: 14px; bottom: 14px; padding: 6px 12px; background: rgba(255,255,255,0.95); border-radius: 999px; font-size: 12px; font-weight: 600; box-shadow: var(--shadow-sm); cursor: pointer; }
        .profile-body { padding: 0 28px 28px; }
        .profile-id-row { display: flex; gap: 18px; align-items: flex-end; margin-top: -42px; }
        .profile-pic { width: 96px; height: 96px; border-radius: 999px; position: relative; overflow: hidden; border: 4px solid #fff; box-shadow: var(--shadow-sm); flex-shrink: 0; display: grid; place-items: center; color: #fff; font-weight: 700; font-size: 36px; }
        .profile-pic-bg { position: absolute; inset: 0; }
        .profile-pic span { position: relative; z-index: 1; text-shadow: 0 2px 4px rgba(0,0,0,0.25); }
        .pic-edit-btn { position: absolute; bottom: 0; right: 0; width: 32px; height: 32px; background: rgba(0,0,0,0.6); color: #fff; border-radius: 999px; z-index: 2; font-size: 14px; cursor: pointer; border: 2px solid #fff; }
        .profile-verified { display: inline-flex; align-items: center; gap: 4px; font-size: 12px; color: var(--green); background: var(--green-pale); padding: 3px 10px; border-radius: 999px; font-weight: 600; }
        .profile-stats { display: grid; grid-template-columns: repeat(4, 1fr); gap: 0; margin-top: 28px; padding: 18px 0; border-top: 1px solid var(--line-soft); border-bottom: 1px solid var(--line-soft); }
        .profile-stats > div + div { border-left: 1px solid var(--line-soft); }
        .profile-section { margin-top: 28px; }
        .profile-h3 { font-family: 'Lexend', sans-serif; font-weight: 600; font-size: 18px; margin: 0 0 4px; }
        .profile-section-sub { font-size: 13px; color: var(--ink-mute); margin: 0 0 16px; }
        .vibe-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 14px 24px; }
        .vibe-track { height: 6px; background: var(--paper-3); border-radius: 999px; overflow: hidden; }
        .vibe-fill { height: 100%; border-radius: 999px; transition: width .4s; }
        .social-row { display: flex; flex-wrap: wrap; gap: 8px; }
        .social-chip { display: inline-flex; align-items: center; gap: 8px; padding: 8px 14px; background: var(--paper-2); border-radius: 10px; font-size: 13px; font-weight: 500; color: var(--ink); text-decoration: none; transition: background .12s; cursor: pointer; border: 0; }
        .social-chip:hover { background: var(--paper-3); }
        .social-add { color: var(--indigo); border: 1px dashed var(--indigo) !important; background: transparent !important; }
        .social-icon { font-family: 'JetBrains Mono', monospace; font-weight: 700; color: var(--indigo); }
        .acct-input { padding: 10px 12px; border: 1px solid var(--line-soft); border-radius: 8px; font-family: 'Work Sans', sans-serif; font-size: 14px; outline: none; transition: border-color .12s, box-shadow .12s; background: #fff; }
        .acct-input:focus { border-color: var(--indigo); box-shadow: 0 0 0 3px rgba(75,82,167,0.15); }
        .acct-input-lg { font-family: 'Lexend', sans-serif; font-weight: 700; font-size: 24px; padding: 6px 10px; width: 100%; }
        @media (max-width: 720px) {
          .profile-stats { grid-template-columns: 1fr 1fr; }
          .profile-stats > div + div { border-left: 0; }
          .profile-stats > div:nth-child(3), .profile-stats > div:nth-child(4) { border-top: 1px solid var(--line-soft); padding-top: 12px; margin-top: 12px; }
          .vibe-grid { grid-template-columns: 1fr; }
        }
      `}</style>
    </>
  );
}

function ProfileStat({ n, l }: { n: string; l: string }) {
  return (
    <div style={{ padding: "4px 18px", textAlign: "center" }}>
      <div className="h-display" style={{ fontSize: 22, lineHeight: 1.05 }}>
        {n}
      </div>
      <div
        className="h-mono"
        style={{
          fontSize: 10,
          color: "var(--ink-mute)",
          letterSpacing: "0.16em",
          marginTop: 4,
          textTransform: "uppercase",
        }}
      >
        {l}
      </div>
    </div>
  );
}

// ============================================================================
// HISTORY
// ============================================================================
function HistorySection({ fullHistory }: { fullHistory: HistoryItem[] }) {
  const [filter, setFilter] = useState<string>("all");
  const tags = useMemo(() => ["all", ...Array.from(new Set(fullHistory.map((h) => h.tag)))], [fullHistory]);
  const filtered = filter === "all" ? fullHistory : fullHistory.filter((h) => h.tag === filter);

  const byMonth = useMemo<[string, HistoryItem[]][]>(() => {
    const groups: Record<string, HistoryItem[]> = {};
    filtered.forEach((h) => {
      const k = h.startsAt.toLocaleDateString("en", { year: "numeric", month: "long" });
      (groups[k] = groups[k] || []).push(h);
    });
    return Object.entries(groups);
  }, [filtered]);

  return (
    <>
      <header style={{ marginBottom: 20 }}>
        <span className="kicker">Past bookings</span>
        <h1 className="acct-h1">History</h1>
        <p className="acct-sub">Every space, every hour, in order.</p>
      </header>

      <div className="row" style={{ flexWrap: "wrap", gap: 6, marginBottom: 24 }}>
        {tags.map((t) => (
          <button
            key={t}
            className={`chip ${filter === t ? "active" : ""}`}
            onClick={() => setFilter(t)}
          >
            {t === "all" ? "All" : t}{" "}
            {t !== "all" && (
              <span className="h-mono" style={{ fontSize: 10, opacity: 0.7, marginLeft: 4 }}>
                {fullHistory.filter((h) => h.tag === t).length}
              </span>
            )}
          </button>
        ))}
      </div>

      <div className="hist-timeline">
        {byMonth.map(([month, items]) => (
          <div key={month}>
            <div className="hist-month">
              <span
                className="h-mono"
                style={{ fontSize: 11, letterSpacing: "0.18em", color: "var(--ink-mute)" }}
              >
                {month.toUpperCase()}
              </span>
              <span
                className="h-mono"
                style={{ fontSize: 10, color: "var(--ink-mute)", letterSpacing: "0.12em" }}
              >
                {items.length} {items.length === 1 ? "BOOKING" : "BOOKINGS"} ·{" "}
                {items.reduce((a, b) => a + b.duration, 0)}H
              </span>
            </div>
            {items.map((h, i) => (
              <div key={i} className="hist-row">
                <div className="hist-dot" />
                <div className="hist-day">
                  <span className="h-display" style={{ fontSize: 18, lineHeight: 1 }}>
                    {h.startsAt.getDate()}
                  </span>
                  <span
                    className="h-mono"
                    style={{ fontSize: 9, color: "var(--ink-mute)", letterSpacing: "0.14em" }}
                  >
                    {h.startsAt.toLocaleDateString("en", { weekday: "short" }).toUpperCase()}
                  </span>
                </div>
                <div className="grow">
                  <div style={{ fontWeight: 600, fontSize: 15 }}>{h.space.name}</div>
                  <div
                    className="h-mono"
                    style={{
                      fontSize: 11,
                      color: "var(--ink-mute)",
                      letterSpacing: "0.1em",
                      marginTop: 2,
                    }}
                  >
                    {formatHour(h.startH)}–{formatHour(h.endH)} · {h.duration}H ·{" "}
                    {h.space.building.toUpperCase()}
                  </div>
                </div>
                <span className="pill pill-booked">{h.tag}</span>
              </div>
            ))}
          </div>
        ))}
      </div>

      <style>{`
        .hist-timeline { position: relative; padding-left: 18px; }
        .hist-timeline::before { content: ''; position: absolute; left: 4px; top: 0; bottom: 0; width: 1px; background: var(--line-soft); }
        .hist-month { padding: 16px 0 8px; display: flex; justify-content: space-between; }
        .hist-row { display: flex; gap: 16px; padding: 14px 16px; border-radius: 10px; align-items: center; position: relative; }
        .hist-row:hover { background: var(--paper-2); }
        .hist-dot { position: absolute; left: -18px; width: 10px; height: 10px; border-radius: 999px; background: #fff; border: 2px solid var(--indigo); }
        .hist-day { min-width: 36px; display: flex; flex-direction: column; align-items: center; gap: 2px; }
      `}</style>
    </>
  );
}

// ============================================================================
// STATS
// ============================================================================
function StatsSection({ fullHistory }: { fullHistory: HistoryItem[] }) {
  const todayY = new Date().getFullYear();
  const yearStart = new Date(todayY, 0, 1);
  const thisYear = fullHistory.filter((h) => h.startsAt >= yearStart);
  const saved = thisYear.length * 6;

  const heatmap = useMemo(() => buildHeatmap(fullHistory), [fullHistory]);

  const bySpace = useMemo<[string, number][]>(() => {
    const m: Record<string, number> = {};
    fullHistory.forEach((h) => {
      m[h.space.name] = (m[h.space.name] || 0) + h.duration;
    });
    return Object.entries(m)
      .sort((a, b) => b[1] - a[1])
      .slice(0, 6);
  }, [fullHistory]);
  const maxBySpace = bySpace[0] ? bySpace[0][1] : 1;

  const byDay = useMemo(() => {
    const m = [0, 0, 0, 0, 0, 0, 0];
    fullHistory.forEach((h) => {
      m[h.startsAt.getDay()] += h.duration;
    });
    return m;
  }, [fullHistory]);

  return (
    <>
      <header style={{ marginBottom: 20 }}>
        <span className="kicker">The numbers</span>
        <h1 className="acct-h1">Stats</h1>
        <p className="acct-sub">Your year on Reservity.</p>
      </header>

      <div className="big-stats">
        <BigStat n={String(thisYear.length)} l="bookings this year" />
        <BigStat n={`${thisYear.reduce((a, b) => a + b.duration, 0)}h`} l="hours reserved" tone="indigo" />
        <BigStat n={String(new Set(thisYear.map((h) => h.space.id)).size)} l="distinct spaces" />
        <BigStat n={`$${saved}`} l="saved vs cafes" tone="orange" />
      </div>

      <section className="stats-card">
        <div className="row" style={{ justifyContent: "space-between", marginBottom: 12 }}>
          <h3 className="profile-h3" style={{ margin: 0 }}>
            Booking heatmap
          </h3>
          <span
            className="h-mono"
            style={{ fontSize: 11, color: "var(--ink-mute)", letterSpacing: "0.12em" }}
          >
            LAST 12 MONTHS
          </span>
        </div>
        <div className="heatmap-wrap">
          <div className="heatmap-rows">
            {["Mon", "Wed", "Fri"].map((d) => (
              <div key={d} className="hm-rowlabel">
                {d}
              </div>
            ))}
          </div>
          <div className="heatmap">
            {heatmap.weeks.map((week, wi) => (
              <div key={wi} className="hm-week">
                {week.map((cell, di) => (
                  <div key={di} className={`hm-cell hm-l${cell.level}`} title={cell.title} />
                ))}
              </div>
            ))}
          </div>
        </div>
        <div className="row gap-2" style={{ marginTop: 12, justifyContent: "flex-end" }}>
          <span
            className="h-mono"
            style={{ fontSize: 10, color: "var(--ink-mute)", letterSpacing: "0.12em" }}
          >
            LESS
          </span>
          {[0, 1, 2, 3, 4].map((l) => (
            <div key={l} className={`hm-cell hm-l${l}`} />
          ))}
          <span
            className="h-mono"
            style={{ fontSize: 10, color: "var(--ink-mute)", letterSpacing: "0.12em" }}
          >
            MORE
          </span>
        </div>
      </section>

      <div
        className="stats-twoup"
        style={{ display: "grid", gridTemplateColumns: "1fr 1fr", gap: 16, marginTop: 16 }}
      >
        <section className="stats-card">
          <h3 className="profile-h3" style={{ margin: "0 0 16px" }}>
            Where you spend hours
          </h3>
          <div className="col gap-2">
            {bySpace.map(([name, hrs]) => (
              <div key={name}>
                <div className="row" style={{ justifyContent: "space-between", marginBottom: 4 }}>
                  <span style={{ fontSize: 13, fontWeight: 500 }}>{name}</span>
                  <span className="h-mono" style={{ fontSize: 11, color: "var(--ink-mute)" }}>
                    {hrs}h
                  </span>
                </div>
                <div className="vibe-track">
                  <div
                    className="vibe-fill"
                    style={{ width: `${(hrs / maxBySpace) * 100}%`, background: "var(--indigo)" }}
                  />
                </div>
              </div>
            ))}
          </div>
        </section>

        <section className="stats-card">
          <h3 className="profile-h3" style={{ margin: "0 0 16px" }}>
            By day of week
          </h3>
          <div className="weekday-bars">
            {["S", "M", "T", "W", "T", "F", "S"].map((d, i) => {
              const max = Math.max(...byDay);
              const pct = max ? (byDay[i] / max) * 100 : 0;
              return (
                <div key={i} className="wd-col">
                  <div className="wd-bar-wrap">
                    <div className="wd-bar" style={{ height: `${pct}%` }} />
                  </div>
                  <span className="wd-label">{d}</span>
                  <span className="wd-num">{byDay[i]}h</span>
                </div>
              );
            })}
          </div>
        </section>
      </div>

      <style>{`
        .big-stats { display: grid; grid-template-columns: repeat(4, 1fr); gap: 12px; }
        .big-stat { padding: 22px 20px; border-radius: 14px; border: 1px solid var(--line-soft); background: #fff; position: relative; overflow: hidden; }
        .big-stat.tone-indigo { background: linear-gradient(135deg, var(--indigo), #6c73c5); color: #fff; border-color: var(--indigo); }
        .big-stat.tone-orange { background: linear-gradient(135deg, var(--orange), #ff9d65); color: #fff; border-color: var(--orange); }
        .stats-card { padding: 22px 24px; background: #fff; border: 1px solid var(--line-soft); border-radius: 14px; margin-top: 16px; }
        .heatmap-wrap { display: flex; gap: 8px; overflow-x: auto; padding-bottom: 4px; }
        .heatmap-rows { display: flex; flex-direction: column; gap: 2px; padding-top: 6px; flex-shrink: 0; }
        .hm-rowlabel { height: 13px; font-family: 'JetBrains Mono', monospace; font-size: 10px; color: var(--ink-mute); width: 24px; }
        .heatmap { display: flex; gap: 3px; }
        .hm-week { display: flex; flex-direction: column; gap: 3px; }
        .hm-cell { width: 13px; height: 13px; border-radius: 3px; background: var(--paper-3); }
        .hm-l0 { background: var(--paper-3); }
        .hm-l1 { background: rgba(75,82,167,0.25); }
        .hm-l2 { background: rgba(75,82,167,0.5); }
        .hm-l3 { background: rgba(75,82,167,0.75); }
        .hm-l4 { background: var(--indigo); }
        .weekday-bars { display: grid; grid-template-columns: repeat(7, 1fr); gap: 8px; height: 200px; align-items: end; }
        .wd-col { display: flex; flex-direction: column; align-items: center; gap: 4px; height: 100%; justify-content: flex-end; }
        .wd-bar-wrap { width: 28px; height: 100%; display: flex; align-items: flex-end; }
        .wd-bar { width: 100%; background: linear-gradient(180deg, var(--orange), var(--indigo)); border-radius: 4px 4px 0 0; min-height: 2px; transition: height .4s; }
        .wd-label { font-size: 11px; font-weight: 600; color: var(--ink-soft); }
        .wd-num { font-family: 'JetBrains Mono', monospace; font-size: 10px; color: var(--ink-mute); }
        @media (max-width: 720px) {
          .big-stats { grid-template-columns: 1fr 1fr; }
          .stats-twoup { grid-template-columns: 1fr !important; }
        }
      `}</style>
    </>
  );
}

interface BigStatProps {
  n: string;
  l: string;
  tone?: "indigo" | "orange";
}

function BigStat({ n, l, tone }: BigStatProps) {
  return (
    <div className={`big-stat ${tone ? `tone-${tone}` : ""}`}>
      <div
        className="h-display"
        style={{ fontSize: 42, lineHeight: 1, color: tone ? "#fff" : "var(--ink)" }}
      >
        {n}
      </div>
      <div
        className="h-mono"
        style={{
          fontSize: 11,
          letterSpacing: "0.16em",
          marginTop: 8,
          opacity: tone ? 0.85 : 0.7,
          textTransform: "uppercase",
        }}
      >
        {l}
      </div>
    </div>
  );
}

interface StatProps {
  n: string;
  unit: string;
  sub: string;
  tone?: "indigo" | "orange";
}

function Stat({ n, unit, sub, tone }: StatProps) {
  return (
    <div
      className="card"
      style={{ padding: 18, borderColor: tone === "indigo" ? "rgba(75,82,167,0.25)" : undefined }}
    >
      <div
        className="h-display"
        style={{
          fontSize: 32,
          color:
            tone === "orange"
              ? "var(--orange-deep)"
              : tone === "indigo"
                ? "var(--indigo)"
                : "var(--ink)",
          lineHeight: 1,
        }}
      >
        {n}
      </div>
      <div style={{ fontSize: 13, color: "var(--ink-soft)", marginTop: 6 }}>{unit}</div>
      <div
        className="h-mono"
        style={{ fontSize: 10, color: "var(--ink-mute)", letterSpacing: "0.14em", marginTop: 4 }}
      >
        {sub.toUpperCase()}
      </div>
    </div>
  );
}

// ============================================================================
// AUTHENTICATION
// ============================================================================
function AuthSection({ profile }: { profile: Profile }) {
  const [twoFA, setTwoFA] = useState(true);
  const [showChangePw, setShowChangePw] = useState(false);
  return (
    <>
      <header style={{ marginBottom: 28 }}>
        <span className="kicker">Settings</span>
        <h1 className="acct-h1">Authentication</h1>
        <p className="acct-sub">Email, password, two-factor — the locks on your front door.</p>
      </header>

      <div className="settings-stack">
        <SettingRow label="Email address" sub="Where we send pass receipts and notifications.">
          <div className="row" style={{ gap: 12, alignItems: "center" }}>
            <span style={{ fontSize: 15, fontWeight: 500 }}>{profile.email}</span>
            <span className="profile-verified">
              <svg width="14" height="14" viewBox="0 0 16 16" fill="none" stroke="currentColor" strokeWidth="2">
                <path d="M3 8l3 3 7-7" />
              </svg>
              Verified
            </span>
          </div>
          <button className="btn btn-ghost" style={{ padding: "6px 14px", fontSize: 13 }}>
            Change
          </button>
        </SettingRow>

        <SettingRow label="Password" sub="Last changed 47 days ago.">
          {showChangePw ? (
            <div style={{ flex: 1 }}>
              <div className="col gap-2" style={{ maxWidth: 380 }}>
                <input className="acct-input" type="password" placeholder="Current password" />
                <input className="acct-input" type="password" placeholder="New password" />
                <input className="acct-input" type="password" placeholder="Confirm new password" />
                <div className="row gap-2" style={{ marginTop: 4 }}>
                  <button className="btn btn-primary" style={{ padding: "8px 14px", fontSize: 13 }}>
                    Update
                  </button>
                  <button
                    className="btn btn-ghost"
                    style={{ padding: "8px 14px", fontSize: 13 }}
                    onClick={() => setShowChangePw(false)}
                  >
                    Cancel
                  </button>
                </div>
              </div>
            </div>
          ) : (
            <>
              <span style={{ fontSize: 15, color: "var(--ink-soft)", letterSpacing: "0.2em" }}>
                •••••••••••
              </span>
              <button
                className="btn btn-ghost"
                style={{ padding: "6px 14px", fontSize: 13 }}
                onClick={() => setShowChangePw(true)}
              >
                Change password
              </button>
            </>
          )}
        </SettingRow>

        <SettingRow
          label="Two-factor authentication"
          sub="A 6-digit code from your authenticator app, in addition to your password."
        >
          <div style={{ flex: 1 }}>
            <div
              className="row"
              style={{ justifyContent: "space-between", alignItems: "center" }}
            >
              <div>
                <div className="row" style={{ gap: 8 }}>
                  <Toggle on={twoFA} onChange={setTwoFA} />
                  <span style={{ fontSize: 14, fontWeight: 600 }}>{twoFA ? "Enabled" : "Disabled"}</span>
                </div>
                {twoFA && (
                  <div
                    className="h-mono"
                    style={{ fontSize: 11, color: "var(--ink-mute)", letterSpacing: "0.1em", marginTop: 6 }}
                  >
                    VIA AUTHENTICATOR APP · 8 BACKUP CODES REMAINING
                  </div>
                )}
              </div>
              {twoFA && (
                <button className="btn btn-ghost" style={{ padding: "6px 14px", fontSize: 13 }}>
                  Show backup codes
                </button>
              )}
            </div>
          </div>
        </SettingRow>

        <SettingRow label="Active sessions" sub="Where you're signed in right now." vertical>
          <div className="col gap-2" style={{ width: "100%" }}>
            {[
              { device: "MacBook Air · Safari", loc: "Hawthorn campus", last: "Active now", current: true },
              { device: "iPhone 15 · Reservity app", loc: "Hawthorn campus", last: "2 hours ago", current: false },
              { device: "Chrome · Library iMac", loc: "Linden Hall", last: "Yesterday", current: false },
            ].map((s, i) => (
              <div
                key={i}
                className="row"
                style={{
                  justifyContent: "space-between",
                  padding: "12px 14px",
                  background: "var(--paper-2)",
                  borderRadius: 10,
                }}
              >
                <div>
                  <div style={{ fontWeight: 600, fontSize: 14 }}>
                    {s.device}{" "}
                    {s.current && (
                      <span className="pill pill-indigo" style={{ fontSize: 10, padding: "1px 8px" }}>
                        This device
                      </span>
                    )}
                  </div>
                  <div
                    className="h-mono"
                    style={{
                      fontSize: 10,
                      color: "var(--ink-mute)",
                      letterSpacing: "0.1em",
                      marginTop: 2,
                    }}
                  >
                    {s.loc.toUpperCase()} · {s.last.toUpperCase()}
                  </div>
                </div>
                {!s.current && (
                  <button
                    className="btn btn-ghost"
                    style={{ padding: "4px 10px", fontSize: 12, color: "var(--orange-deep)" }}
                  >
                    Sign out
                  </button>
                )}
              </div>
            ))}
            <button
              style={{
                alignSelf: "flex-start",
                marginTop: 4,
                fontSize: 13,
                color: "var(--orange-deep)",
                fontWeight: 600,
                background: "transparent",
                border: 0,
                cursor: "pointer",
                padding: 0,
              }}
            >
              Sign out everywhere else →
            </button>
          </div>
        </SettingRow>

        <div className="danger-zone">
          <div>
            <div style={{ fontSize: 15, fontWeight: 600, color: "var(--orange-deep)" }}>
              Delete account
            </div>
            <div style={{ fontSize: 13, color: "var(--ink-soft)", marginTop: 4 }}>
              Permanently erase your bookings, pass history, and identity. Cannot be undone.
            </div>
          </div>
          <button
            className="btn btn-ghost"
            style={{ color: "var(--orange-deep)", borderColor: "rgba(159,66,0,0.3)" }}
          >
            Delete account…
          </button>
        </div>
      </div>

      <style>{`
        .settings-stack { display: flex; flex-direction: column; gap: 16px; }
        .danger-zone { display: flex; justify-content: space-between; align-items: center; gap: 16px; padding: 18px 22px; border: 1px solid rgba(159,66,0,0.25); border-radius: 12px; background: rgba(159,66,0,0.04); margin-top: 8px; }
        @media (max-width: 600px) { .danger-zone { flex-direction: column; align-items: stretch; } }
      `}</style>
    </>
  );
}

interface SettingRowProps {
  label: string;
  sub: string;
  children: React.ReactNode;
  vertical?: boolean;
}

function SettingRow({ label, sub, children, vertical }: SettingRowProps) {
  return (
    <div className="setting-row">
      <div className="setting-label">
        <div style={{ fontSize: 15, fontWeight: 600 }}>{label}</div>
        <div style={{ fontSize: 13, color: "var(--ink-mute)", marginTop: 4 }}>{sub}</div>
      </div>
      <div className={`setting-control ${vertical ? "vertical" : ""}`}>{children}</div>
      <style>{`
        .setting-row { display: grid; grid-template-columns: minmax(220px, 0.8fr) 1.2fr; gap: 32px; padding: 22px 24px; background: #fff; border: 1px solid var(--line-soft); border-radius: 12px; align-items: center; }
        .setting-label { min-width: 0; }
        .setting-control { display: flex; gap: 12px; align-items: center; justify-content: space-between; min-width: 0; }
        .setting-control.vertical { display: block; }
        @media (max-width: 720px) { .setting-row { grid-template-columns: 1fr; gap: 16px; padding: 18px 20px; } }
      `}</style>
    </div>
  );
}

function Toggle({ on, onChange }: { on: boolean; onChange: (v: boolean) => void }) {
  return (
    <button
      type="button"
      className={`toggle ${on ? "on" : ""}`}
      onClick={() => onChange(!on)}
      role="switch"
      aria-checked={on}
    >
      <span className="toggle-knob" />
      <style>{`
        .toggle { width: 44px; height: 24px; border-radius: 999px; background: var(--paper-3); position: relative; cursor: pointer; transition: background .2s; border: 0; flex-shrink: 0; }
        .toggle.on { background: var(--indigo); }
        .toggle-knob { position: absolute; top: 2px; left: 2px; width: 20px; height: 20px; border-radius: 999px; background: #fff; box-shadow: 0 1px 3px rgba(0,0,0,0.2); transition: left .2s; }
        .toggle.on .toggle-knob { left: 22px; }
      `}</style>
    </button>
  );
}

// ============================================================================
// APPEARANCE
// ============================================================================
type ThemeId = "light" | "dark" | "sepia" | "auto";
type AccentId = "indigo" | "plum" | "forest" | "rust" | "slate";
type DensityId = "cozy" | "comfortable" | "compact";

function readLs(key: string, fallback: string) {
  try {
    return localStorage.getItem(key) ?? fallback;
  } catch {
    return fallback;
  }
}

function AppearanceSection() {
  const [theme, setTheme] = useState<ThemeId>(() => readLs("rv-theme", "auto") as ThemeId);
  const [accent, setAccent] = useState<AccentId>(() => readLs("rv-accent", "indigo") as AccentId);
  const [density, setDensity] = useState<DensityId>(() => readLs("rv-density", "comfortable") as DensityId);
  const [ornament, setOrnament] = useState(() => readLs("rv-ornament", "on") !== "off");
  const [reduceMotion, setReduceMotion] = useState(() => readLs("rv-rm", "off") === "on");

  useEffect(() => {
    try {
      localStorage.setItem("rv-theme", theme);
    } catch {
      /* ignore */
    }
    document.documentElement.setAttribute("data-theme", theme);
  }, [theme]);
  useEffect(() => {
    try {
      localStorage.setItem("rv-accent", accent);
    } catch {
      /* ignore */
    }
    document.documentElement.setAttribute("data-accent", accent);
  }, [accent]);
  useEffect(() => {
    try {
      localStorage.setItem("rv-density", density);
    } catch {
      /* ignore */
    }
  }, [density]);
  useEffect(() => {
    try {
      localStorage.setItem("rv-ornament", ornament ? "on" : "off");
    } catch {
      /* ignore */
    }
    document.documentElement.setAttribute("data-ornament", ornament ? "on" : "off");
  }, [ornament]);
  useEffect(() => {
    try {
      localStorage.setItem("rv-rm", reduceMotion ? "on" : "off");
    } catch {
      /* ignore */
    }
  }, [reduceMotion]);

  const accents: { id: AccentId; name: string; color: string }[] = [
    { id: "indigo", name: "Indigo", color: "#4b52a7" },
    { id: "plum", name: "Plum", color: "#7c3a8c" },
    { id: "forest", name: "Forest", color: "#2f6b3a" },
    { id: "rust", name: "Rust", color: "#a0461c" },
    { id: "slate", name: "Slate", color: "#3d4350" },
  ];
  const themes: { id: ThemeId; name: string; desc: string }[] = [
    { id: "light", name: "Light", desc: "Lavender paper" },
    { id: "dark", name: "Dark", desc: "Indigo midnight" },
    { id: "sepia", name: "Sepia", desc: "Old library warmth" },
    { id: "auto", name: "Auto", desc: "Follow system" },
  ];
  const densities: DensityId[] = ["cozy", "comfortable", "compact"];

  return (
    <>
      <header style={{ marginBottom: 28 }}>
        <span className="kicker">Settings</span>
        <h1 className="acct-h1">Appearance</h1>
        <p className="acct-sub">Make Reservity look like yours.</p>
      </header>

      <section className="appear-section">
        <h3 className="profile-h3" style={{ margin: "0 0 4px" }}>
          Theme
        </h3>
        <p className="profile-section-sub">
          Light is the default. Dark is gentler at night. Auto follows your system.
        </p>
        <div className="theme-grid">
          {themes.map((t) => (
            <button
              key={t.id}
              className={`theme-card ${theme === t.id ? "active" : ""}`}
              onClick={() => setTheme(t.id)}
            >
              <ThemePreview theme={t.id} />
              <div style={{ padding: "12px 14px" }}>
                <div style={{ fontSize: 14, fontWeight: 600 }}>{t.name}</div>
                <div
                  className="h-mono"
                  style={{
                    fontSize: 10,
                    color: "var(--ink-mute)",
                    letterSpacing: "0.1em",
                    marginTop: 2,
                  }}
                >
                  {t.desc.toUpperCase()}
                </div>
              </div>
            </button>
          ))}
        </div>
      </section>

      <section className="appear-section">
        <h3 className="profile-h3" style={{ margin: "0 0 4px" }}>
          Accent color
        </h3>
        <p className="profile-section-sub">
          Used for primary actions, links, and the now-line on schedules.
        </p>
        <div className="row" style={{ gap: 10, flexWrap: "wrap" }}>
          {accents.map((a) => (
            <button
              key={a.id}
              className={`accent-swatch ${accent === a.id ? "active" : ""}`}
              onClick={() => setAccent(a.id)}
              title={a.name}
            >
              <span className="accent-color" style={{ background: a.color }} />
              <span style={{ fontSize: 12, fontWeight: 500 }}>{a.name}</span>
            </button>
          ))}
        </div>
      </section>

      <section className="appear-section">
        <h3 className="profile-h3" style={{ margin: "0 0 4px" }}>
          Density
        </h3>
        <p className="profile-section-sub">More breathing room, or pack more in.</p>
        <div className="density-tabs">
          {densities.map((d) => (
            <button
              key={d}
              className={`density-tab ${density === d ? "active" : ""}`}
              onClick={() => setDensity(d)}
            >
              <DensityIcon mode={d} />
              <span>{d[0].toUpperCase() + d.slice(1)}</span>
            </button>
          ))}
        </div>
      </section>

      <section className="appear-section">
        <h3 className="profile-h3" style={{ margin: "0 0 16px" }}>
          Quirks
        </h3>
        <div className="col gap-3">
          <ToggleRow
            label="Architectural ornament"
            sub="Floor plans, isometric room sketches, grid paper. Off = bare and minimal."
            on={ornament}
            onChange={setOrnament}
          />
          <ToggleRow
            label="Reduce motion"
            sub="Disable slide-ins, the pulsing live dot, and other movement."
            on={reduceMotion}
            onChange={setReduceMotion}
          />
        </div>
      </section>

      <style>{`
        .appear-section { padding: 24px 26px; background: #fff; border: 1px solid var(--line-soft); border-radius: 14px; margin-bottom: 16px; }
        .theme-grid { display: grid; grid-template-columns: repeat(4, 1fr); gap: 12px; margin-top: 12px; }
        .theme-card { padding: 0; border-radius: 12px; overflow: hidden; cursor: pointer; background: #fff; border: 2px solid var(--line-soft); transition: border-color .12s, transform .12s; text-align: left; }
        .theme-card:hover { border-color: var(--ink-mute); }
        .theme-card.active { border-color: var(--indigo); box-shadow: 0 0 0 3px rgba(75,82,167,0.15); }
        .accent-swatch { display: inline-flex; align-items: center; gap: 8px; padding: 8px 14px; border-radius: 999px; background: var(--paper-2); cursor: pointer; border: 2px solid transparent; }
        .accent-swatch:hover { background: var(--paper-3); }
        .accent-swatch.active { border-color: var(--ink); background: #fff; }
        .accent-color { width: 16px; height: 16px; border-radius: 999px; box-shadow: inset 0 0 0 1px rgba(0,0,0,0.1); }
        .density-tabs { display: flex; gap: 8px; margin-top: 8px; }
        .density-tab { display: flex; flex-direction: column; align-items: center; gap: 8px; padding: 14px 24px; border-radius: 12px; background: var(--paper-2); cursor: pointer; flex: 1; border: 2px solid transparent; transition: all .12s; font-size: 13px; font-weight: 600; }
        .density-tab.active { background: #fff; border-color: var(--indigo); color: var(--indigo); }
        @media (max-width: 720px) { .theme-grid { grid-template-columns: 1fr 1fr; } }
      `}</style>
    </>
  );
}

function ThemePreview({ theme }: { theme: ThemeId }) {
  const styles: Record<ThemeId, { bg: string; card: string; text: string; muted: string; accent: string }> = {
    light: { bg: "#fbf8fe", card: "#fff", text: "#1b1b1f", muted: "#767683", accent: "#4b52a7" },
    dark: { bg: "#1a1b29", card: "#252739", text: "#f3f0f5", muted: "#7d7e94", accent: "#bec2ff" },
    sepia: { bg: "#f3eddd", card: "#fbf6e8", text: "#2e261a", muted: "#857a5e", accent: "#a85c2c" },
    auto: {
      bg: "linear-gradient(90deg, #fbf8fe 50%, #1a1b29 50%)",
      card: "#fff",
      text: "#1b1b1f",
      muted: "#767683",
      accent: "#4b52a7",
    },
  };
  const s = styles[theme];
  return (
    <div style={{ height: 96, background: s.bg, padding: 12, position: "relative" }}>
      <div
        style={{
          background: theme === "auto" ? "rgba(255,255,255,0.85)" : s.card,
          padding: 8,
          borderRadius: 6,
          boxShadow: "0 1px 3px rgba(0,0,0,0.08)",
        }}
      >
        <div style={{ height: 4, width: "70%", background: s.text, borderRadius: 2, opacity: 0.85 }} />
        <div style={{ height: 3, width: "50%", background: s.muted, borderRadius: 2, marginTop: 5 }} />
        <div style={{ height: 3, width: "60%", background: s.muted, borderRadius: 2, marginTop: 3 }} />
        <div style={{ height: 8, width: 28, background: s.accent, borderRadius: 4, marginTop: 6 }} />
      </div>
    </div>
  );
}

function DensityIcon({ mode }: { mode: DensityId }) {
  const gaps: Record<DensityId, number> = { cozy: 6, comfortable: 4, compact: 2 };
  const g = gaps[mode];
  return (
    <svg width="32" height="20" viewBox="0 0 32 20" fill="none">
      <rect x="2" y="2" width="28" height="3" rx="1" fill="currentColor" opacity="0.4" />
      <rect x="2" y={5 + g} width="28" height="3" rx="1" fill="currentColor" opacity="0.7" />
      <rect x="2" y={8 + 2 * g} width="28" height="3" rx="1" fill="currentColor" />
    </svg>
  );
}

function ToggleRow({
  label,
  sub,
  on,
  onChange,
}: {
  label: string;
  sub: string;
  on: boolean;
  onChange: (v: boolean) => void;
}) {
  return (
    <div
      className="row"
      style={{
        justifyContent: "space-between",
        padding: "14px 16px",
        background: "var(--paper-2)",
        borderRadius: 10,
        gap: 16,
      }}
    >
      <div>
        <div style={{ fontSize: 14, fontWeight: 600 }}>{label}</div>
        <div style={{ fontSize: 12, color: "var(--ink-mute)", marginTop: 4 }}>{sub}</div>
      </div>
      <Toggle on={on} onChange={onChange} />
    </div>
  );
}

// ============================================================================
// Icons
// ============================================================================
function Svg(children: JSX.Element) {
  return () => (
    <svg
      className="si-icon"
      viewBox="0 0 24 24"
      fill="none"
      stroke="currentColor"
      strokeWidth="1.6"
      strokeLinecap="round"
      strokeLinejoin="round"
    >
      {children}
    </svg>
  );
}

const IconCalendar = Svg(
  <g>
    <rect x="3" y="5" width="18" height="16" rx="2" />
    <line x1="3" y1="10" x2="21" y2="10" />
    <line x1="8" y1="3" x2="8" y2="7" />
    <line x1="16" y1="3" x2="16" y2="7" />
  </g>,
);
const IconUser = Svg(
  <g>
    <circle cx="12" cy="8" r="4" />
    <path d="M4 21c0-4 4-7 8-7s8 3 8 7" />
  </g>,
);
const IconArchive = Svg(
  <g>
    <rect x="3" y="4" width="18" height="4" rx="1" />
    <path d="M5 8v12h14V8" />
    <line x1="10" y1="13" x2="14" y2="13" />
  </g>,
);
const IconBars = Svg(
  <g>
    <line x1="6" y1="20" x2="6" y2="10" />
    <line x1="12" y1="20" x2="12" y2="4" />
    <line x1="18" y1="20" x2="18" y2="14" />
  </g>,
);
const IconKey = Svg(
  <g>
    <circle cx="8" cy="14" r="4" />
    <path d="M11 14h11l-3 3m3-3l-3-3" />
  </g>,
);
const IconSparkle = Svg(
  <g>
    <path d="M12 3v6m0 6v6M3 12h6m6 0h6M5.5 5.5l4 4m5 5l4 4m0-13l-4 4m-5 5l-4 4" />
  </g>,
);
const IconLogout = Svg(
  <g>
    <path d="M9 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h4" />
    <polyline points="16 17 21 12 16 7" />
    <line x1="21" y1="12" x2="9" y2="12" />
  </g>,
);

// ============================================================================
// Data builders
// ============================================================================
function formatMemberSince(iso: string | null | undefined): string | null {
  if (!iso) return null;
  const d = new Date(iso);
  if (isNaN(d.getTime())) return null;
  return d.toLocaleDateString("en", { month: "short", year: "2-digit" }).replace(" ", " '");
}

function minimalSpaceFromSummary(s: {
  id: string;
  name: string;
  type: string;
  building: string;
  seats: number;
  area: number;
  price: number;
  currency: string;
  blurb: string;
}): Space {
  return {
    id: s.id,
    name: s.name,
    type: (s.type as Space["type"]) ?? "open",
    building: s.building,
    floor: 0,
    room: "",
    seats: s.seats,
    area: s.area,
    price: s.price,
    currency: s.currency,
    vibes: [],
    booked: [],
    events: [],
    viewing: 0,
    todayBookings: 0,
    blurb: s.blurb,
    description: "",
    amenities: [],
    rules: [],
    pin: { x: 0.5, y: 0.5 },
    slug: s.id,
  };
}

function buildUpcoming(now: Date, spaceLookup: Map<string, Space>): Upcoming[] {
  const fallback = spaceLookup.values().next().value as Space | undefined;
  if (!fallback) return [];
  const seeds: { spaceId: string; daysAhead: number; startH: number; endH: number; tag: string; recurring: boolean }[] = [
    { spaceId: "pod-3", daysAhead: 0, startH: 16, endH: 18, tag: "Solo focus", recurring: false },
    { spaceId: "lab-4b", daysAhead: 2, startH: 10, endH: 12, tag: "Group session", recurring: false },
    { spaceId: "studio-grand", daysAhead: 4, startH: 14, endH: 16, tag: "Recording", recurring: true },
    { spaceId: "pod-3", daysAhead: 7, startH: 16, endH: 18, tag: "Solo focus", recurring: true },
  ];
  return seeds.map((it) => {
    const space = spaceLookup.get(it.spaceId) ?? fallback;
    const startsAt = new Date(now);
    startsAt.setDate(startsAt.getDate() + it.daysAhead);
    startsAt.setHours(it.startH, 0, 0, 0);
    const dayLabel =
      it.daysAhead === 0
        ? "Today"
        : it.daysAhead === 1
          ? "Tomorrow"
          : startsAt.toLocaleDateString("en", { weekday: "short" });
    return {
      space,
      startsAt,
      startH: it.startH,
      endH: it.endH,
      duration: it.endH - it.startH,
      tag: it.tag,
      recurring: it.recurring,
      dayLabel,
    };
  });
}

function buildHistory(now: Date, spaceLookup: Map<string, Space>): HistoryItem[] {
  const fallback = spaceLookup.values().next().value as Space | undefined;
  if (!fallback) return [];
  const seeds: { sid: string; m: number; d: number; sh: number; dur: number; t: string }[] = [
    { sid: "pod-3", m: -1, d: 28, sh: 14, dur: 2, t: "Solo focus" },
    { sid: "lab-4b", m: -1, d: 24, sh: 9, dur: 3, t: "Group session" },
    { sid: "pod-3", m: -1, d: 22, sh: 18, dur: 4, t: "Solo focus" },
    { sid: "atrium", m: -1, d: 19, sh: 11, dur: 1, t: "Drop-in" },
    { sid: "studio-grand", m: -1, d: 16, sh: 14, dur: 2, t: "Group session" },
    { sid: "pod-3", m: -1, d: 14, sh: 10, dur: 3, t: "Solo focus" },
    { sid: "studio-c", m: -1, d: 11, sh: 13, dur: 2, t: "Group session" },
    { sid: "pod-7", m: -1, d: 8, sh: 9, dur: 4, t: "Solo focus" },
    { sid: "crit-room", m: -1, d: 5, sh: 15, dur: 2, t: "Group session" },
    { sid: "rooftop", m: -1, d: 3, sh: 12, dur: 1, t: "Drop-in" },
    { sid: "pod-3", m: -2, d: 28, sh: 9, dur: 3, t: "Solo focus" },
    { sid: "lab-4b", m: -2, d: 24, sh: 13, dur: 2, t: "Group session" },
    { sid: "darkroom", m: -2, d: 19, sh: 14, dur: 4, t: "Solo focus" },
    { sid: "pod-3", m: -2, d: 14, sh: 16, dur: 2, t: "Solo focus" },
    { sid: "atrium", m: -2, d: 11, sh: 12, dur: 1, t: "Drop-in" },
    { sid: "studio-c", m: -2, d: 7, sh: 10, dur: 3, t: "Group session" },
    { sid: "pod-3", m: -3, d: 26, sh: 9, dur: 4, t: "Solo focus" },
    { sid: "lab-2a", m: -3, d: 21, sh: 11, dur: 2, t: "Group session" },
    { sid: "pod-3", m: -3, d: 17, sh: 14, dur: 3, t: "Solo focus" },
    { sid: "crit-room", m: -3, d: 12, sh: 15, dur: 2, t: "Group session" },
    { sid: "pod-3", m: -3, d: 8, sh: 10, dur: 4, t: "Solo focus" },
    { sid: "pod-7", m: -4, d: 22, sh: 13, dur: 3, t: "Solo focus" },
    { sid: "studio-grand", m: -4, d: 18, sh: 14, dur: 2, t: "Recording" },
    { sid: "lab-4b", m: -4, d: 13, sh: 9, dur: 3, t: "Group session" },
    { sid: "pod-3", m: -4, d: 8, sh: 16, dur: 2, t: "Solo focus" },
    { sid: "atrium", m: -5, d: 27, sh: 11, dur: 1, t: "Drop-in" },
    { sid: "pod-3", m: -5, d: 22, sh: 9, dur: 3, t: "Solo focus" },
    { sid: "rooftop", m: -5, d: 16, sh: 17, dur: 2, t: "Drop-in" },
    { sid: "pod-3", m: -6, d: 24, sh: 14, dur: 3, t: "Solo focus" },
    { sid: "crit-room", m: -6, d: 18, sh: 15, dur: 2, t: "Group session" },
    { sid: "lab-4b", m: -7, d: 22, sh: 10, dur: 3, t: "Group session" },
    { sid: "pod-3", m: -7, d: 14, sh: 16, dur: 4, t: "Solo focus" },
    { sid: "studio-c", m: -8, d: 18, sh: 13, dur: 3, t: "Group session" },
    { sid: "pod-3", m: -8, d: 10, sh: 9, dur: 2, t: "Solo focus" },
    { sid: "atrium", m: -9, d: 20, sh: 12, dur: 1, t: "Drop-in" },
    { sid: "pod-7", m: -10, d: 15, sh: 14, dur: 4, t: "Solo focus" },
  ];
  return seeds
    .map((s) => {
      const space = spaceLookup.get(s.sid) ?? fallback;
      const startsAt = new Date(now);
      startsAt.setMonth(startsAt.getMonth() + s.m);
      startsAt.setDate(s.d);
      startsAt.setHours(s.sh, 0, 0, 0);
      return {
        space,
        startsAt,
        startH: s.sh,
        endH: s.sh + s.dur,
        duration: s.dur,
        tag: s.t,
      };
    })
    .sort((a, b) => b.startsAt.getTime() - a.startsAt.getTime());
}

function buildProfile(): Profile {
  return {
    displayName: "Maya Reyes",
    realName: "Maya Asuncion Reyes",
    handle: "mayareyes",
    initials: "MR",
    email: "maya.r@university.edu",
    bio: "Cog sci & studio practice. I book the same coding pod every Tuesday and pretend it's mine. Currently writing a thesis about how rooms shape thinking.",
    memberSince: "Sep '24",
    coverGradient: "linear-gradient(135deg, #4b52a7 0%, #646bc2 50%, #ff823c 130%)",
    socials: [
      { kind: "site", glyph: "◐", handle: "mayareyes.studio" },
      { kind: "github", glyph: "<>", handle: "github.com/mayareyes" },
      { kind: "instagram", glyph: "▢", handle: "@maya.builds" },
      { kind: "lab", glyph: "⌬", handle: "CogLab member" },
    ],
    vibes: [
      { label: "Focus mode", icon: "◐", percent: 78 },
      { label: "Solo", icon: "·", percent: 64 },
      { label: "Window seat", icon: "▢", percent: 52 },
      { label: "After hours", icon: "◑", percent: 41 },
      { label: "Natural light", icon: "☼", percent: 34 },
      { label: "Group of 4+", icon: "◊", percent: 18 },
    ],
  };
}

function profileStats(history: HistoryItem[]) {
  return {
    total: history.length,
    hours: history.reduce((a, b) => a + b.duration, 0),
    uniqueSpaces: new Set(history.map((h) => h.space.id)).size,
  };
}

interface HeatmapCell {
  level: number;
  title: string;
}

function buildHeatmap(history: HistoryItem[]): { weeks: HeatmapCell[][] } {
  const end = new Date();
  end.setHours(0, 0, 0, 0);
  const saturday = new Date(end);
  saturday.setDate(end.getDate() + (6 - end.getDay()));
  const start = new Date(saturday);
  start.setDate(saturday.getDate() - 53 * 7 + 1);

  const counts: Record<string, number> = {};
  history.forEach((h) => {
    const k = h.startsAt.toDateString();
    counts[k] = (counts[k] || 0) + h.duration;
  });

  const weeks: HeatmapCell[][] = [];
  const cursor = new Date(start);
  for (let w = 0; w < 53; w++) {
    const week: HeatmapCell[] = [];
    for (let d = 0; d < 7; d++) {
      const c = counts[cursor.toDateString()] || 0;
      let level = 0;
      if (c >= 1) level = 1;
      if (c >= 2) level = 2;
      if (c >= 3) level = 3;
      if (c >= 5) level = 4;
      week.push({ level, title: `${cursor.toDateString()} · ${c}h` });
      cursor.setDate(cursor.getDate() + 1);
    }
    weeks.push(week);
  }
  return { weeks };
}
