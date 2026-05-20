import { useEffect, useMemo, useState } from "react";
import { useNavigate, useParams } from "react-router-dom";
import { VIBES } from "@/data/fixtures";
import { formatHour, getStatus } from "@/data/booking";
import { useApp } from "@/state/AppStore";
import type { Space, SpaceEvent, Vibe, VibeId } from "@/types";
import FloorPlanSketch from "@/components/viz/FloorPlanSketch";
import Sparkline from "@/components/viz/Sparkline";
import SpacePhoto from "@/components/viz/PhotoFrame";
import { useSpaces } from "@/hooks/useSpaces";
import { useSpace } from "@/hooks/useSpace";
import { useBuildings } from "@/hooks/useBuildings";
import { useAvailabilityBatch } from "@/hooks/useAvailabilityBatch";
import { localIsoDate } from "@/hooks/localTime";
import { useNow } from "@/hooks/useNow";

function buildDateWindow(now: Date): Date[] {
  const start = new Date(now.getFullYear(), now.getMonth(), now.getDate());
  return Array.from({ length: 14 }, (_, i) => {
    const d = new Date(start);
    d.setDate(start.getDate() + i);
    return d;
  });
}

type SpaceTypeFilter = "all" | "lab" | "pod" | "open" | "studio";
type PriceFilter = "any" | "free" | "cheap" | "mid" | "high";

interface Filters {
  minCapacity: number;
  type: SpaceTypeFilter;
  vibes: Set<VibeId>;
  price: PriceFilter;
  buildings: Set<string>;
  openNow: boolean;
  hideFull: boolean;
}

interface ResolvedSpace extends Space {
  fullyBooked: boolean;
}

