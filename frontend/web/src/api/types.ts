/**
 * Backend response shape mirrors. Field-for-field copies of the Java records
 * under `dev.tmmc.reservity.*.dto` so the API client returns typed data without
 * us having to import generated code.
 *
 * Naming: every type prefixed `Api` to keep them distinct from the frontend's
 * domain types (Space, ConfirmedBooking, …). API → frontend mapping happens in
 * `api/spaces.ts` etc. before data crosses into hooks/components.
 */

// ============================================================================
// Auth (M1)
// ============================================================================

export type ApiAccountType = "REQUESTER" | "OWNER" | "ADMIN";

export interface ApiUserResponse {
  id: string;
  email: string;
  emailVerified: boolean;
  handle: string;
  displayName: string;
  realName: string | null;
  initials: string;
  accountType: ApiAccountType;
  verifiedStudent: boolean;
  bio: string | null;
  avatarUrl: string | null;
  coverGradient: string;
  memberSince: string; // ISO date "YYYY-MM-DD"
  twoFaEnabled: boolean;
  createdAt: string; // ISO instant
}

export interface ApiAuthResponse {
  accessToken: string;
  refreshToken: string;
  expiresInSeconds: number;
  user: ApiUserResponse;
}

export interface ApiRegisterRequest {
  email: string;
  password: string;
  displayName: string;
  handle: string;
}

export interface ApiLoginRequest {
  email: string;
  password: string;
}

export interface ApiRefreshRequest {
  refreshToken: string;
}

// ============================================================================
// Spaces (M2 + M3 + M5)
// ============================================================================

export interface ApiPin {
  x: number | null;
  y: number | null;
}

export interface ApiSpaceImageResponse {
  id: string;
  url: string;
  altText: string | null;
  width: number | null;
  height: number | null;
  displayOrder: number;
  isPrimary: boolean;
}

export interface ApiSpaceEventDto {
  startH: number;
  endH: number;
  title: string;
  host: string;
}

export interface ApiSpaceSummary {
  id: string; // slug
  uuid: string;
  name: string;
  type: string; // "lab" | "pod" | "open" | "studio" (already lowercased server-side)
  building: string;
  seats: number;
  area: number;
  price: number;
  currency: string;
  vibes: string[];
  blurb: string;
  pin: ApiPin | null;
  surprise: boolean | null;
  dropIn: boolean;
  instantBook: boolean;
  primaryImageUrl: string | null;
}

export interface ApiSpaceResponse {
  id: string; // slug
  uuid: string;
  name: string;
  type: string;
  building: string;
  buildingId: string;
  floor: number;
  room: string;
  seats: number;
  area: number;
  price: number;
  currency: string;
  vibes: string[];
  booked: number[];
  events: ApiSpaceEventDto[];
  viewing: number;
  todayBookings: number;
  fullyBooked: boolean | null;
  blurb: string;
  description: string | null;
  amenities: string[];
  rules: string[];
  pin: ApiPin | null;
  surprise: boolean | null;
  dropIn: boolean;
  instantBook: boolean;
  images: ApiSpaceImageResponse[];
  primaryImageUrl: string | null;
}

export interface ApiPageResponse<T> {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
  hasNext: boolean;
}

export interface ApiBuildingResponse {
  id: string;
  name: string;
  shortCode: string;
  campusName: string | null;
  address: string | null;
  latitude: number | null;
  longitude: number | null;
  pin: ApiPin | null;
  heroImageUrl: string | null;
  description: string | null;
}

// ============================================================================
// Availability (M4)
// ============================================================================

export type ApiSlotState = "OPEN" | "CLOSED" | "RESERVED" | "EVENT";

export interface ApiHourSlot {
  hour: number;
  state: ApiSlotState;
  label: string | null;
  eventId: string | null;
  reservationId: string | null;
}

export interface ApiEventDto {
  source: "RESERVATION" | "EVENT";
  id: string | null;
  startH: number;
  endH: number;
  title: string;
  host: string;
}

export interface ApiDayAvailability {
  date: string; // YYYY-MM-DD
  dayOfWeek: string;
  hours: ApiHourSlot[];
  events: ApiEventDto[];
  booked: number[];
  fullyBooked: boolean;
  closedAllDay: boolean;
}

export interface ApiAvailabilityResponse {
  spaceId: string;
  slug: string;
  name: string;
  zone: string;
  days: ApiDayAvailability[];
}

export interface ApiBatchAvailabilityRequest {
  spaceIds?: string[];
  slugs?: string[];
  date: string; // YYYY-MM-DD
}

export interface ApiBatchAvailabilityResponse {
  date: string;
  zone: string;
  spaces: Record<string, ApiDayAvailability>;
}

// ============================================================================
// Reservations (M5)
// ============================================================================

export interface ApiCreateReservationRequest {
  startsAt: string; // ISO instant
  endsAt: string;
  recurring?: "none" | "weekly";
  purpose?: string | null;
  attendeesCount?: number | null;
  contactPhone?: string | null;
  tag?: string | null;
}

export type ApiReservationStatus =
  | "PENDING"
  | "APPROVED"
  | "DENIED"
  | "CANCELLED"
  | "EXPIRED";

export interface ApiReservationResponse {
  id: string; // reservation_code "RV-LAB4B-839271"
  requestId: string;
  reservationId: string | null;
  space: ApiSpaceSummary;
  startsAt: string;
  endsAt: string;
  status: ApiReservationStatus;
  autoApproved: boolean;
  passToken: string | null;
  createdAt: string;
  decidedAt: string | null;
  recurring: "none" | "weekly";
  seriesId: string | null;
  seriesOccurrences: number | null;
}

export interface ApiPassResponse {
  id: string;
  reservationCode: string;
  space: ApiSpaceResponse;
  startsAt: string;
  endsAt: string;
  qrPayload: string;
  revoked: boolean;
  checkinAt: string | null;
  checkoutAt: string | null;
}
