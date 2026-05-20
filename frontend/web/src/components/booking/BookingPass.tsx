import { formatHour } from "@/data/booking";
import type { ConfirmedBooking } from "@/types";
import Logomark from "@/components/brand/Logomark";

interface BookingPassProps {
  booking: ConfirmedBooking;
  onBack: () => void;
  onShare: () => void;
}

export default function BookingPass({ booking, onBack, onShare }: BookingPassProps) {
  const { space, startH, endH, recurring, date, id } = booking;
  const dateStr = date
    .toLocaleDateString("en", { weekday: "long", month: "long", day: "numeric" })
    .toUpperCase();

  const nameParts = space.name.split(" ");
  const nameHead = nameParts.slice(0, -1).join(" ");
  const nameTail = nameParts.slice(-1)[0];

  return (
    <div style={{ background: "var(--paper-3)", minHeight: "calc(100vh - 65px)", padding: "40px 24px" }}>
      <div className="container" style={{ maxWidth: 980 }}>
        <div
          className="row"
          style={{ justifyContent: "space-between", marginBottom: 24, flexWrap: "wrap", gap: 12 }}
        >
          <button className="btn btn-ghost" onClick={onBack}>
            ← Back to spaces
          </button>
          <div className="row gap-3">
            <button className="btn btn-ghost" onClick={onShare}>
              ↗ Share
            </button>
            <button className="btn btn-primary" onClick={() => window.print()}>
              ↓ Download pass
            </button>
          </div>
        </div>

        <div
          className="pass-grid"
          style={{
            display: "grid",
            gridTemplateColumns: "minmax(0, 1fr) auto",
            gap: 32,
            alignItems: "start",
          }}
        >
          <article
            className="pass grain"
            aria-label="Booking pass"
            style={{
              position: "relative",
              width: "100%",
              maxWidth: 540,
              aspectRatio: "1080/1920",
              borderRadius: 28,
              overflow: "hidden",
              color: "#fff",
              boxShadow: "var(--shadow-lg)",
              margin: "0 auto",
              display: "flex",
              flexDirection: "column",
            }}
          >
            <div style={{ position: "absolute", right: -60, top: -60, opacity: 0.18 }}>
              <Logomark size={360} color="#fff" accent="#fff" detail />
            </div>
            <svg style={{ position: "absolute", inset: 0, opacity: 0.18 }} aria-hidden="true">
              <defs>
                <pattern id="passgrid" width="32" height="32" patternUnits="userSpaceOnUse">
                  <path d="M 32 0 L 0 0 0 32" fill="none" stroke="#fff" strokeWidth="1" />
                </pattern>
              </defs>
              <rect width="100%" height="100%" fill="url(#passgrid)" />
            </svg>

            <header
              style={{
                padding: "32px 32px 0",
                display: "flex",
                justifyContent: "space-between",
                alignItems: "flex-start",
                position: "relative",
                zIndex: 2,
              }}
            >
              <Logomark size={28} color="#fff" accent="#fff" detail={false} />
              <span className="h-mono" style={{ fontSize: 11, letterSpacing: "0.18em", opacity: 0.9 }}>
                RESERVITY · PASS
              </span>
            </header>

            <div style={{ padding: "40px 32px 0", position: "relative", zIndex: 2 }}>
              <span
                className="h-mono"
                style={{ fontSize: 11, letterSpacing: "0.22em", opacity: 0.85 }}
              >
                {dateStr}
              </span>
              <h1
                className="h-display"
                style={{ fontSize: "clamp(48px, 8vw, 72px)", margin: "12px 0 0", lineHeight: 0.95 }}
              >
                {nameHead}
                {nameHead && <br />}
                <span style={{ color: "#ffd9bf" }}>{nameTail}</span>
              </h1>
              <p style={{ marginTop: 16, fontSize: 18, opacity: 0.95 }}>{space.blurb}</p>
            </div>

            <div
              style={{
                flex: 1,
                display: "grid",
                placeItems: "center",
                padding: 16,
                position: "relative",
                zIndex: 2,
              }}
            >
              <svg viewBox="0 0 360 240" style={{ width: "90%", height: "auto" }} aria-hidden="true">
                <g
                  fill="none"
                  stroke="#fff"
                  strokeWidth="1.4"
                  strokeLinecap="round"
                  strokeLinejoin="round"
                  opacity="0.95"
                >
                  <path d="M70 160 L180 100 L290 160 L180 220 Z" />
                  <path d="M70 160 L70 80 L180 20 L180 100 Z" />
                  <path d="M180 100 L180 20 L290 80 L290 160 Z" />
                  <path d="M130 130 L200 95 L250 120 L180 155 Z" />
                  <path d="M130 130 L130 138 L180 163 L180 155" />
                  <path d="M250 120 L250 128 L180 163" />
                  <path d="M170 100 L210 80 L210 60 L170 80 Z" />
                  <path d="M150 168 L185 152 L210 165 L175 181 Z" />
                </g>
                <g fill="none" stroke="#fff" strokeWidth="1" strokeDasharray="3 4" opacity="0.55">
                  <line x1="20" y1="220" x2="60" y2="170" />
                  <line x1="300" y1="170" x2="340" y2="220" />
                </g>
                <text
                  x="20"
                  y="232"
                  fontFamily="JetBrains Mono"
                  fontSize="9"
                  fill="#fff"
                  letterSpacing="0.18em"
                  opacity="0.8"
                >
                  {space.area}M² · {space.seats} SEATS
                </text>
                <text
                  x="340"
                  y="232"
                  fontFamily="JetBrains Mono"
                  fontSize="9"
                  fill="#fff"
                  letterSpacing="0.18em"
                  opacity="0.8"
                  textAnchor="end"
                >
                  FLOOR {space.floor}
                </text>
              </svg>
            </div>

            <div
              style={{
                borderTop: "1.5px dashed rgba(255,255,255,0.55)",
                margin: "0 24px",
                position: "relative",
                zIndex: 2,
              }}
            />

            <footer
              style={{
                padding: "24px 32px 32px",
                display: "grid",
                gridTemplateColumns: "1fr auto",
                gap: 20,
                alignItems: "flex-end",
                position: "relative",
                zIndex: 2,
              }}
            >
              <div>
                <div
                  className="h-mono"
                  style={{ fontSize: 11, letterSpacing: "0.22em", opacity: 0.85 }}
                >
                  WHEN
                </div>
                <div className="h-display" style={{ fontSize: 32, margin: "4px 0 12px" }}>
                  {formatHour(startH)} — {formatHour(endH)}
                </div>
                <div
                  className="h-mono"
                  style={{ fontSize: 11, letterSpacing: "0.22em", opacity: 0.85 }}
                >
                  WHO
                </div>
                <div style={{ fontSize: 18, fontWeight: 600 }}>Maya R. · maya@university.edu</div>
                {recurring !== "none" && (
                  <div
                    className="h-mono"
                    style={{
                      fontSize: 10,
                      letterSpacing: "0.18em",
                      marginTop: 6,
                      padding: "3px 8px",
                      background: "rgba(255,255,255,0.18)",
                      borderRadius: 999,
                      display: "inline-block",
                    }}
                  >
                    ↻ EVERY {date.toLocaleDateString("en", { weekday: "short" }).toUpperCase()} · 11 SESSIONS
                  </div>
                )}
                <div
                  className="h-mono"
                  style={{ fontSize: 10, letterSpacing: "0.18em", opacity: 0.7, marginTop: 14 }}
                >
                  {id}
                </div>
              </div>
              <QRCodePlaceholder />
            </footer>
          </article>

          <aside style={{ minWidth: 260, maxWidth: 320 }}>
            <div className="card" style={{ padding: 20, marginBottom: 12 }}>
              <span className="kicker">You're in.</span>
              <h3 className="h-headline" style={{ fontSize: 22, margin: "8px 0 8px" }}>
                Pass saved.
              </h3>
              <p style={{ color: "var(--ink-soft)", fontSize: 14, margin: 0 }}>
                Find this in your dashboard. Show the QR at the door — or just walk in, your face is already on the list.
              </p>
            </div>
            <div className="card" style={{ padding: 20 }}>
              <span className="label">Tip</span>
              <p style={{ fontSize: 14, margin: "8px 0 0" }}>
                Long-press to save the pass to Photos at 1080×1920. It's screenshot-shaped on purpose.
              </p>
            </div>
          </aside>
        </div>
      </div>

      <style>{`
        @media print {
          @page { margin: 0 }
          body { background: #fff }
          .nav, .container > .row, aside { display: none !important }
          .pass { box-shadow: none !important; max-width: 100% !important; aspect-ratio: 1080/1920 !important; height: 100vh; }
        }
        @media (max-width: 820px) {
          .pass-grid { grid-template-columns: 1fr !important; }
        }
      `}</style>
    </div>
  );
}

function QRCodePlaceholder() {
  const cells = Array.from({ length: 49 }, (_, i) => ((i * 31 + 7) % 11) > 5);
  [
    0, 1, 2, 7, 8, 9, 14, 15, 16, 4, 5, 6, 11, 12, 13, 18, 19, 20, 28, 29, 30, 35, 36, 37, 42, 43, 44,
  ].forEach((i) => (cells[i] = i % 3 !== 1));
  return (
    <div
      style={{
        background: "#fff",
        padding: 8,
        borderRadius: 10,
        width: 90,
        height: 90,
        display: "grid",
        gridTemplateColumns: "repeat(7, 1fr)",
        gap: 1,
      }}
    >
      {cells.map((on, i) => (
        <div key={i} style={{ background: on ? "#1b1b1f" : "transparent", borderRadius: 1 }} />
      ))}
    </div>
  );
}