export default function ListingsRoute() {
  const navigate = useNavigate();
  const { id: routeSpaceId } = useParams<{ id: string }>();
  const { openBooking, showToast } = useApp();
  const { now } = useNow();
  const currentHour = now.getHours();

  const dateWindow = useMemo(() => buildDateWindow(now), [now]);

  const [view, setView] = useState<"split" | "schedule">("split");
  const [filterOpen, setFilterOpen] = useState(false);
  const [selectedId, setSelectedId] = useState<string | null>(routeSpaceId ?? null);
  const [detailOpen, setDetailOpen] = useState<boolean>(!!routeSpaceId);
  const [notified, setNotified] = useState<Set<string>>(new Set());
  const [surprise, setSurprise] = useState<string | null>(null);
  const [selectedDate, setSelectedDate] = useState<Date>(() => {
    return new Date(now.getFullYear(), now.getMonth(), now.getDate());
  });

  // Server-driven listings (M2). Filters apply server-side via query params.
  const { data: spacesData } = useSpaces({ size: 50 });
  const allSpaces = useMemo<Space[]>(() => spacesData?.spaces ?? [], [spacesData]);
  const { data: buildingsData } = useBuildings();
  const buildingNames = useMemo(
    () => (buildingsData ?? []).map((b) => b.name),
    [buildingsData],
  );

  const maxCapacity = useMemo(
    () => (allSpaces.length ? Math.max(...allSpaces.map((s) => s.seats)) : 12),
    [allSpaces],
  );

  // Sync route param → state when URL changes (deep-link)
  useEffect(() => {
    if (!routeSpaceId) return;
    if (!allSpaces.length) return; // wait for spaces to load
    const known = allSpaces.some((s) => s.id === routeSpaceId);
    if (known) {
      setSelectedId(routeSpaceId);
      setDetailOpen(true);
    } else {
      showToast(`No space matches “${routeSpaceId}”.`, "error");
      navigate("/spaces", { replace: true });
    }
  }, [routeSpaceId, allSpaces, navigate, showToast]);

  const isToday = localIsoDate(selectedDate) === localIsoDate(now);
  const refHour = isToday ? now.getHours() : 8;

  // Pull availability for ALL listed spaces on the selected date in one call.
  const slugs = useMemo(() => allSpaces.map((s) => s.id), [allSpaces]);
  const { data: availability } = useAvailabilityBatch(slugs, selectedDate);

  // Fetch full details for the selected space to ensure synchronized info (description, amenities, etc.)
  const { data: fullSpaceDetail } = useSpace(selectedId || undefined);

  const spacesForDate = useMemo<ResolvedSpace[]>(
    () =>
      allSpaces.map((s) => {
        const day = availability?.spaces[s.id];
        const booked = day ? Array.from(day.booked).sort((a, b) => a - b) : [];
        const events: SpaceEvent[] = (day?.events ?? []).map((e) => ({
          startH: e.startH,
          endH: e.endH,
          title: e.title,
          host: e.host,
        }));
        
        // Merge with full detail if this is the selected space
        const details = (fullSpaceDetail && fullSpaceDetail.id === s.id) ? fullSpaceDetail : {};

        return {
          ...s,
          ...details,
          booked,
          events,
          // M9: revisit fullyBooked when per-local-day open-hours coverage is plumbed.
          fullyBooked: false,
        };
      }),
    [allSpaces, availability, fullSpaceDetail],
  );


  const [filters, setFilters] = useState<Filters>({
    minCapacity: 1,
    type: "all",
    vibes: new Set<VibeId>(),
    price: "any",
    buildings: new Set<string>(),
    openNow: false,
    hideFull: false,
  });

  const filtered = useMemo<ResolvedSpace[]>(() => {
    return spacesForDate.filter((s) => {
      if (s.seats < filters.minCapacity) return false;
      if (filters.type !== "all" && s.type !== filters.type) return false;
      if (filters.vibes.size && !s.vibes.some((v) => filters.vibes.has(v))) return false;
      if (filters.buildings.size && !filters.buildings.has(s.building)) return false;
      if (filters.openNow && (s.fullyBooked || s.booked.includes(refHour))) return false;
      if (filters.hideFull && s.fullyBooked) return false;
      if (filters.price === "free" && s.price !== 0) return false;
      if (filters.price === "cheap" && s.price > 10) return false;
      if (filters.price === "mid" && (s.price < 10 || s.price > 20)) return false;
      if (filters.price === "high" && s.price < 20) return false;
      return true;
    });
  }, [spacesForDate, filters, refHour]);

  useEffect(() => {
    if (selectedId && filtered.length && !filtered.some((s) => s.id === selectedId)) {
      setDetailOpen(false);
    }
  }, [filtered, selectedId]);

  // Detail-panel data: pull rich space (description, amenities, rules, images)
  // when an item is selected. Falls back to the summary row before the GET resolves.
  const { data: selectedFull } = useSpace(selectedId ?? undefined);
  const selectedSummary = spacesForDate.find((s) => s.id === selectedId) ?? null;
  const selected: ResolvedSpace | null = selectedSummary
    ? {
        ...selectedSummary,
        ...(selectedFull ?? {}),
        // Preserve booked/events/fullyBooked from the day-of availability batch
        booked: selectedSummary.booked,
        events: selectedSummary.events,
        fullyBooked: selectedSummary.fullyBooked,
      }
    : null;

  const openDetail = (id: string) => {
    setSelectedId(id);
    setDetailOpen(true);
  };
  const closeDetail = () => {
    setDetailOpen(false);
    if (routeSpaceId) navigate("/spaces", { replace: true });
  };

  const activeFilterCount =
    (filters.minCapacity > 1 ? 1 : 0) +
    (filters.type !== "all" ? 1 : 0) +
    filters.vibes.size +
    (filters.price !== "any" ? 1 : 0) +
    filters.buildings.size +
    (filters.openNow ? 1 : 0) +
    (filters.hideFull ? 1 : 0);

  const handleNotify = (id: string) => {
    setNotified((prev) => {
      const n = new Set(prev);
      if (n.has(id)) n.delete(id);
      else n.add(id);
      return n;
    });
  };

  const handleSurprise = () => {
    const underused = spacesForDate.filter((s) => s.todayBookings <= 3 && !s.fullyBooked);
    const pool = underused.length ? underused : spacesForDate;
    const pick = pool[Math.floor(Math.random() * pool.length)] ?? spacesForDate[0];
    if (!pick) return;
    openDetail(pick.id);
    setSurprise(`✨ Try ${pick.name}.`);
    window.setTimeout(() => setSurprise(null), 4000);
  };

  const handleBook = (space: Space) => openBooking(space, selectedDate);

  return (
    <div style={{ display: "flex", flexDirection: "column", minHeight: "calc(100vh - 65px)" }}>
      {/* Toolbar */}
      <section
        style={{
          borderBottom: "1px solid var(--line-soft)",
          background: "rgba(251,248,254,0.92)",
          backdropFilter: "blur(8px)",
          position: "sticky",
          top: 65,
          zIndex: 30,
        }}
      >
        <div className="container" style={{ padding: "14px 32px" }}>
          <div className="row" style={{ justifyContent: "space-between", flexWrap: "wrap", gap: 12 }}>
            <div className="row gap-3">
              <span className="label" style={{ fontSize: 11 }}>
                {isToday
                  ? "Today"
                  : selectedDate.toLocaleDateString("en", { weekday: "long" })}{" "}
                · {selectedDate.toLocaleDateString("en", { month: "short", day: "numeric" })}
              </span>
              <span
                className="h-mono"
                style={{ fontSize: 11, color: "var(--ink-mute)", letterSpacing: "0.12em" }}
              >
                {filtered.length} OF {allSpaces.length} SPACES
              </span>
            </div>
            <div className="row gap-2">
              <div className="row" style={{ gap: 0, padding: 3, background: "var(--paper-3)", borderRadius: 9 }}>
                <button
                  onClick={() => setView("split")}
                  className={view === "split" ? "view-tab active" : "view-tab"}
                >
                  <svg width="14" height="14" viewBox="0 0 16 16" fill="none" stroke="currentColor" strokeWidth="1.5">
                    <rect x="2" y="2" width="5" height="12" rx="1" />
                    <rect x="9" y="2" width="5" height="12" rx="1" />
                  </svg>
                  Split
                </button>
                <button
                  onClick={() => setView("schedule")}
                  className={view === "schedule" ? "view-tab active" : "view-tab"}
                >
                  <svg width="14" height="14" viewBox="0 0 16 16" fill="none" stroke="currentColor" strokeWidth="1.5">
                    <rect x="2" y="3" width="12" height="10" rx="1" />
                    <line x1="2" y1="6" x2="14" y2="6" />
                    <line x1="6" y1="3" x2="6" y2="13" />
                  </svg>
                  Schedule
                </button>
              </div>
              <button
                className="btn btn-orange"
                style={{ padding: "8px 14px", fontSize: 13 }}
                onClick={handleSurprise}
              >
                <svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.5" strokeLinecap="round">
                  <path d="m12 3 2.5 6L21 10l-5 4.5L17.5 21 12 17.5 6.5 21 8 14.5 3 10l6.5-1z" />
                </svg>
                Surprise me
              </button>
              <button
                className="btn btn-ghost"
                style={{ padding: "8px 14px", fontSize: 13, position: "relative" }}
                onClick={() => setFilterOpen(true)}
              >
                <svg width="14" height="14" viewBox="0 0 16 16" fill="none" stroke="currentColor" strokeWidth="1.5">
                  <line x1="2" y1="4" x2="14" y2="4" />
                  <line x1="4" y1="8" x2="12" y2="8" />
                  <line x1="6" y1="12" x2="10" y2="12" />
                </svg>
                Filters
                {activeFilterCount > 0 && (
                  <span
                    style={{
                      marginLeft: 4,
                      background: "var(--indigo)",
                      color: "#fff",
                      borderRadius: 999,
                      padding: "0 7px",
                      fontSize: 11,
                      fontWeight: 700,
                    }}
                  >
                    {activeFilterCount}
                  </span>
                )}
              </button>
            </div>
          </div>
        </div>
        {/* Date strip */}
        <div className="date-strip-wrap">
          <div className="container" style={{ padding: "0 32px 14px" }}>
            <div className="date-strip" role="tablist" aria-label="Pick a date">
              {dateWindow.map((d) => {
                const dKey = localIsoDate(d);
                const sel = dKey === localIsoDate(selectedDate);
                const today = dKey === localIsoDate(now);
                return (
                  <button
                    key={d.toISOString()}
                    role="tab"
                    aria-selected={sel}
                    className={`date-pill ${sel ? "active" : ""}`}
                    onClick={() => setSelectedDate(d)}
                  >
                    <span
                      className="h-mono"
                      style={{ fontSize: 9, letterSpacing: "0.16em", opacity: 0.75 }}
                    >
                      {d.toLocaleDateString("en", { weekday: "short" }).toUpperCase()}
                    </span>
                    <span
                      className="h-display"
                      style={{ fontSize: 18, lineHeight: 1, marginTop: 2 }}
                    >
                      {d.getDate()}
                    </span>
                    {today && <span className="date-today-dot" />}
                  </button>
                );
              })}
            </div>
          </div>
        </div>
      </section>

      <div className="container" style={{ padding: "20px 32px 48px", flex: 1 }}>
        {view === "split" ? (
          <div className={`listings-split ${detailOpen ? "is-split" : ""}`}>
            <div className={`cards-pane ${detailOpen ? "is-narrow" : ""}`}>
              {filtered.length === 0 && (
                <div className="card" style={{ padding: 40, textAlign: "center" }}>
                  <p className="h-headline" style={{ fontSize: 20, margin: 0 }}>
                    Nothing matches.
                  </p>
                  <p style={{ color: "var(--ink-mute)" }}>Loosen a filter or hit Surprise me.</p>
                </div>
              )}
              {filtered.map((s) => (
                <SpaceCard
                  key={s.id}
                  space={s}
                  isToday={isToday}
                  currentHour={currentHour}
                  refHour={refHour}
                  selected={selectedId === s.id}
                  onOpen={() => openDetail(s.id)}
                  onBook={() => handleBook(s)}
                />
              ))}
            </div>

            <div className={`detail-pane ${detailOpen ? "is-open" : ""}`} aria-hidden={!detailOpen}>
              {selected && (
                <div style={{ position: "sticky", top: 200 }}>
                  <button onClick={closeDetail} className="detail-close" aria-label="Close detail panel">
                    <svg width="14" height="14" viewBox="0 0 16 16" fill="none" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round">
                      <line x1="4" y1="4" x2="12" y2="12" />
                      <line x1="12" y1="4" x2="4" y2="12" />
                    </svg>
                  </button>
                  <DetailPanel
                    space={selected}
                    currentHour={refHour}
                    isToday={isToday}
                    selectedDate={selectedDate}
                    onBook={() => handleBook(selected)}
                    onNotify={handleNotify}
                    isNotified={notified.has(selected.id)}
                  />
                </div>
              )}
            </div>
          </div>
        ) : (
          <ScheduleView
            spaces={filtered}
            currentHour={isToday ? currentHour : -1}
            selectedId={selectedId}
            onSelect={openDetail}
          />
        )}
      </div>

      <FilterSidebar
        open={filterOpen}
        onClose={() => setFilterOpen(false)}
        filters={filters}
        setFilters={setFilters}
        maxCapacity={maxCapacity}
        vibes={VIBES}
        count={filtered.length}
        buildingNames={buildingNames}
      />

      {surprise && (
        <div className="toast" style={{ background: "linear-gradient(135deg,#4b52a7,#ff823c)" }}>
          {surprise}
        </div>
      )}

      <style>{`
        .listings-split {
          display: grid;
          grid-template-columns: minmax(0, 1fr) minmax(0, 0fr);
          gap: 0px;
          align-items: start;
          transition: grid-template-columns 480ms cubic-bezier(.22,.9,.32,1), gap 480ms cubic-bezier(.22,.9,.32,1);
        }
        .listings-split.is-split {
          grid-template-columns: minmax(0, 1fr) minmax(0, 1.05fr);
          gap: 24px;
        }
        .cards-pane {
          display: grid;
          grid-template-columns: 1fr 1fr;
          gap: 12px;
          transition: grid-template-columns 480ms cubic-bezier(.22,.9,.32,1);
        }
        .cards-pane.is-narrow {
          grid-template-columns: 1fr;
        }
        .date-strip-wrap { border-top: 1px solid var(--line-soft); }
        .date-strip { display: flex; gap: 6px; overflow-x: auto; padding: 12px 0 2px; scrollbar-width: thin; }
        .date-strip::-webkit-scrollbar { height: 4px; }
        .date-pill {
          flex-shrink: 0; min-width: 56px; padding: 8px 12px; border-radius: 12px;
          display: flex; flex-direction: column; align-items: center; gap: 2px;
          background: transparent; border: 1px solid transparent; color: var(--ink-soft);
          cursor: pointer; transition: all .15s; position: relative;
        }
        .date-pill:hover { background: var(--paper-2); }
        .date-pill.active { background: var(--indigo); color: #fff; border-color: var(--indigo); }
        .date-pill.active .h-display { color: #fff; }
        .date-today-dot {
          position: absolute; bottom: 4px; left: 50%; transform: translateX(-50%);
          width: 4px; height: 4px; border-radius: 999px; background: var(--orange);
        }
        .detail-pane {
          min-width: 0;
          opacity: 0;
          transform: translateX(40px);
          pointer-events: none;
          transition: opacity 380ms ease 80ms, transform 480ms cubic-bezier(.22,.9,.32,1) 60ms;
          position: relative;
        }
        .detail-pane.is-open {
          opacity: 1;
          transform: translateX(0);
          pointer-events: auto;
        }
        .detail-close {
          position: absolute;
          top: 14px; right: 14px;
          z-index: 5;
          width: 32px; height: 32px;
          border-radius: 999px;
          background: rgba(255,255,255,0.92);
          backdrop-filter: blur(6px);
          color: var(--ink-soft);
          display: grid; place-items: center;
          box-shadow: 0 2px 8px rgba(27,27,31,0.15);
          border: 0;
          cursor: pointer;
          transition: transform .15s, background .15s;
        }
        .detail-close:hover { transform: scale(1.08); background: #fff; color: var(--ink); }
        .view-tab { display: inline-flex; align-items: center; gap: 6px; padding: 6px 12px; border-radius: 7px; font-size: 13px; font-weight: 500; color: var(--ink-soft); cursor: pointer; border: 0; background: transparent; }
        .view-tab.active { background: #fff; color: var(--indigo); box-shadow: var(--shadow-sm); }
        @media (max-width: 1100px) {
          .cards-pane, .cards-pane.is-narrow { grid-template-columns: 1fr; }
          .listings-split.is-split { grid-template-columns: 1fr; }
          .detail-pane.is-open { position: fixed; inset: 0; z-index: 100; background: var(--paper); overflow-y: auto; }
        }
      `}</style>
    </div>
  );
}

