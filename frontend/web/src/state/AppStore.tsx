import { createContext, useCallback, useContext, useEffect, useMemo, useReducer } from "react";
import type { ReactNode } from "react";
import type { Space, ToastKind } from "@/types";
import { Action, AppState, initialState, reducer } from "./reducer";

interface AppContextValue {
  state: AppState;
  dispatch: React.Dispatch<Action>;
  openBooking: (space: Space, date?: Date | null) => void;
  closeBooking: () => void;
  showToast: (msg: string, kind?: ToastKind) => void;
  clearToast: () => void;
}

const AppContext = createContext<AppContextValue | null>(null);

export function AppStoreProvider({ children }: { children: ReactNode }) {
  const [state, dispatch] = useReducer(reducer, initialState);

  const openBooking = useCallback(
    (space: Space, date?: Date | null) => dispatch({ type: "OPEN_BOOKING", space, date }),
    [],
  );
  const closeBooking = useCallback(() => dispatch({ type: "CLOSE_BOOKING" }), []);
  const showToast = useCallback(
    (msg: string, kind?: ToastKind) => dispatch({ type: "SHOW_TOAST", msg, kind }),
    [],
  );
  const clearToast = useCallback(() => dispatch({ type: "CLEAR_TOAST" }), []);

  // Auto-clear toast after 2400ms.
  useEffect(() => {
    if (!state.toast) return;
    const id = window.setTimeout(() => dispatch({ type: "CLEAR_TOAST" }), 2400);
    return () => window.clearTimeout(id);
  }, [state.toast]);

  const value = useMemo<AppContextValue>(
    () => ({ state, dispatch, openBooking, closeBooking, showToast, clearToast }),
    [state, openBooking, closeBooking, showToast, clearToast],
  );

  return <AppContext.Provider value={value}>{children}</AppContext.Provider>;
}

export function useApp() {
  const ctx = useContext(AppContext);
  if (!ctx) throw new Error("useApp must be used within AppStoreProvider");
  return ctx;
}
