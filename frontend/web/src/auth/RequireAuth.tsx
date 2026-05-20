import { Navigate, useLocation } from "react-router-dom";
import type { ReactNode } from "react";
import { useAuth } from "./AuthContext";

export function RequireAuth({ children }: { children: ReactNode }) {
  const auth = useAuth();
  const location = useLocation();

  if (auth.status === "loading") {
    return (
      <div className="page-shell" style={{ padding: "4rem 2rem", textAlign: "center" }}>
        <p style={{ color: "var(--ink-3)" }}>Loading…</p>
      </div>
    );
  }

  if (auth.status === "unauthenticated") {
    const here = location.pathname + location.search;
    return <Navigate to={`/login?returnTo=${encodeURIComponent(here)}`} replace />;
  }

  return <>{children}</>;
}
