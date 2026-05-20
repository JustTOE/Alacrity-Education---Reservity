import { useEffect, useMemo, useState } from "react";
import { useNavigate } from "react-router-dom";
import { formatHour } from "@/data/booking";
import { useApp } from "@/state/AppStore";
import type { Space } from "@/types";
import FloorPlanSketch from "@/components/viz/FloorPlanSketch";
import { useNow } from "@/hooks/useNow";
import { useSubmitReservation } from "@/hooks/useSubmitReservation";
import {
  emptyLocalDay,
  localIsoDate,
  projectUtcDaysToLocal,
  utcDatesForLocalRange,
} from "@/hooks/localTime";
import { spacesApi } from "@/api/spaces";
import { useQuery } from "@tanstack/react-query";
import { ApiError } from "@/api/client";
import { useAuth } from "@/auth/AuthContext";

export default function BookingFlow() {
  const { state, closeBooking, showToast } = useApp();
  const navigate = useNavigate();
  const auth = useAuth();
  const draft = state.bookingDraft;

  if (!draft) return null;
  const initialDate = draft.date ?? new Date();

  return (
    <BookingFlowModal
      space={draft.space}
      initialDate={initialDate}
      isAuthenticated={auth.status === "authenticated"}
      onCancel={closeBooking}
      onAfterConfirm={(reservationId, code) => {
        closeBooking();
        showToast(`Confirmed: ${code}`, "success");
        navigate(`/pass/${reservationId}`);
      }}
      onAuthRequired={() => {
        // Backend would 401 anyway, but we route earlier for a smoother UX.
        const slug = draft.space.slug ?? draft.space.id;
        closeBooking();
        navigate(`/login?returnTo=${encodeURIComponent(`/spaces/${slug}`)}`);
      }}
      onError={(msg) => showToast(msg, "error")}
    />
  );
}

interface BookingFlowModalProps {
  space: Space;
  initialDate: Date;
  isAuthenticated: boolean;
  onCancel: () => void;
  onAfterConfirm: (reservationId: string, code: string) => void;
  onAuthRequired: () => void;
  onError: (msg: string) => void;
}

