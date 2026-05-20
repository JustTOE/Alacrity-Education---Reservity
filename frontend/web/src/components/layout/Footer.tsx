import { Link } from "react-router-dom";
import Logomark from "@/components/brand/Logomark";

interface FooterLink {
  label: string;
  to?: string;
}

interface FooterColProps {
  title: string;
  links: FooterLink[];
}

function FooterCol({ title, links }: FooterColProps) {
  return (
    <div>
      <div
        className="h-mono"
        style={{
          fontSize: 11,
          letterSpacing: "0.22em",
          color: "rgba(243,240,245,0.55)",
          textTransform: "uppercase",
          marginBottom: 16,
        }}
      >
        {title}
      </div>
      <ul
        style={{
          listStyle: "none",
          padding: 0,
          margin: 0,
          display: "flex",
          flexDirection: "column",
          gap: 10,
        }}
      >
        {links.map((l, i) => (
          <li key={i}>
            {l.to ? (
              <Link
                to={l.to}
                style={{
                  color: "rgba(243,240,245,0.85)",
                  fontSize: 14,
                  textDecoration: "none",
                }}
                onMouseEnter={(e) => (e.currentTarget.style.color = "#ffb892")}
                onMouseLeave={(e) =>
                  (e.currentTarget.style.color = "rgba(243,240,245,0.85)")
                }
              >
                {l.label}
              </Link>
            ) : (
              <button
                style={{
                  background: "none",
                  border: 0,
                  color: "rgba(243,240,245,0.85)",
                  fontSize: 14,
                  padding: 0,
                  cursor: "pointer",
                  textAlign: "left",
                }}
                onMouseEnter={(e) => (e.currentTarget.style.color = "#ffb892")}
                onMouseLeave={(e) =>
                  (e.currentTarget.style.color = "rgba(243,240,245,0.85)")
                }
              >
                {l.label}
              </button>
            )}
          </li>
        ))}
      </ul>
    </div>
  );
}

type SocialIcon = "ig" | "ig-tt" | "x" | "ds" | "gh";

const SOCIAL_PATHS: Record<SocialIcon, JSX.Element> = {
  ig: (
    <>
      <rect x="3" y="3" width="18" height="18" rx="5" />
      <circle cx="12" cy="12" r="4" />
      <circle cx="17.5" cy="6.5" r="0.8" fill="currentColor" />
    </>
  ),
  "ig-tt": <path d="M14 4v9.5a3.5 3.5 0 1 1-3.5-3.5M14 4c.5 2.5 2 4 4.5 4.5" />,
  x: (
    <>
      <path d="M4 4l16 16" />
      <path d="M20 4L4 20" />
    </>
  ),
  ds: (
    <>
      <path d="M5 7c2-1 4-1.5 7-1.5s5 .5 7 1.5l1 10c-2 2-4 2.5-7 2.5l-1-2-1 2c-3 0-5-.5-7-2.5z" />
      <circle cx="9" cy="13" r="1" fill="currentColor" />
      <circle cx="15" cy="13" r="1" fill="currentColor" />
    </>
  ),
  gh: (
    <path d="M12 3a9 9 0 0 0-3 17.5c.5.1.7-.2.7-.5v-2c-2.5.5-3-1-3-1-.5-1-1-1.5-1-1.5-1-.5 0-.5 0-.5 1 .1 1.5 1 1.5 1 1 1.5 2.5 1 3 .8.1-.7.4-1 .7-1.3-2-.2-4-1-4-4.5 0-1 .4-1.8 1-2.5 0-.2-.4-1.2.1-2.5 0 0 .8-.3 2.5 1a8.5 8.5 0 0 1 4.5 0c1.7-1.3 2.5-1 2.5-1 .5 1.3.2 2.3.1 2.5.6.7 1 1.5 1 2.5 0 3.5-2 4.3-4 4.5.4.3.7 1 .7 1.9v3c0 .3.2.6.7.5A9 9 0 0 0 12 3z" />
  ),
};

function Social({ icon, label }: { icon: SocialIcon; label: string }) {
  return (
    <a
      href="#"
      aria-label={label}
      title={label}
      style={{
        display: "grid",
        placeItems: "center",
        width: 36,
        height: 36,
        borderRadius: 9,
        background: "rgba(255,255,255,0.06)",
        color: "rgba(243,240,245,0.85)",
        transition: "all .15s",
      }}
      onMouseEnter={(e) => {
        e.currentTarget.style.background = "#ffb892";
        e.currentTarget.style.color = "#1b1b1f";
      }}
      onMouseLeave={(e) => {
        e.currentTarget.style.background = "rgba(255,255,255,0.06)";
        e.currentTarget.style.color = "rgba(243,240,245,0.85)";
      }}
    >
      <svg
        width="16"
        height="16"
        viewBox="0 0 24 24"
        fill="none"
        stroke="currentColor"
        strokeWidth="1.8"
        strokeLinecap="round"
        strokeLinejoin="round"
      >
        {SOCIAL_PATHS[icon]}
      </svg>
    </a>
  );
}