// ---------- Space card ----------
interface SpaceCardProps {
  space: ResolvedSpace;
  isToday: boolean;
  currentHour: number;
  refHour: number;
  selected: boolean;
  onOpen: () => void;
  onBook: () => void;
}

function SpaceCard({ space: s, isToday, currentHour, refHour, selected, onOpen, onBook }: SpaceCardProps) {
  const status = isToday
    ? getStatus(s, currentHour)
    : s.fullyBooked
      ? { label: "Fully booked", cls: "pill-red" }
      : { label: "Available", cls: "pill-green" };
  return (
    <article
      className="card"
      style={{
        padding: 14,
        display: "flex",
        gap: 14,
        cursor: "pointer",
        borderColor: selected ? "var(--indigo)" : undefined,
        boxShadow: selected ? "0 0 0 3px rgba(75,82,167,0.15)" : undefined,
      }}
      onClick={onOpen}
    >
      <div
        style={{
          flexShrink: 0,
          background: "linear-gradient(180deg,#f6f2f8,#fff)",
          border: "1px solid var(--line-soft)",
          borderRadius: 10,
          padding: 6,
          display: "grid",
          placeItems: "center",
          width: 108,
        }}
      >
        <FloorPlanSketch space={s} width={94} height={70} />
      </div>
      <div className="grow" style={{ minWidth: 0 }}>
        <div className="row" style={{ justifyContent: "space-between", alignItems: "flex-start", gap: 8 }}>
          <div style={{ minWidth: 0 }}>
            <span className={`pill ${status.cls}`} style={{ marginBottom: 4 }}>
              <span className="dot" />
              {status.label}
            </span>
            <h3 className="h-headline" style={{ fontSize: 17, margin: "4px 0 0" }}>
              {s.name}
            </h3>
            <p
              className="h-mono"
              style={{ fontSize: 10, color: "var(--ink-mute)", margin: "4px 0 0", letterSpacing: "0.08em" }}
            >
              {s.building.toUpperCase()} · {s.area}M² · {s.seats} SEATS
            </p>
          </div>
          <div style={{ textAlign: "right", flexShrink: 0 }}>
            <div className="h-display" style={{ fontSize: 18 }}>
              {s.price === 0 ? "Free" : `$${s.price}`}
            </div>
            {s.price > 0 && (
              <div className="h-mono" style={{ fontSize: 9, color: "var(--ink-mute)" }}>
                /HR
              </div>
            )}
          </div>
        </div>
        <div className="row" style={{ justifyContent: "space-between", gap: 12, marginTop: 8 }}>
          <Sparkline booked={s.booked} startHour={refHour} hours={12} width={88} height={18} />
          {s.fullyBooked ? (
            <span className="h-mono" style={{ fontSize: 11, color: "var(--ink-mute)" }}>
              FULLY BOOKED
            </span>
          ) : (
            <button
              className="btn btn-primary"
              style={{ padding: "6px 12px", fontSize: 12 }}
              onClick={(e) => {
                e.stopPropagation();
                onBook();
              }}
            >
              Book →
            </button>
          )}
        </div>
      </div>
    </article>
  );
}

