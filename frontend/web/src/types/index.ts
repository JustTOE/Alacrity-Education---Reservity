export type SpaceType = "lab" | "pod" | "open" | "studio";

export type VibeId =
  | "focus"
  | "loud"
  | "window"
  | "solo"
  | "group"
  | "natural-light"
  | "after-hours"
  | "lab-only";

export interface Vibe {
  id: VibeId;
  label: string;
  icon: string;
}

export interface SpaceEvent {
  startH: number;
  endH: number;
  title: string;
  host: string;
}

export interface SpaceImage {
  id: string;
  url: string;
  altText: string | null;
  width: number | null;
  height: number | null;
  displayOrder: number;
  isPrimary: boolean;
}

export interface Space {
  id: string;
  name: string;
  type: SpaceType;
  building: string;
  floor: number;
  room: string;
  seats: number;
  area: number;
  price: number;
  currency: string;
  vibes: VibeId[];
  booked: number[];
  events: SpaceEvent[];
  viewing: number;
  todayBookings: number;
  fullyBooked?: boolean;
  blurb: string;
  description: string;
  amenities: string[];
  rules: string[];
  pin: { x: number; y: number };
  surprise?: boolean;
  // Backend extensions (optional so existing fixtures still type-check)
  slug?: string;
  primaryImageUrl?: string | null;
  images?: SpaceImage[];
  instantBook?: boolean;
  dropIn?: boolean;
}

export type EventStatus = "live" | "soon" | "tomorrow";

export interface CommunityEvent {
  id: string;
  title: string;
  host: string;
  hostInitials: string;
  space: string;
  building: string;
  status: EventStatus;
  startH: number;
  endH: number;
  attendees: number;
  capacity: number;
  tag: string;
  blurb: string;
}

export interface HistoryEntry {
  date: string;
  space: string;
  dur: string;
  tag: string;
}

export interface BookedForDateResult {
  booked: number[];
  events: SpaceEvent[];
  fullyBooked: boolean;
}

export type StatusKind = "booked" | "now" | "soon";

export interface SpaceStatus {
  kind: StatusKind;
  label: string;
  cls: string;
}

export type RecurringMode = "none" | "weekly" | "biweekly";

export interface BookingDraft {
  space: Space;
  startH: number | null;
  endH: number | null;
  date: Date;
  recurring: RecurringMode;
  ctx?: { from?: string };
}

export interface ConfirmedBooking {
  id: string;
  space: Space;
  startH: number;
  endH: number;
  date: Date;
  recurring: RecurringMode;
  confirmedAt: Date;
  // Server-side fields (present after API submit)
  reservationCode?: string;
  reservationId?: string;
  passToken?: string;
  status?: "PENDING" | "APPROVED" | "DENIED" | "CANCELLED" | "EXPIRED";
}

export type SavedPass = ConfirmedBooking;

export type ToastKind = "info" | "success" | "error";

export interface ToastMsg {
  msg: string;
  kind: ToastKind;
}
