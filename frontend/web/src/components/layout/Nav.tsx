import { NavLink, useNavigate } from "react-router-dom";
import Wordmark from "@/components/brand/Wordmark";
import { useAuth } from "@/auth/AuthContext";

export default function Nav() {
  const navigate = useNavigate();
  const auth = useAuth();
  const isAuthed = auth.status === "authenticated";
  const accountType = auth.user?.accountType;
  
  const canHost = accountType === "OWNER";
  const isAdmin = accountType === "ADMIN";

  return (
    <header className="nav">
      <div className="nav-inner">
        <button onClick={() => navigate("/")} aria-label="Reservity home">
          <Wordmark size={20} />
        </button>
        <nav className="nav-tabs" aria-label="Primary">
          <NavLink
            to="/"
            end
            className={({ isActive }) => `nav-tab ${isActive ? "active" : ""}`}
          >
            Home
          </NavLink>
          <NavLink
            to="/spaces"
            className={({ isActive }) => `nav-tab ${isActive ? "active" : ""}`}
          >
            Spaces
          </NavLink>
          {canHost && (
            <NavLink
              to="/create"
              className={({ isActive }) => `nav-tab ${isActive ? "active" : ""}`}
            >
              List a space
            </NavLink>
          )}
          {isAuthed && (
            <NavLink
              to="/account"
              className={({ isActive }) => `nav-tab ${isActive ? "active" : ""}`}
            >
              My bookings
            </NavLink>
          )}
        </nav>
        <div className="nav-spacer" />
        <button className="nav-tab" aria-label="Location">
          📍 Hawthorn Campus
        </button>
        {isAdmin && (
          <button
            className="admin-badge"
            onClick={() => navigate("/admin")}
            aria-label="Open admin console"
            title="You're signed in as an admin"
          >
            <svg width="12" height="12" viewBox="0 0 16 16" fill="none" stroke="currentColor" strokeWidth="2" aria-hidden="true"><path d="M8 1.5 L13.5 4 V8.5 C13.5 11.5 11 13.5 8 14.5 C5 13.5 2.5 11.5 2.5 8.5 V4 Z"/></svg>
            ADMIN
          </button>
        )}
        {isAuthed && auth.user ? (
          <button
            className="avatar"
            onClick={() => navigate("/account")}
            aria-label={`Open dashboard for ${auth.user.displayName}`}
          >
            {auth.user.initials}
          </button>
        ) : (
          <button
            className="nav-tab"
            onClick={() => navigate("/login")}
            aria-label="Sign in"
          >
            Sign in
          </button>
        )}
      </div>
    </header>
  );
}