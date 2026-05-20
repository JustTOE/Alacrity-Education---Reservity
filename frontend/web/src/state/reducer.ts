import type { RecurringMode, Space, ToastKind, ToastMsg } from "@/types";

export interface BookingDraftState {
  space: Space;
  date: Date | null;
  startH: number | null;
  duration: number;
  recurring: boolean;
}

export interface AppState {
  bookingDraft: BookingDraftState | null;
  toast: ToastMsg | null;
}

export const initialState: AppState = {
  bookingDraft: null,
  toast: null,
};

export type Action =
  | { type: "OPEN_BOOKING"; space: Space; date?: Date | null }
  | { type: "UPDATE_DRAFT"; patch: Partial<Omit<BookingDraftState, "space">> }
  | { type: "CLOSE_BOOKING" }
  | { type: "SHOW_TOAST"; msg: string; kind?: ToastKind }
  | { type: "CLEAR_TOAST" };

export function reducer(state: AppState, action: Action): AppState {
  switch (action.type) {
    case "OPEN_BOOKING":
      return {
        ...state,
        bookingDraft: {
          space: action.space,
          date: action.date ?? null,
          startH: null,
          duration: 2,
          recurring: false,
        },
      };
    case "UPDATE_DRAFT":
      if (!state.bookingDraft) return state;
      return { ...state, bookingDraft: { ...state.bookingDraft, ...action.patch } };
    case "CLOSE_BOOKING":
      return { ...state, bookingDraft: null };
    case "SHOW_TOAST":
      return { ...state, toast: { msg: action.msg, kind: action.kind ?? "info" } };
    case "CLEAR_TOAST":
      return { ...state, toast: null };
    default:
      return state;
  }
}

export type RecurringValue = RecurringMode;
