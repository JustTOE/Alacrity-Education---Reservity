import type { ApiDayAvailability, ApiEventDto } from "@/api/types";

/**
 * Wire format ↔ display format. The backend stores reservations as UTC
 * `Instant` and buckets availability by UTC hour on a UTC date. The UI shows
 * everything in the user's LOCAL timezone — what they see in the schedule grid
 * is what their wall clock reads.
 *
 * Use these helpers at the API boundary: parse local dates → UTC `Date` for
 * the wire; project the API's per-UTC-day buckets back onto local-hour grids
 * for rendering.
 */

/** "YYYY-MM-DD" of `d` interpreted in the LOCAL timezone. */
export function localIsoDate(d: Date): string {
  const y = d.getFullYear();
  const m = String(d.getMonth() + 1).padStart(2, "0");
  const day = String(d.getDate()).padStart(2, "0");
  return `${y}-${m}-${day}`;
}

/** "YYYY-MM-DD" of `d` interpreted in UTC. The wire format. */
export function utcIsoDate(d: Date): string {
  return d.toISOString().slice(0, 10);
}

/** Build a LOCAL midnight Date for the given Y-M-D. */
export function localMidnight(year: number, month0: number, day: number): Date {
  return new Date(year, month0, day, 0, 0, 0, 0);
}

/** Walk a list of UTC days from the API and bucket their reservations into a local-hour grid keyed by the local date string. */
export interface LocalDay {
  date: string; // local YYYY-MM-DD
  booked: Set<number>;
  events: { startH: number; endH: number; title: string; host: string }[];
  /** True if every hour 0..23 is in `booked` ∪ a closure (mirrors fullyBooked from API but we recompute since the day is now in local). */
  closedAllDay: boolean;
}

interface LocalEventBuild {
  startH: number;
  endH: number;
  title: string;
  host: string;
}

/**
 * Project a list of UTC-bucketed `ApiDayAvailability` entries onto local
 * dates. A reservation at UTC hour `h` on UTC date `d` lands at the local
 * hour and local date that `Date.UTC(d, h)` resolves to in the user's zone.
 *
 * To get a complete local day, you must pass UTC days adjacent to the local
 * date you care about: the local day spans up to two consecutive UTC dates
 * depending on the zone offset.
 */
export function projectUtcDaysToLocal(utcDays: ApiDayAvailability[]): Map<string, LocalDay> {
  const out = new Map<string, LocalDay>();

  // First pass: bucket booked hours by local date.
  for (const day of utcDays) {
    const [yStr, mStr, dStr] = day.date.split("-");
    const y = Number(yStr);
    const m = Number(mStr) - 1;
    const d = Number(dStr);
    for (const utcHour of day.booked) {
      const instant = new Date(Date.UTC(y, m, d, utcHour, 0, 0, 0));
      const localDate = localIsoDate(instant);
      const localHour = instant.getHours();
      let bucket = out.get(localDate);
      if (!bucket) {
        bucket = { date: localDate, booked: new Set(), events: [], closedAllDay: false };
        out.set(localDate, bucket);
      }
      bucket.booked.add(localHour);
    }
  }

  // Second pass: project events. A UTC event spanning [startH, endH) on UTC
  // date d may straddle two local dates. We split it at local-midnight and
  // emit one fragment per local date.
  for (const day of utcDays) {
    const [yStr, mStr, dStr] = day.date.split("-");
    const y = Number(yStr);
    const m = Number(mStr) - 1;
    const d = Number(dStr);
    for (const ev of day.events) {
      addEventToLocalDays(out, y, m, d, ev);
    }
  }

  return out;
}

function addEventToLocalDays(
  out: Map<string, LocalDay>,
  y: number,
  m: number,
  d: number,
  ev: ApiEventDto,
): void {
  // Each UTC hour `[h, h+1)` of the event maps to a local hour on a local date.
  // We collect contiguous local hours per local date and merge into ranges.
  const fragments: Map<string, LocalEventBuild[]> = new Map();
  for (let utcHour = ev.startH; utcHour < ev.endH; utcHour++) {
    const instant = new Date(Date.UTC(y, m, d, utcHour, 0, 0, 0));
    const localDate = localIsoDate(instant);
    const localHour = instant.getHours();
    const list = fragments.get(localDate) ?? [];
    const last = list[list.length - 1];
    if (last && last.endH === localHour && last.title === ev.title && last.host === ev.host) {
      last.endH = localHour + 1;
    } else {
      list.push({ startH: localHour, endH: localHour + 1, title: ev.title, host: ev.host });
    }
    fragments.set(localDate, list);
  }
  for (const [localDate, list] of fragments) {
    let bucket = out.get(localDate);
    if (!bucket) {
      bucket = { date: localDate, booked: new Set(), events: [], closedAllDay: false };
      out.set(localDate, bucket);
    }
    bucket.events.push(...list);
  }
}

/** Empty bucket helper for missing local dates. */
export function emptyLocalDay(date: string): LocalDay {
  return { date, booked: new Set(), events: [], closedAllDay: false };
}

/**
 * Return the UTC dates that need to be fetched to fully cover the local
 * range `[localStart, localEnd]` (inclusive). Adds a 1-day pad on each side
 * so any zone offset is covered.
 */
export function utcDatesForLocalRange(localStart: Date, localEnd: Date): { from: string; to: string } {
  const fromMs = new Date(localStart.getFullYear(), localStart.getMonth(), localStart.getDate()).getTime();
  const toMs = new Date(localEnd.getFullYear(), localEnd.getMonth(), localEnd.getDate()).getTime();
  const from = utcIsoDate(new Date(fromMs - 86_400_000));
  const to = utcIsoDate(new Date(toMs + 86_400_000));
  return { from, to };
}