// ---------- Filter sidebar ----------
interface FilterSidebarProps {
  open: boolean;
  onClose: () => void;
  filters: Filters;
  setFilters: (f: Filters) => void;
  maxCapacity: number;
  vibes: Vibe[];
  count: number;
  buildingNames: string[];
}

function FilterSidebar({ open, onClose, filters, setFilters, maxCapacity, vibes, count, buildingNames }: FilterSidebarProps) {
  const update = <K extends keyof Filters>(k: K, v: Filters[K]) => setFilters({ ...filters, [k]: v });
  const toggleVibe = (set: Set<VibeId>, id: VibeId): Set<VibeId> => {
    const n = new Set(set);
    if (n.has(id)) n.delete(id);
    else n.add(id);
    return n;
  };
  const toggleBuilding = (set: Set<string>, id: string): Set<string> => {
    const n = new Set(set);
    if (n.has(id)) n.delete(id);
    else n.add(id);
    return n;
  };

  const types: { id: SpaceTypeFilter; l: string }[] = [
    { id: "all", l: "All" },
    { id: "lab", l: "Labs" },
    { id: "pod", l: "Pods" },
    { id: "open", l: "Open rooms" },
    { id: "studio", l: "Studios" },
  ];
  const prices: { id: PriceFilter; l: string }[] = [
    { id: "any", l: "Any" },
    { id: "free", l: "Free" },
    { id: "cheap", l: "≤ $10" },
    { id: "mid", l: "$10–20" },
    { id: "high", l: "$20+" },
  ];

  return (
    <>
      {open && (
        <div
          onClick={onClose}
          style={{
            position: "fixed",
            inset: 0,
            background: "rgba(27,27,31,0.4)",
            zIndex: 80,
            animation: "fade .2s",
          }}
        />
      )}
      <aside className={`filter-sidebar ${open ? "is-open" : ""}`} aria-hidden={!open}>
        <header
          className="row"
          style={{ justifyContent: "space-between", padding: "20px 24px", borderBottom: "1px solid var(--line-soft)" }}
        >
          <div>
            <span className="kicker">Filters</span>
            <div className="h-headline" style={{ fontSize: 22, marginTop: 4 }}>
              Tune your search
            </div>
          </div>
          <button onClick={onClose} aria-label="Close" style={{ fontSize: 22, color: "var(--ink-mute)" }}>
            ✕
          </button>
        </header>

        <div className="filter-body">
          <FilterSection title="Accommodate" subtitle="Number of people">
            <div className="row" style={{ justifyContent: "space-between" }}>
              <span className="h-display" style={{ fontSize: 32 }}>
                {filters.minCapacity}
              </span>
              <span
                className="h-mono"
                style={{ fontSize: 11, color: "var(--ink-mute)", letterSpacing: "0.12em" }}
              >
                OF {maxCapacity} MAX
              </span>
            </div>
            <input
              type="range"
              min={1}
              max={maxCapacity}
              value={filters.minCapacity}
              onChange={(e) => update("minCapacity", Number(e.target.value))}
              style={{ width: "100%", accentColor: "var(--indigo)", marginTop: 8 }}
              aria-label="Minimum capacity"
            />
            <div className="row" style={{ justifyContent: "space-between" }}>
              <span className="h-mono" style={{ fontSize: 10, color: "var(--ink-mute)" }}>
                1
              </span>
              <span className="h-mono" style={{ fontSize: 10, color: "var(--ink-mute)" }}>
                {maxCapacity}
              </span>
            </div>
          </FilterSection>

          <FilterSection title="Space type">
            <div style={{ display: "grid", gridTemplateColumns: "1fr 1fr", gap: 8 }}>
              {types.map((t) => (
                <button
                  key={t.id}
                  className={`chip ${filters.type === t.id ? "active" : ""}`}
                  onClick={() => update("type", t.id)}
                  style={{ width: "100%", justifyContent: "center" }}
                >
                  {t.l}
                </button>
              ))}
            </div>
          </FilterSection>

          <FilterSection title="Vibe" subtitle="Pick any">
            <div className="row" style={{ flexWrap: "wrap", gap: 6 }}>
              {vibes.map((v) => (
                <button
                  key={v.id}
                  className={`chip ${filters.vibes.has(v.id) ? "active" : ""}`}
                  onClick={() => update("vibes", toggleVibe(filters.vibes, v.id))}
                >
                  <span style={{ fontSize: 12 }}>{v.icon}</span>
                  {v.label}
                </button>
              ))}
            </div>
          </FilterSection>

          <FilterSection title="Price" subtitle="Per hour">
            <div className="row" style={{ gap: 6, flexWrap: "wrap" }}>
              {prices.map((p) => (
                <button
                  key={p.id}
                  className={`chip ${filters.price === p.id ? "active" : ""}`}
                  onClick={() => update("price", p.id)}
                >
                  {p.l}
                </button>
              ))}
            </div>
          </FilterSection>

          <FilterSection title="Building">
            <div className="col gap-2">
              {buildingNames.map((b) => (
                <label
                  key={b}
                  className="row"
                  style={{ gap: 10, padding: "6px 4px", cursor: "pointer" }}
                >
                  <input
                    type="checkbox"
                    checked={filters.buildings.has(b)}
                    onChange={() => update("buildings", toggleBuilding(filters.buildings, b))}
                    style={{ accentColor: "var(--indigo)" }}
                  />
                  <span style={{ fontSize: 14 }}>{b}</span>
                </label>
              ))}
            </div>
          </FilterSection>

          <FilterSection title="Availability">
            <label className="row" style={{ gap: 10, padding: "6px 4px", cursor: "pointer" }}>
              <input
                type="checkbox"
                checked={filters.openNow}
                onChange={(e) => update("openNow", e.target.checked)}
                style={{ accentColor: "var(--indigo)" }}
              />
              <span style={{ fontSize: 14 }}>Available now</span>
            </label>
            <label className="row" style={{ gap: 10, padding: "6px 4px", cursor: "pointer" }}>
              <input
                type="checkbox"
                checked={filters.hideFull}
                onChange={(e) => update("hideFull", e.target.checked)}
                style={{ accentColor: "var(--indigo)" }}
              />
              <span style={{ fontSize: 14 }}>Hide fully booked</span>
            </label>
          </FilterSection>
        </div>

        <footer
          className="row"
          style={{ padding: 16, gap: 8, borderTop: "1px solid var(--line-soft)", background: "#fff" }}
        >
          <button
            className="btn btn-ghost"
            onClick={() =>
              setFilters({
                minCapacity: 1,
                type: "all",
                vibes: new Set<VibeId>(),
                price: "any",
                buildings: new Set<string>(),
                openNow: false,
                hideFull: false,
              })
            }
          >
            Reset
          </button>
          <button className="btn btn-primary grow" onClick={onClose}>
            Show {count} {count === 1 ? "space" : "spaces"}
          </button>
        </footer>
      </aside>
      <style>{`
        .filter-sidebar { position: fixed; top: 0; left: 0; width: 380px; max-width: 92vw; height: 100vh; background: var(--paper); z-index: 90; transform: translateX(-100%); transition: transform .28s cubic-bezier(.22,.9,.32,1); box-shadow: 20px 0 40px rgba(27,27,31,0.15); display: flex; flex-direction: column; }
        .filter-sidebar.is-open { transform: translateX(0); }
        .filter-body { flex: 1; overflow-y: auto; padding: 8px 24px 16px; }
        @keyframes fade { from { opacity: 0 } to { opacity: 1 } }
      `}</style>
    </>
  );
}