export default function Footer() {
  return (
    <footer
      style={{
        background: "var(--ink)",
        color: "rgba(243,240,245,0.92)",
        borderTop: "1px solid var(--line-soft)",
      }}
    >
      <div className="container" style={{ padding: "72px 32px 32px" }}>
        <div
          style={{
            display: "grid",
            gridTemplateColumns: "1.6fr 1fr 1fr 1fr 1.2fr",
            gap: 40,
          }}
          className="footer-grid"
        >
          <div>
            <div className="row" style={{ gap: 10 }}>
              <Logomark size={32} color="#fff" accent="#ffb892" />
              <span
                className="h-display"
                style={{
                  fontSize: 24,
                  color: "#fff",
                  letterSpacing: "-0.02em",
                }}
              >
                Reservity
              </span>
            </div>
            <p
              style={{
                marginTop: 16,
                fontSize: 14,
                lineHeight: 1.6,
                color: "rgba(243,240,245,0.7)",
                maxWidth: 280,
              }}
            >
              Drawn to scale, booked in 30 seconds. Built on the Hawthorn Campus, opened to all.
            </p>
            <form
              onSubmit={(e) => e.preventDefault()}
              style={{ marginTop: 20, display: "flex", gap: 6, maxWidth: 320 }}
            >
              <input
                type="email"
                placeholder="you@university.edu"
                aria-label="Email for weekly newsletter"
                style={{
                  flex: 1,
                  background: "rgba(255,255,255,0.08)",
                  border: "1px solid rgba(255,255,255,0.15)",
                  borderRadius: 8,
                  padding: "10px 12px",
                  color: "#fff",
                  fontSize: 14,
                  outline: "none",
                }}
              />
              <button
                type="submit"
                className="btn btn-orange"
                style={{ padding: "10px 14px" }}
              >
                Subscribe
              </button>
            </form>
            <p
              className="h-mono"
              style={{
                fontSize: 10,
                color: "rgba(243,240,245,0.45)",
                letterSpacing: "0.12em",
                marginTop: 10,
              }}
            >
              WEEKLY · WHAT'S ON, IN ONE EMAIL
            </p>
          </div>

          <FooterCol
            title="Spaces"
            links={[
              { label: "Browse all", to: "/spaces" },
              { label: "Labs" },
              { label: "Studios" },
              { label: "Pods" },
              { label: "Open rooms" },
              { label: "Suggest a space" },
            ]}
          />
          <FooterCol
            title="Events"
            links={[
              { label: "Live now" },
              { label: "Tonight" },
              { label: "This week" },
              { label: "Host an event" },
              { label: "Event guidelines" },
            ]}
          />
          <FooterCol
            title="Account"
            links={[
              { label: "My passes", to: "/account" },
              { label: "My events" },
              { label: "Vibe profile" },
              { label: "Notifications" },
              { label: "Sign out" },
            ]}
          />
          <FooterCol
            title="Reservity"
            links={[
              { label: "Who we are" },
              { label: "Campus partners" },
              { label: "Press kit" },
              { label: "Help center" },
              { label: "Accessibility" },
              { label: "Status · all green" },
            ]}
          />
        </div>

        <div
          style={{
            height: 1,
            background: "rgba(255,255,255,0.1)",
            margin: "48px 0 24px",
          }}
        />

        <div
          className="row"
          style={{
            justifyContent: "space-between",
            alignItems: "center",
            flexWrap: "wrap",
            gap: 16,
          }}
        >
          <span
            className="h-mono"
            style={{
              fontSize: 11,
              letterSpacing: "0.16em",
              color: "rgba(243,240,245,0.55)",
            }}
          >
            © 2026 RESERVITY · HAWTHORN CAMPUS · MADE WITH GRAPHITE & GRIDS
          </span>
          <div className="row" style={{ gap: 14 }}>
            <Social icon="ig" label="Instagram" />
            <Social icon="ig-tt" label="TikTok" />
            <Social icon="x" label="X" />
            <Social icon="ds" label="Discord" />
            <Social icon="gh" label="GitHub" />
          </div>
          <div className="row" style={{ gap: 16 }}>
            <a href="#" style={{ fontSize: 12, color: "rgba(243,240,245,0.6)" }}>
              Privacy
            </a>
            <a href="#" style={{ fontSize: 12, color: "rgba(243,240,245,0.6)" }}>
              Terms
            </a>
            <a href="#" style={{ fontSize: 12, color: "rgba(243,240,245,0.6)" }}>
              Cookies
            </a>
          </div>
        </div>
      </div>
      <style>{`
        @media (max-width: 980px){
          .footer-grid { grid-template-columns: 1fr 1fr !important; gap: 32px !important; }
          .footer-grid > :first-child { grid-column: span 2 !important; }
        }
      `}</style>
    </footer>
  );
}
