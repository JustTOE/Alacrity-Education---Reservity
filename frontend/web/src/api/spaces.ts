import { api } from "./client";
import type {
  ApiAvailabilityResponse,
  ApiBatchAvailabilityRequest,
  ApiBatchAvailabilityResponse,
  ApiPageResponse,
  ApiSpaceResponse,
  ApiSpaceSummary,
} from "./types";
import type { Space, SpaceImage, SpaceType, VibeId } from "@/types";

export interface SpacesQuery {
  q?: string;
  type?: SpaceType;
  minSeats?: number;
  maxPrice?: number;
  isFree?: boolean;
  vibes?: VibeId[];
  buildingId?: string;
  hideFull?: boolean;
  page?: number;
  size?: number;
  sort?: string;
}

function buildQuery(params: Record<string, unknown>): string {
  const search = new URLSearchParams();
  for (const [k, v] of Object.entries(params)) {
    if (v === undefined || v === null) continue;
    if (Array.isArray(v)) {
      if (v.length === 0) continue;
      search.set(k, v.join(","));
    } else if (typeof v === "boolean") {
      search.set(k, v ? "true" : "false");
    } else {
      search.set(k, String(v));
    }
  }
  const s = search.toString();
  return s ? `?${s}` : "";
}

/**
 * Map an `ApiSpaceSummary` → frontend `Space`. The summary endpoint omits
 * heavy fields (description, amenities, rules, events, pin), so we fill in
 * sensible defaults; the listings card view never reads them.
 */
export function summaryToSpace(s: ApiSpaceSummary): Space {
  return {
    id: s.id, // slug
    name: s.name,
    type: (s.type as SpaceType) ?? "open",
    building: s.building,
    floor: 0,
    room: "",
    seats: s.seats,
    area: s.area,
    price: s.price,
    currency: s.currency,
    vibes: (s.vibes as VibeId[]) ?? [],
    booked: [],
    events: [],
    viewing: 0,
    todayBookings: 0,
    fullyBooked: undefined,
    blurb: s.blurb,
    description: "",
    amenities: [],
    rules: [],
    pin: { x: s.pin?.x ?? 0.5, y: s.pin?.y ?? 0.5 },
    surprise: s.surprise ?? undefined,
    // M3+ extensions
    slug: s.id,
    primaryImageUrl: s.primaryImageUrl,
    instantBook: s.instantBook,
    dropIn: s.dropIn,
  };
}

export function responseToSpace(s: ApiSpaceResponse): Space {
  const images: SpaceImage[] = s.images.map((i) => ({
    id: i.id,
    url: i.url,
    altText: i.altText,
    width: i.width,
    height: i.height,
    displayOrder: i.displayOrder,
    isPrimary: i.isPrimary,
  }));
  return {
    id: s.id, // slug
    name: s.name,
    type: (s.type as SpaceType) ?? "open",
    building: s.building,
    floor: s.floor,
    room: s.room,
    seats: s.seats,
    area: s.area,
    price: s.price,
    currency: s.currency,
    vibes: (s.vibes as VibeId[]) ?? [],
    booked: s.booked ?? [],
    events: s.events ?? [],
    viewing: s.viewing ?? 0,
    todayBookings: s.todayBookings ?? 0,
    fullyBooked: s.fullyBooked ?? undefined,
    blurb: s.blurb,
    description: s.description ?? "",
    amenities: s.amenities ?? [],
    rules: s.rules ?? [],
    pin: { x: s.pin?.x ?? 0.5, y: s.pin?.y ?? 0.5 },
    surprise: s.surprise ?? undefined,
    // M3+ extensions
    slug: s.id,
    primaryImageUrl: s.primaryImageUrl,
    instantBook: s.instantBook,
    dropIn: s.dropIn,
    images,
  };
}

export const spacesApi = {
  list: (query: SpacesQuery = {}) => {
    const qs = buildQuery({
      q: query.q,
      type: query.type,
      minSeats: query.minSeats,
      maxPrice: query.maxPrice,
      isFree: query.isFree,
      vibes: query.vibes,
      buildingId: query.buildingId,
      hideFull: query.hideFull,
      page: query.page ?? 0,
      size: query.size ?? 50,
      sort: query.sort,
    });
    return api.get<ApiPageResponse<ApiSpaceSummary>>(`/spaces${qs}`);
  },

  get: (slugOrId: string) =>
    api.get<ApiSpaceResponse>(`/spaces/${encodeURIComponent(slugOrId)}`),

  availabilityRange: (slugOrId: string, from: string, to: string) =>
    api.get<ApiAvailabilityResponse>(
      `/spaces/${encodeURIComponent(slugOrId)}/availability?from=${from}&to=${to}`,
    ),

  availabilityBatch: (req: ApiBatchAvailabilityRequest) =>
    api.post<ApiBatchAvailabilityResponse>("/spaces/availability:batch", req),
};