interface FilterSectionProps {
  title: string;
  subtitle?: string;
  children: React.ReactNode;
}

function FilterSection({ title, subtitle, children }: FilterSectionProps) {
  return (
    <section style={{ padding: "20px 0", borderBottom: "1px solid var(--line-soft)" }}>
      <div
        className="h-mono"
        style={{
          fontSize: 11,
          letterSpacing: "0.18em",
          color: "var(--ink-soft)",
          textTransform: "uppercase",
          fontWeight: 600,
        }}
      >
        {title}
      </div>
      {subtitle && (
        <div style={{ fontSize: 13, color: "var(--ink-mute)", marginTop: 2, marginBottom: 12 }}>
          {subtitle}
        </div>
      )}
      {!subtitle && <div style={{ marginTop: 12 }} />}
      {children}
    </section>
  );
}

// ---------- Schedule view ----------
interface ScheduleViewProps {
  spaces: ResolvedSpace[];
  currentHour: number;
  selectedId: string | null;
  onSelect: (id: string) => void;
}

function ScheduleView({ spaces, currentHour, selectedId, onSelect }: ScheduleViewProps) {
  const HOURS = Array.from({ length: 14 }, (_, i) => 8 + i);
  const colW = 64;
  return (
    <div className="card" style={{ padding: 0, overflow: "hidden" }}>
      <div style={{ overflowX: "auto" }}>
        <div style={{ minWidth: 200 + HOURS.length * colW }}>
          <div
            style={{
              display: "grid",
              gridTemplateColumns: `200px repeat(${HOURS.length}, ${colW}px)`,
              position: "sticky",
              top: 0,
              background: "var(--paper-2)",
              borderBottom: "1px solid var(--line-soft)",
              zIndex: 2,
            }}
          >
            <div style={{ padding: "14px 16px" }} className="label">
              Space · {spaces.length}
            </div>
            {HOURS.map((h) => (
              <div
                key={h}
                style={{
                  padding: "14px 0",
                  textAlign: "center",
                  borderLeft: "1px solid var(--line-soft)",
                  position: "relative",
                }}
              >
                <span
                  className="h-mono"
                  style={{
                    fontSize: 10,
                    color: h === currentHour ? "var(--indigo)" : "var(--ink-mute)",
                    fontWeight: h === currentHour ? 700 : 500,
                    letterSpacing: "0.06em",
                  }}
                >
                  {formatHour(h)}
                </span>
                {h === currentHour && (
                  <span
                    style={{
                      position: "absolute",
                      top: 0,
                      left: "50%",
                      width: 1,
                      height: "100%",
                      background: "var(--indigo)",
                      opacity: 0.3,
                    }}
                  />
                )}
              </div>
            ))}
          </div>

          {spaces.map((s) => {
            const isSel = selectedId === s.id;
            return (
              <div
                key={s.id}
                style={{
                  display: "grid",
                  gridTemplateColumns: `200px repeat(${HOURS.length}, ${colW}px)`,
                  borderBottom: "1px solid var(--line-soft)",
                  background: isSel ? "rgba(75,82,167,0.05)" : "#fff",
                  cursor: "pointer",
                }}
                onClick={() => onSelect(s.id)}
              >
                <div
                  style={{
                    padding: "12px 16px",
                    display: "flex",
                    flexDirection: "column",
                    justifyContent: "center",
                    borderRight: "1px solid var(--line-soft)",
                  }}
                >
                  <div style={{ fontSize: 13, fontWeight: 600, lineHeight: 1.2 }}>{s.name}</div>
                  <div
                    className="h-mono"
                    style={{
                      fontSize: 10,
                      color: "var(--ink-mute)",
                      letterSpacing: "0.08em",
                      marginTop: 2,
                    }}
                  >
                    {s.building.toUpperCase()} · {s.seats} SEATS
                  </div>
                </div>
                <div style={{ position: "relative", gridColumn: `2 / span ${HOURS.length}`, height: 56 }}>
                  {HOURS.map((h, i) => (
                    <div
                      key={h}
                      style={{
                        position: "absolute",
                        left: i * colW,
                        top: 0,
                        bottom: 0,
                        width: colW,
                        borderLeft: i === 0 ? "none" : "1px solid var(--line-soft)",
                        background: h === currentHour ? "rgba(75,82,167,0.04)" : "transparent",
                      }}
                    />
                  ))}
                  {currentHour >= 8 && currentHour <= 21 && (
                    <div
                      style={{
                        position: "absolute",
                        left: (currentHour - 8) * colW,
                        top: 0,
                        bottom: 0,
                        width: 2,
                        background: "var(--indigo)",
                        zIndex: 2,
                      }}
                    >
                      <div
                        style={{
                          position: "absolute",
                          top: -3,
                          left: -4,
                          width: 10,
                          height: 10,
                          borderRadius: 999,
                          background: "var(--indigo)",
                        }}
                      />
                    </div>
                  )}
                  {(s.events || []).map((ev: SpaceEvent, i: number) => {
                    const left = (ev.startH - 8) * colW + 2;
                    const width = (ev.endH - ev.startH) * colW - 4;
                    return (
                      <div
                        key={i}
                        title={`${ev.title} · ${ev.host}`}
                        style={{
                          position: "absolute",
                          left,
                          top: 6,
                          width,
                          height: 44,
                          background:
                            "linear-gradient(135deg, rgba(75,82,167,0.92), rgba(100,107,194,0.92))",
                          borderRadius: 6,
                          padding: "6px 8px",
                          color: "#fff",
                          overflow: "hidden",
                          boxShadow: "0 2px 6px rgba(75,82,167,0.25)",
                        }}
                      >
                        <div
                          style={{
                            fontSize: 11,
                            fontWeight: 600,
                            lineHeight: 1.15,
                            overflow: "hidden",
                            textOverflow: "ellipsis",
                            whiteSpace: "nowrap",
                          }}
                        >
                          {ev.title}
                        </div>
                        <div
                          className="h-mono"
                          style={{
                            fontSize: 9,
                            opacity: 0.85,
                            letterSpacing: "0.06em",
                            marginTop: 1,
                            overflow: "hidden",
                            textOverflow: "ellipsis",
                            whiteSpace: "nowrap",
                          }}
                        >
                          {ev.host.toUpperCase()}
                        </div>
                      </div>
                    );
                  })}
                  {s.booked
                    .filter((h: number) => !(s.events || []).some((e) => h >= e.startH && h < e.endH))
                    .map((h: number) => (
                      <div
                        key={`b${h}`}
                        style={{
                          position: "absolute",
                          left: (h - 8) * colW + 2,
                          top: 6,
                          width: colW - 4,
                          height: 44,
                          background:
                            "repeating-linear-gradient(45deg, rgba(118,118,131,0.18), rgba(118,118,131,0.18) 4px, transparent 4px, transparent 8px)",
                          borderRadius: 6,
                        }}
                        title="Booked"
                      />
                    ))}
                </div>
              </div>
            );
          })}
          {spaces.length === 0 && (
            <div style={{ padding: 40, textAlign: "center", color: "var(--ink-mute)" }}>
              No spaces match these filters.
            </div>
          )}
        </div>
      </div>
    </div>
  );
}

