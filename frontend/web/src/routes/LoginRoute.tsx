import { FormEvent, useState } from "react";
import { Link, useNavigate, useSearchParams } from "react-router-dom";
import { useAuth } from "@/auth/AuthContext";
import { ApiError } from "@/api/client";

export default function LoginRoute() {
  const auth = useAuth();
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();
  const returnTo = searchParams.get("returnTo") ?? "/";

  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [error, setError] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);

  async function onSubmit(e: FormEvent) {
    e.preventDefault();
    setError(null);
    setSubmitting(true);
    try {
      await auth.login(email, password);
      navigate(returnTo, { replace: true });
    } catch (err) {
      if (err instanceof ApiError) {
        setError(
          err.status === 401
            ? "Wrong email or password."
            : err.message || "Something went wrong. Try again.",
        );
      } else {
        setError("Network error. Is the backend running on :8080?");
      }
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <main className="container" style={{ padding: "64px 32px", minHeight: "60vh", maxWidth: 480 }}>
      <p className="kicker">Welcome back</p>
      <h1 className="h-display" style={{ fontSize: 48, margin: "8px 0 24px" }}>
        Sign in
      </h1>

      <form onSubmit={onSubmit} style={{ display: "grid", gap: 16 }}>
        <label style={{ display: "grid", gap: 6 }}>
          <span style={{ fontSize: 13, color: "var(--ink-soft)" }}>Email or Username</span>
          <input
            type="text"
            required
            autoComplete="username"
            value={email}
            onChange={(e) => setEmail(e.target.value)}
            style={inputStyle}
          />
        </label>

        <label style={{ display: "grid", gap: 6 }}>
          <span style={{ fontSize: 13, color: "var(--ink-soft)" }}>Password</span>
          <input
            type="password"
            required
            autoComplete="current-password"
            value={password}
            onChange={(e) => setPassword(e.target.value)}
            style={inputStyle}
          />
        </label>

        {error ? (
          <div
            role="alert"
            style={{
              padding: "10px 14px",
              background: "var(--paper-2, #fbeae5)",
              border: "1px solid var(--coral, #d6644a)",
              borderRadius: 6,
              fontSize: 14,
              color: "var(--ink, #2a2820)",
            }}
          >
            {error}
          </div>
        ) : null}

        <button
          type="submit"
          disabled={submitting}
          className="btn btn-primary"
          style={{
            padding: "12px 18px",
            background: "var(--indigo, #4b52a7)",
            color: "white",
            border: 0,
            borderRadius: 8,
            fontSize: 15,
            fontWeight: 600,
            cursor: submitting ? "not-allowed" : "pointer",
            opacity: submitting ? 0.7 : 1,
          }}
        >
          {submitting ? "Signing in…" : "Sign in"}
        </button>
      </form>

      <p style={{ marginTop: 24, fontSize: 14, color: "var(--ink-soft)" }}>
        New here?{" "}
        <Link
          to={`/register${returnTo !== "/" ? `?returnTo=${encodeURIComponent(returnTo)}` : ""}`}
          style={{ color: "var(--indigo, #4b52a7)" }}
        >
          Create an account
        </Link>
      </p>
    </main>
  );
}

const inputStyle: React.CSSProperties = {
  padding: "10px 12px",
  border: "1px solid var(--paper-3, #d8d2c4)",
  borderRadius: 6,
  fontSize: 15,
  fontFamily: "inherit",
  background: "var(--paper, #faf6ec)",
};