function BookingFlowModal({
  space,
  initialDate,
  isAuthenticated,
  onCancel,
  onAfterConfirm,
  onAuthRequired,
  onError,
}: BookingFlowModalProps) {
  const { now } = useNow();
  // Week of LOCAL dates starting at the initial day. Iterating by adding 24h
  // is DST-safe in practice for a 7-day window because we only read Y/M/D and
  // discard the time part — across a DST boundary we may step by 23h or 25h
  // local, but the resulting `Date` still lands on the next calendar day.
  const week = useMemo(() => {
    const start = new Date(
      initialDate.getFullYear(),
      initialDate.getMonth(),
      initialDate.getDate(),
    );
    return Array.from({ length: 7 }, (_, i) => {
      const d = new Date(start);
      d.setDate(start.getDate() + i);
      return d;
    });
  }, [initialDate]);

  const HOURS = Array.from({ length: 14 }, (_, i) => 8 + i);

  const [selectedSlot, setSelectedSlot] = useState<{ dayIdx: number; hour: number } | null>(null);
  const [duration, setDuration] = useState<number>(2);
  const [recurring, setRecurring] = useState(false);

  // Fetch a UTC range wide enough to cover the local week (1-day pad each side).
  const slug = space.slug ?? space.id;
  const { from: fromStr, to: toStr } = useMemo(
    () => utcDatesForLocalRange(week[0], week[6]),
    [week],
  );
  const { data: range, isLoading: loadingRange } = useQuery({
    queryKey: ["availability:range", slug, fromStr, toStr],
    queryFn: () => spacesApi.availabilityRange(slug, fromStr, toStr),
    staleTime: 30_000,
  });

  const submit = useSubmitReservation();

  // Project the UTC days from the API onto LOCAL dates (the user's wall clock).
  const localDays = useMemo(() => {
    if (!range) return new Map<string, ReturnType<typeof emptyLocalDay>>();
    return projectUtcDaysToLocal(range.days);
  }, [range]);

  const schedules = useMemo(() => {
    return week.map((d) => {
      const key = localIsoDate(d);
      return localDays.get(key) ?? emptyLocalDay(key);
    });
  }, [localDays, week]);

  // Reset selection when the space changes (provider gives a new draft for a new space).
  useEffect(() => {
    setSelectedSlot(null);
    setDuration(2);
    setRecurring(false);
  }, [space.id]);

  // Close on Escape.
  useEffect(() => {
    const onKey = (e: KeyboardEvent) => {
      if (e.key === "Escape") onCancel();
    };
    window.addEventListener("keydown", onKey);
    return () => window.removeEventListener("keydown", onKey);
  }, [onCancel]);

  const isHourBooked = (dayIdx: number, h: number) => schedules[dayIdx].booked.has(h);
  const eventForCell = (dayIdx: number, h: number) =>
    schedules[dayIdx].events.find((e) => h >= e.startH && h < e.endH);

  const slotFits = (dayIdx: number, startH: number, dur: number) => {
    if (startH + dur > 22) return false;
    for (let i = 0; i < dur; i++) if (isHourBooked(dayIdx, startH + i)) return false;
    return true;
  };

  const startH = selectedSlot ? selectedSlot.hour : null;
  const endH = startH != null ? startH + duration : null;
  const selectedDate = selectedSlot ? week[selectedSlot.dayIdx] : null;
  const dayDateStr = selectedDate
    ? selectedDate.toLocaleDateString("en", { weekday: "long", month: "short", day: "numeric" })
    : null;
  const slotValid = selectedSlot ? slotFits(selectedSlot.dayIdx, selectedSlot.hour, duration) : false;

  async function handleConfirm() {
    if (!selectedSlot || startH == null || endH == null || !selectedDate) return;
    if (!isAuthenticated) {
      onAuthRequired();
      return;
    }

    // Treat the clicked hour as LOCAL — `toISOString()` converts to UTC for the wire.
    const startsAt = new Date(
      selectedDate.getFullYear(),
      selectedDate.getMonth(),
      selectedDate.getDate(),
      startH, 0, 0, 0,
    );
    const endsAt = new Date(
      selectedDate.getFullYear(),
      selectedDate.getMonth(),
      selectedDate.getDate(),
      endH, 0, 0, 0,
    );

    try {
      const resp = await submit.mutateAsync({
        slugOrId: slug,
        body: {
          startsAt: startsAt.toISOString(),
          endsAt: endsAt.toISOString(),
          recurring: recurring ? "weekly" : "none",
        },
      });
      // For instant-book spaces (the default), we get a reservationId immediately.
      // For PENDING (instant_book=false) flows, no reservationId yet — toast and bail.
      if (!resp.reservationId) {
        onError("Request submitted — waiting for owner approval.");
        onCancel();
        return;
      }
      onAfterConfirm(resp.reservationId, resp.id);
    } catch (err) {
      if (err instanceof ApiError) {
        if (err.status === 401) {
          onAuthRequired();
          return;
        }
        if (err.status === 409) {
          onError("That hour was just booked. Pick another.");
          return;
        }
        onError(err.message ?? "Couldn't confirm — try again.");
        return;
      }
      onError("Network error. Is the backend running?");
    }
  }

  return (
    <div role="dialog" aria-modal="true" className="bf-overlay" onClick={onCancel}>
      <div className="bf-modal card archgrid" onClick={(e) => e.stopPropagation()}>
        <header className="bf-header">
          <div>
            <span className="kicker">Pick a time</span>
            <h2 className="h-display" style={{ fontSize: 32, margin: "4px 0 4px" }}>
              {space.name}
            </h2>
            <p
              className="h-mono"
              style={{ fontSize: 11, color: "var(--ink-mute)", margin: 0, letterSpacing: "0.12em" }}
            >
              {space.building.toUpperCase()} · FLOOR {space.floor < 0 ? "B1" : space.floor} · {space.area}M² · {space.seats} SEATS
            </p>
          </div>
          <button onClick={onCancel} aria-label="Close" className="bf-close">
            ✕
          </button>
        </header>

        <div className="bf-body">
          <div className="bf-cal-wrap">
            <div className="row" style={{ justifyContent: "space-between", marginBottom: 8 }}>
              <div className="row" style={{ gap: 8, alignItems: "baseline" }}>
                <span className="label">
                  {week[0].toLocaleDateString("en", { month: "short", day: "numeric" })} –{" "}
                  {week[6].toLocaleDateString("en", { month: "short", day: "numeric" })}
                </span>
                <span
                  className="h-mono"
                  style={{ fontSize: 9, letterSpacing: "0.16em", color: "var(--ink-mute)" }}
                >
                  YOUR LOCAL TIME
                </span>
              </div>
              <div className="row gap-2" style={{ fontSize: 11 }}>
                <Legend swatch="var(--green-pale)" label="Open" />
                <Legend pattern label="Booked" />
                <Legend swatch="var(--indigo)" label="Selected" />
              </div>
            </div>

            {loadingRange ? (
              <div style={{ padding: "48px 0", textAlign: "center", color: "var(--ink-3)" }}>
                Loading schedule…
              </div>
            ) : (
              <div className="bf-cal">
                <div className="bf-cal-row bf-cal-headers">
                  <div className="bf-cal-corner" />
                  {week.map((d, i) => {
                    const isTodayCol = localIsoDate(d) === localIsoDate(now);
                    return (
                      <div key={i} className="bf-cal-dayhead">
                        <span
                          className="h-mono"
                          style={{ fontSize: 9, letterSpacing: "0.16em", color: "var(--ink-mute)" }}
                        >
                          {d.toLocaleDateString("en", { weekday: "short" }).toUpperCase()}
                        </span>
                        <span className="h-display" style={{ fontSize: 16 }}>
                          {d.getDate()}
                        </span>
                        {isTodayCol && <span className="bf-today">TODAY</span>}
                      </div>
                    );
                  })}
                </div>

                {HOURS.map((h) => (
                  <div key={h} className="bf-cal-row">
                    <div className="bf-cal-hour">
                      <span
                        className="h-mono"
                        style={{ fontSize: 10, color: "var(--ink-mute)", letterSpacing: "0.06em" }}
                      >
                        {formatHour(h)}
                      </span>
                    </div>
                    {week.map((d, dIdx) => {
                      const booked = isHourBooked(dIdx, h);
                      const ev = eventForCell(dIdx, h);
                      const sel =
                        selectedSlot &&
                        selectedSlot.dayIdx === dIdx &&
                        startH != null &&
                        endH != null &&
                        h >= startH &&
                        h < endH;
                      const todayLocal = localIsoDate(now);
                      const dLocal = localIsoDate(d);
                      const isPast =
                        dLocal < todayLocal ||
                        (dLocal === todayLocal && h < now.getHours());
                      let cls = "bf-cell";
                      if (sel) cls += " is-selected";
                      else if (booked) cls += " is-booked";
                      else if (isPast) cls += " is-past";
                      else cls += " is-open";
                      return (
                        <button
                          key={dIdx}
                          type="button"
                          className={cls}
                          disabled={booked || isPast}
                          title={
                            ev
                              ? `${ev.title} · ${ev.host}`
                              : booked
                                ? "Booked"
                                : isPast
                                  ? "Past"
                                  : `${formatHour(h)} · open`
                          }
                          onClick={() => setSelectedSlot({ dayIdx: dIdx, hour: h })}
                        >
                          {ev && h === ev.startH && <span className="bf-event-label">{ev.title}</span>}
                        </button>
                      );
                    })}
                  </div>
                ))}
              </div>
            )}
          </div>

          <aside className="bf-rail">
            <div className="bf-room-card">
              <FloorPlanSketch space={space} width={150} height={94} />
              <div style={{ marginTop: 10 }}>
                <div
                  className="h-mono"
                  style={{ fontSize: 10, letterSpacing: "0.16em", color: "var(--ink-mute)" }}
                >
                  {space.type.toUpperCase()}
                </div>
                <div style={{ fontSize: 14, fontWeight: 600, marginTop: 2 }}>{space.building}</div>
              </div>
            </div>

            {selectedSlot && startH != null && endH != null && selectedDate ? (
              <>
                <div className="bf-summary">
                  <span className="kicker">Selected</span>
                  <div className="h-display" style={{ fontSize: 26, marginTop: 4 }}>
                    {formatHour(startH)} → {formatHour(endH)}
                  </div>
                  <div style={{ fontSize: 14, color: "var(--ink-soft)", marginTop: 2 }}>{dayDateStr}</div>
                </div>

                <div>
                  <span className="label" style={{ fontSize: 11 }}>
                    Duration
                  </span>
                  <div className="row" style={{ gap: 6, marginTop: 8, flexWrap: "wrap" }}>
                    {[1, 2, 3, 4].map((d) => {
                      const fits = slotFits(selectedSlot.dayIdx, selectedSlot.hour, d);
                      return (
                        <button
                          key={d}
                          className={`chip ${duration === d ? "active" : ""}`}
                          disabled={!fits}
                          style={{ opacity: fits ? 1 : 0.35 }}
                          onClick={() => setDuration(d)}
                        >
                          {d}h
                        </button>
                      );
                    })}
                  </div>
                  {!slotValid && (
                    <p style={{ fontSize: 12, color: "var(--orange-deep)", marginTop: 8 }}>
                      Doesn't fit — pick a shorter duration or another slot.
                    </p>
                  )}
                </div>

                <label className="bf-recurring">
                  <input
                    type="checkbox"
                    checked={recurring}
                    onChange={(e) => setRecurring(e.target.checked)}
                    style={{ marginTop: 4, accentColor: "var(--indigo)" }}
                  />
                  <div>
                    <div style={{ fontWeight: 600, fontSize: 14 }}>
                      Repeat every {selectedDate.toLocaleDateString("en", { weekday: "long" })}
                    </div>
                    <div style={{ color: "var(--ink-mute)", fontSize: 12 }}>For 11 sessions</div>
                  </div>
                </label>

                <div className="bf-actions">
                  <button className="btn btn-ghost" onClick={onCancel} disabled={submit.isPending}>
                    Cancel
                  </button>
                  <button
                    className="btn btn-primary grow"
                    disabled={!slotValid || submit.isPending}
                    onClick={handleConfirm}
                  >
                    {submit.isPending
                      ? "Confirming…"
                      : `Confirm — ${space.price === 0 ? "Free" : `$${space.price * duration}`}`}
                  </button>
                </div>
              </>
            ) : (
              <div className="bf-empty">
                <div className="h-display" style={{ fontSize: 22, marginBottom: 6 }}>
                  Tap any open slot.
                </div>
                <p style={{ fontSize: 13, color: "var(--ink-mute)" }}>
                  Green is open. Hatched is booked. Past hours are dimmed. Pick a starting hour, set the duration on the right.
                </p>
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

interface LegendProps {
  swatch?: string;
  pattern?: boolean;
  label: string;
}

function Legend({ swatch, pattern, label }: LegendProps) {
  const sw = pattern
    ? { background: "repeating-linear-gradient(45deg, #e4e1e7, #e4e1e7 3px, #f0edf2 3px, #f0edf2 6px)" }
    : { background: swatch };
  return (
    <span className="row" style={{ gap: 5, color: "var(--ink-mute)" }}>
      <span style={{ width: 14, height: 10, borderRadius: 2, ...sw }} />
      <span className="h-mono" style={{ letterSpacing: "0.08em", fontSize: 10 }}>
        {label.toUpperCase()}
      </span>
    </span>
  );
}