// ---------- Detail panel ----------
interface DetailPanelProps {
  space: ResolvedSpace;
  currentHour: number;
  isToday: boolean;
  selectedDate: Date;
  onBook: () => void;
  onNotify: (id: string) => void;
  isNotified: boolean;
}

function DetailPanel({ space, currentHour, isToday, selectedDate, onBook, onNotify, isNotified }: DetailPanelProps) {
  const [photoIdx, setPhotoIdx] = useState(0);
  useEffect(() => setPhotoIdx(0), [space.id]);

  const status = isToday
    ? getStatus(space, currentHour)
    : space.fullyBooked
      ? { label: "Fully booked", cls: "pill-red" }
      : { label: "Open this day", cls: "pill-green" };
  const photos: PhotoVariant[] = ["a", "b", "c", "d"];

  const startHr = isToday ? currentHour : 8;
  const HOURS = Array.from({ length: isToday ? 12 : 13 }, (_, i) => startHr + i);

  return (
    <div className="card" style={{ padding: 0, overflow: "hidden" }}>
      <div
        style={{
          position: "relative",
          aspectRatio: "16/10",
          background: "var(--paper-3)",
          overflow: "hidden",
        }}
      >
        <PhotoFrame space={space} variant={photos[photoIdx]} />
        <div
          style={{
            position: "absolute",
            top: 14,
            left: 14,
            right: 14,
            display: "flex",
            justifyContent: "space-between",
            pointerEvents: "none",
          }}
        >
          <span className={`pill ${status.cls}`} style={{ background: "#fff" }}>
            <span className="dot" />
            {status.label}
          </span>
          <span
            className="h-mono"
            style={{
              fontSize: 10,
              background: "rgba(255,255,255,0.95)",
              padding: "4px 8px",
              borderRadius: 999,
              color: "var(--ink-soft)",
              letterSpacing: "0.1em",
            }}
          >
            {photoIdx + 1} / {photos.length}
          </span>
        </div>
        <div
          style={{
            position: "absolute",
            bottom: 14,
            left: 14,
            right: 14,
            display: "flex",
            gap: 6,
            justifyContent: "center",
          }}
        >
          {photos.map((_, i) => (
            <button
              key={i}
              onClick={() => setPhotoIdx(i)}
              aria-label={`Photo ${i + 1}`}
              style={{
                width: i === photoIdx ? 24 : 8,
                height: 8,
                borderRadius: 999,
                background: i === photoIdx ? "#fff" : "rgba(255,255,255,0.55)",
                transition: "all .2s",
                cursor: "pointer",
                border: 0,
              }}
            />
          ))}
        </div>
        <button
          onClick={() => setPhotoIdx((photoIdx - 1 + photos.length) % photos.length)}
          className="carousel-nav"
          style={{ left: 8 }}
          aria-label="Previous photo"
        >
          ‹
        </button>
        <button
          onClick={() => setPhotoIdx((photoIdx + 1) % photos.length)}
          className="carousel-nav"
          style={{ right: 8 }}
          aria-label="Next photo"
        >
          ›
        </button>
      </div>

      <div style={{ padding: 24, maxHeight: "calc(100vh - 320px)", overflowY: "auto" }}>
        <div className="row" style={{ justifyContent: "space-between", alignItems: "flex-start", gap: 12 }}>
          <div style={{ minWidth: 0 }}>
            <span
              className="h-mono"
              style={{ fontSize: 10, letterSpacing: "0.18em", color: "var(--ink-mute)" }}
            >
              {space.type.toUpperCase()} · {space.building.toUpperCase()}
            </span>
            <h2 className="h-display" style={{ fontSize: 30, margin: "4px 0 0" }}>
              {space.name}
            </h2>
          </div>
          <div style={{ textAlign: "right" }}>
            <div className="h-display" style={{ fontSize: 26 }}>
              {space.price === 0 ? "Free" : `$${space.price}`}
            </div>
            {space.price > 0 && (
              <div className="h-mono" style={{ fontSize: 10, color: "var(--ink-mute)" }}>
                /HR
              </div>
            )}
          </div>
        </div>

        <div
          className="row"
          style={{
            gap: 20,
            marginTop: 16,
            padding: "14px 0",
            borderTop: "1px solid var(--line-soft)",
            borderBottom: "1px solid var(--line-soft)",
          }}
        >
          <Stat2 label="seats" value={space.seats} />
          <Stat2 label="area" value={`${space.area} m²`} />
          <Stat2 label="floor" value={space.floor < 0 ? "B1" : space.floor} />
          <Stat2 label="viewing" value={space.viewing} dot />
        </div>

        <p style={{ fontSize: 15, lineHeight: 1.6, color: "var(--ink-soft)", margin: "20px 0 0" }}>
          {space.description || space.blurb}
        </p>

        {space.amenities && space.amenities.length > 0 && (
          <div style={{ marginTop: 24 }}>
            <h3 className="h-mono" style={{ fontSize: 11, letterSpacing: "0.18em", color: "var(--ink-soft)" }}>AMENITIES</h3>
            <div style={{ display: "flex", flexWrap: "wrap", gap: 6, marginTop: 8 }}>
              {space.amenities.map(a => (
                <span key={a} className="pill pill-indigo" style={{ fontSize: 12 }}>{a}</span>
              ))}
            </div>
          </div>
        )}

        {space.rules && space.rules.length > 0 && (
          <div style={{ marginTop: 24 }}>
            <h3 className="h-mono" style={{ fontSize: 11, letterSpacing: "0.18em", color: "var(--ink-soft)" }}>HOUSE RULES</h3>
            <ul style={{ margin: "8px 0 0", padding: 0, listStyle: "none", fontSize: 14, color: "var(--ink-soft)" }}>
              {space.rules.map(r => (
                <li key={r} style={{ display: "flex", gap: 8, marginBottom: 4 }}>
                  <span style={{ color: "var(--orange)" }}>•</span>
                  {r}
                </li>
              ))}
            </ul>
          </div>
        )}

        <h3
          className="h-mono"
          style={{ fontSize: 11, letterSpacing: "0.18em", color: "var(--ink-soft)", marginTop: 24 }}
        >
          {isToday
            ? "TODAY'S SCHEDULE"
            : `SCHEDULE · ${selectedDate
                .toLocaleDateString("en", { weekday: "short", month: "short", day: "numeric" })
                .toUpperCase()}`}
        </h3>
        <div
          style={{
            marginTop: 8,
            display: "grid",
            gridTemplateColumns: `repeat(${HOURS.length}, 1fr)`,
            gap: 2,
          }}
        >
          {HOURS.map((h) => {
            const ev = (space.events || []).find((e) => h >= e.startH && h < e.endH);
            const booked = space.booked.includes(h) && !ev;
            return (
              <div
                key={h}
                title={
                  ev
                    ? `${formatHour(h)} · ${ev.title}`
                    : booked
                      ? `${formatHour(h)} · booked`
                      : `${formatHour(h)} · open`
                }
                style={{
                  height: 28,
                  borderRadius: 4,
                  background: ev
                    ? "var(--indigo)"
                    : booked
                      ? "repeating-linear-gradient(45deg, var(--paper-3), var(--paper-3) 3px, transparent 3px, transparent 6px)"
                      : "var(--green-pale)",
                  border: booked ? "1px solid var(--line-soft)" : "none",
                }}
              />
            );
          })}
        </div>
        <div className="row" style={{ justifyContent: "space-between", marginTop: 6 }}>
          <span className="h-mono" style={{ fontSize: 9, color: "var(--ink-mute)" }}>
            {formatHour(HOURS[0])}
          </span>
          <span className="h-mono" style={{ fontSize: 9, color: "var(--ink-mute)" }}>
            {formatHour(HOURS[HOURS.length - 1])}
          </span>
        </div>

        {(space.events || []).length > 0 && (
          <div className="col gap-2" style={{ marginTop: 14 }}>
            {(space.events || []).slice(0, 3).map((ev, i) => (
              <div
                key={i}
                className="row"
                style={{ gap: 10, padding: 10, background: "var(--paper-2)", borderRadius: 8 }}
              >
                <div
                  style={{
                    width: 4,
                    alignSelf: "stretch",
                    borderRadius: 999,
                    background: "var(--indigo)",
                  }}
                />
                <div className="grow">
                  <div style={{ fontSize: 13, fontWeight: 600 }}>{ev.title}</div>
                  <div
                    className="h-mono"
                    style={{ fontSize: 10, color: "var(--ink-mute)", letterSpacing: "0.08em" }}
                  >
                    {formatHour(ev.startH)}–{formatHour(ev.endH)} · {ev.host.toUpperCase()}
                  </div>
                </div>
              </div>
            ))}
          </div>
        )}

        {space.amenities && space.amenities.length > 0 && (
          <>
            <h3
              className="h-mono"
              style={{ fontSize: 11, letterSpacing: "0.18em", color: "var(--ink-soft)", marginTop: 28 }}
            >
              AMENITIES
            </h3>
            <div className="row" style={{ flexWrap: "wrap", gap: 6, marginTop: 10 }}>
              {space.amenities.map((a) => (
                <span key={a} className="pill pill-indigo" style={{ fontSize: 12 }}>
                  {a}
                </span>
              ))}
            </div>
          </>
        )}

        {space.rules && space.rules.length > 0 && (
          <>
            <h3
              className="h-mono"
              style={{ fontSize: 11, letterSpacing: "0.18em", color: "var(--ink-soft)", marginTop: 24 }}
            >
              HOUSE RULES
            </h3>
            <ul style={{ margin: "10px 0 0", padding: 0, listStyle: "none" }}>
              {space.rules.map((r) => (
                <li key={r} className="row" style={{ gap: 8, padding: "4px 0", fontSize: 14 }}>
                  <span style={{ color: "var(--orange)", fontWeight: 700 }}>·</span>
                  {r}
                </li>
              ))}
            </ul>
          </>
        )}

        <h3
          className="h-mono"
          style={{ fontSize: 11, letterSpacing: "0.18em", color: "var(--ink-soft)", marginTop: 24 }}
        >
          WHERE
        </h3>
        <div
          className="archgrid"
          style={{
            marginTop: 10,
            padding: 16,
            background: "var(--paper-2)",
            borderRadius: 10,
            position: "relative",
            overflow: "hidden",
          }}
        >
          <FloorPlanSketch space={space} width={300} height={140} />
          <div className="row" style={{ justifyContent: "space-between", marginTop: 10 }}>
            <div>
              <div style={{ fontSize: 14, fontWeight: 600 }}>{space.building}</div>
              <div
                className="h-mono"
                style={{ fontSize: 10, color: "var(--ink-mute)", letterSpacing: "0.08em" }}
              >
                FLOOR {space.floor} · ROOM {space.room}
              </div>
            </div>
            <button className="btn btn-ghost" style={{ padding: "6px 12px", fontSize: 12 }}>
              Open in map →
            </button>
          </div>
        </div>
      </div>

      <div
        className="row"
        style={{
          padding: 16,
          gap: 8,
          borderTop: "1px solid var(--line-soft)",
          background: "rgba(255,255,255,0.95)",
          backdropFilter: "blur(8px)",
          position: "sticky",
          bottom: 0,
        }}
      >
        <button className="btn btn-ghost" aria-label="Save">
          ♡
        </button>
        {space.fullyBooked ? (
          <button className="btn btn-ghost grow" onClick={() => onNotify(space.id)}>
            {isNotified ? "✓ We'll notify you" : "Notify me when free"}
          </button>
        ) : (
          <button className="btn btn-primary grow" onClick={onBook}>
            Pick a time · Book →
          </button>
        )}
      </div>

      <style>{`
        .carousel-nav { position: absolute; top: 50%; transform: translateY(-50%); width: 32px; height: 32px; border-radius: 999px; background: rgba(255,255,255,0.9); color: var(--ink); font-size: 18px; font-weight: 600; box-shadow: var(--shadow-sm); transition: all .15s; }
        .carousel-nav:hover { background: #fff; transform: translateY(-50%) scale(1.05); }
      `}</style>
    </div>
  );
}

