import { FormEvent, useEffect, useState } from "react";
import { Link, useNavigate, useSearchParams } from "react-router-dom";
import { useAuth } from "@/auth/AuthContext";
import { ApiError } from "@/api/client";

const HANDLE_RE = /^[a-z0-9_]{3,30}$/;

function suggestHandle(email: string): string {
  const local = email.split("@")[0] ?? "";
  return local
    .toLowerCase()
    .replace(/[^a-z0-9_]/g, "_")
    .replace(/_+/g, "_")
    .replace(/^_|_$/g, "")
    .slice(0, 30);
}

export default function RegisterRoute() {
  const auth = useAuth();
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();
  const returnTo = searchParams.get("returnTo") ?? "/";

  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [displayName, setDisplayName] = useState("");
  const [handle, setHandle] = useState("");
  const [handleEdited, setHandleEdited] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);

  // Auto-derive handle from email until user edits it explicitly.
  useEffect(() => {
    if (!handleEdited && email.includes("@")) {
      setHandle(suggestHandle(email));
    }
  }, [email, handleEdited]);

  function validate(): string | null {
    if (password.length < 8) return "Password must be at least 8 characters.";
    if (!HANDLE_RE.test(handle))
      return "Handle must be 3-30 chars: lowercase letters, digits, underscores.";
    if (displayName.trim().length === 0) return "Display name can't be empty.";
    return null;
  }

  async function onSubmit(e: FormEvent) {
    e.preventDefault();
    const v = validate();
    if (v) {
      setError(v);
      return;
    }
    setError(null);
    setSubmitting(true);
    try {
      await auth.register({ email, password, displayName, handle });
      navigate(returnTo, { replace: true });
    } catch (err) {
      if (err instanceof ApiError) {
        const body = err.body as { message?: string } | null;
        setError(body?.message ?? err.message ?? "Registration failed.");
      } else {
        setError("Network error. Is the backend running on :8080?");
      }
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <main className="container" style={{ padding: "64px 32px", minHeight: "60vh", maxWidth: 480 }}>
      <p className="kicker">Get started</p>
      <h1 className="h-display" style={{ fontSize: 48, margin: "8px 0 24px" }}>
        Create your account
      </h1>

      <form onSubmit={onSubmit} style={{ display: "grid", gap: 16 }}>
        <label style={{ display: "grid", gap: 6 }}>
          <span style={{ fontSize: 13, color: "var(--ink-soft)" }}>Display name</span>
          <input
            type="text"
            required
            value={displayName}
            onChange={(e) => setDisplayName(e.target.value)}
            placeholder="Maya Reyes"
            style={inputStyle}
          />
        </label>

        <label style={{ display: "grid", gap: 6 }}>
          <span style={{ fontSize: 13, color: "var(--ink-soft)" }}>Email</span>
          <input
            type="email"
            required
            autoComplete="email"
            value={email}
            onChange={(e) => setEmail(e.target.value)}
            style={inputStyle}
          />
        </label>

        <label style={{ display: "grid", gap: 6 }}>
          <span style={{ fontSize: 13, color: "var(--ink-soft)" }}>
            Handle <span style={{ color: "var(--ink-3)" }}>· @{handle || "your_handle"}</span>
          </span>
          <input
            type="text"
            required
            value={handle}
            onChange={(e) => {
              setHandle(e.target.value.toLowerCase());
              setHandleEdited(true);
            }}
            pattern="^[a-z0-9_]{3,30}$"
            placeholder="maya_r"
            style={inputStyle}
          />
          <span style={{ fontSize: 12, color: "var(--ink-3)" }}>
            3-30 chars: lowercase, digits, underscores.
          </span>
        </label>

        <label style={{ display: "grid", gap: 6 }}>
          <span style={{ fontSize: 13, color: "var(--ink-soft)" }}>Password</span>
          <input
            type="password"
            required
            autoComplete="new-password"
            value={password}
            onChange={(e) => setPassword(e.target.value)}
            minLength={8}
            style={inputStyle}
          />
          <span style={{ fontSize: 12, color: "var(--ink-3)" }}>At least 8 characters.</span>
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
          {submitting ? "Creating account…" : "Create account"}
        </button>
      </form>

      <p style={{ marginTop: 24, fontSize: 14, color: "var(--ink-soft)" }}>
        Already have an account?{" "}
        <Link
          to={`/login${returnTo !== "/" ? `?returnTo=${encodeURIComponent(returnTo)}` : ""}`}
          style={{ color: "var(--indigo, #4b52a7)" }}
        >
          Sign in
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
