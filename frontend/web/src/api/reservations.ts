import { api } from "./client";
import type {
  ApiCreateReservationRequest,
  ApiPageResponse,
  ApiPassResponse,
  ApiReservationResponse,
} from "./types";

export const reservationsApi = {
  submit: (slugOrId: string, body: ApiCreateReservationRequest) =>
    api.post<ApiReservationResponse>(
      `/spaces/${encodeURIComponent(slugOrId)}/reservation-requests`,
      body,
    ),

  /** GET /api/reservations/mine — confirmed reservations only. */
  mine: (params: { upcoming?: boolean; page?: number; size?: number } = {}) => {
    const search = new URLSearchParams();
    if (params.upcoming !== undefined)
      search.set("upcoming", params.upcoming ? "true" : "false");
    search.set("page", String(params.page ?? 0));
    search.set("size", String(params.size ?? 50));
    return api.get<ApiPageResponse<ApiReservationResponse>>(
      `/reservations/mine?${search.toString()}`,
    );
  },

  /** Single reservation by uuid (NOT the human reservation_code). */
  get: (id: string) => api.get<ApiReservationResponse>(`/reservations/${id}`),

  /** Pass with full space embedded + qrPayload. */
  pass: (id: string) => api.get<ApiPassResponse>(`/reservations/${id}/pass`),

  /** Cancel a pending or approved reservation request. */
  cancel: (requestId: string, reason?: string) =>
    api.post<unknown>(
      `/reservation-requests/${requestId}/cancel`,
      reason ? { reason } : null,
    ),
};