interface Stat2Props {
  label: string;
  value: string | number;
  dot?: boolean;
}

function Stat2({ label, value, dot }: Stat2Props) {
  return (
    <div className="grow">
      <div className="h-display" style={{ fontSize: 18, lineHeight: 1 }}>
        {dot && typeof value === "number" && value > 0 && (
          <span
            style={{
              display: "inline-block",
              width: 6,
              height: 6,
              borderRadius: 999,
              background: "var(--green)",
              marginRight: 6,
              verticalAlign: "middle",
            }}
          />
        )}
        {value}
      </div>
      <div
        className="h-mono"
        style={{
          fontSize: 9,
          color: "var(--ink-mute)",
          letterSpacing: "0.16em",
          marginTop: 2,
          textTransform: "uppercase",
        }}
      >
        {label}
      </div>
    </div>
  );
}

type PhotoVariant = "a" | "b" | "c" | "d";

function PhotoFrame({ space, variant }: { space: Space; variant: PhotoVariant }) {
  const angles: Record<PhotoVariant, string> = {
    a: "translate(0,0) scale(1)",
    b: "translate(-10px,-8px) scale(1.08)",
    c: "translate(20px,5px) scale(0.95)",
    d: "translate(-5px,15px) scale(1.04)",
  };
  return (
    <div
      style={{
        position: "absolute",
        inset: 0,
        background: "linear-gradient(135deg,#f6f2f8 0%, #e8e4ee 100%)",
        display: "grid",
        placeItems: "center",
      }}
    >
      <div
        style={{
          transform: angles[variant],
          transition: "transform .4s ease",
          display: "grid",
          placeItems: "center",
        }}
      >
        <SpacePhoto space={space} width={420} height={260} />
      </div>
      <div style={{ position: "absolute", bottom: 10, left: 14 }}>
        <span
          className="h-mono"
          style={{ fontSize: 9, letterSpacing: "0.18em", color: "var(--ink-mute)" }}
        >
          VIEW {variant.toUpperCase()} · ELEVATION SKETCH
        </span>
      </div>
    </div>
  );
}
