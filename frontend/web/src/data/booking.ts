/**
 * Pure UI helpers — formatting + status derivation. The seeded `bookedForDate`
 * RNG was deleted after the M4 swap; live availability comes from
 * `useAvailabilityBatch` / `spacesApi.availabilityRange`.
 */
import type { Space, SpaceStatus } from "@/types";

export function formatHour(h: number): string {
  if (h === 0) return "12am";
  if (h === 12) return "noon";
  if (h < 12) return `${h}am`;
  return `${h - 12}pm`;
}

export function getStatus(space: Space, currentHour: number): SpaceStatus {
  if (space.fullyBooked) {
    return { kind: "booked", label: "Booked till tomorrow", cls: "pill-booked" };
  }
  const isBookedNow = space.booked.includes(currentHour);
  if (!isBookedNow) {
    return { kind: "now", label: "Available now", cls: "pill-now" };
  }
  let h = currentHour;
  while (space.booked.includes(h) && h < 23) h++;
  if (h - currentHour <= 1) {
    return { kind: "soon", label: "Free in 30 min", cls: "pill-soon" };
  }
  return { kind: "booked", label: `Booked till ${formatHour(h)}`, cls: "pill-booked" };
}

export function nextFreeHour(space: Space, currentHour: number): number | null {
  if (space.fullyBooked) return null;
  let h = currentHour;
  if (!space.booked.includes(h)) return h;
  while (space.booked.includes(h) && h < 23) h++;
  return h;
}
