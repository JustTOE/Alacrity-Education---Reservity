/**
 * Static reference data + (temporarily) the community events board.
 *
 * After backend wiring (M1-M5):
 *  - SPACES, HISTORY, NOW, DATE_WINDOW are gone — components read from
 *    `useSpaces` / `useReservations` / `useNow` / `buildDateWindow`.
 *  - VIBES stays as client-side reference data (8 brand-curated tags;
 *    no /api/vibes endpoint and no plan to add one).
 *  - EVENTS stays as a fixture for the LandingRoute board until the
 *    M8 follow-up swap (`§FE-FOLLOWUP-EVENTS`) wires `useEvents`.
 */

import type { CommunityEvent, Vibe } from "@/types";

export const VIBES: Vibe[] = [
  { id: "focus", label: "Focus mode", icon: "◐" },
  { id: "loud", label: "Loud and proud", icon: "◉" },
  { id: "window", label: "Window seat", icon: "▢" },
  { id: "solo", label: "Solo", icon: "·" },
  { id: "group", label: "Group of 4+", icon: "◊" },
  { id: "natural-light", label: "Natural light", icon: "☼" },
  { id: "after-hours", label: "After hours", icon: "◑" },
  { id: "lab-only", label: "Lab certified", icon: "⌬" },
];

export const EVENTS: CommunityEvent[] = [
  {
    id: "ev-poetry",
    title: "Open-mic poetry, no rules",
    host: "Linden Writers Co.",
    hostInitials: "LW",
    space: "The Glass Atrium",
    building: "Pavilion North",
    status: "live",
    startH: 13,
    endH: 16,
    attendees: 28,
    capacity: 40,
    tag: "Open to all",
    blurb: "Bring a notebook or just listen. Bring tea.",
  },
  {
    id: "ev-thesis",
    title: "Thesis crit · senior studio",
    host: "Magnolia Arts Faculty",
    hostInitials: "MA",
    space: "Crit Room East",
    building: "Magnolia Arts",
    status: "live",
    startH: 13,
    endH: 17,
    attendees: 14,
    capacity: 16,
    tag: "Invite only",
    blurb: "4th-year thesis projects. Quiet observers welcome.",
  },
  {
    id: "ev-build",
    title: "Build night: Arduino + plants",
    host: "Hawthorn Robotics Club",
    hostInitials: "HR",
    space: "Rooftop Greenhouse",
    building: "Hawthorn Sciences",
    status: "soon",
    startH: 17,
    endH: 20,
    attendees: 22,
    capacity: 30,
    tag: "Free · drop-in",
    blurb: "We're wiring soil sensors. Solder at your own risk.",
  },
  {
    id: "ev-jam",
    title: "Friday jam · acoustic only",
    host: "Music Society",
    hostInitials: "MS",
    space: "Recording Studio A",
    building: "Linden Hall",
    status: "soon",
    startH: 19,
    endH: 22,
    attendees: 9,
    capacity: 12,
    tag: "Bring an instrument",
    blurb: "Soft chairs, soft lights, soft songs.",
  },
  {
    id: "ev-talk",
    title: "Mira Adesanya · sci-comm talk",
    host: "Hawthorn Sciences",
    hostInitials: "HS",
    space: "Auditorium 1",
    building: "Hawthorn Sciences",
    status: "tomorrow",
    startH: 11,
    endH: 12,
    attendees: 84,
    capacity: 220,
    tag: "Free · RSVP",
    blurb: "How to write about science without lying or boring anyone.",
  },
];
