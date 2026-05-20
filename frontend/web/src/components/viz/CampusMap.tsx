import type { Space } from "@/types";
import { getStatus } from "@/data/booking";

interface CampusMapProps {
  spaces: Space[];
  currentHour: number;
  selectedId: string | null;
  onSelect: (id: string) => void;
}

export default function CampusMap({
  spaces,
  currentHour,
  selectedId,
  onSelect,
}: CampusMapProps) {
  return (
    <svg
      viewBox="0 0 600 700"
      style={{ width: "100%", height: "100%", display: "block" }}
      aria-label="Campus map"
    >
      <defs>
        <pattern id="mapgrid" width="24" height="24" patternUnits="userSpaceOnUse">
          <path
            d="M 24 0 L 0 0 0 24"
            fill="none"
            stroke="rgba(75,82,167,0.08)"
            strokeWidth="1"
          />
        </pattern>
      </defs>
      <rect width="600" height="700" fill="#fbf8fe" />
      <rect width="600" height="700" fill="url(#mapgrid)" />

      <g className="stroke stroke-mid" opacity="0.85">
        <rect x="320" y="220" width="180" height="140" rx="4" fill="rgba(75,82,167,0.04)" />
        <rect x="340" y="240" width="60" height="40" />
        <rect x="420" y="240" width="60" height="40" />
        <rect x="340" y="300" width="140" height="50" />
        <rect x="180" y="290" width="160" height="120" rx="4" fill="rgba(75,82,167,0.04)" />
        <line x1="260" y1="290" x2="260" y2="410" />
        <rect x="120" y="170" width="160" height="100" rx="4" fill="rgba(75,82,167,0.04)" />
        <line x1="200" y1="170" x2="200" y2="270" />
        <rect x="380" y="380" width="180" height="120" rx="4" fill="rgba(75,82,167,0.04)" />
        <line x1="380" y1="440" x2="560" y2="440" />
      </g>

      <g
        stroke="rgba(75,82,167,0.35)"
        strokeWidth="1.2"
        strokeDasharray="3 4"
        fill="none"
      >
        <path d="M60 360 C 200 360, 280 360, 380 360" />
        <path d="M300 80 C 300 200, 300 350, 320 460" />
        <path d="M120 480 C 280 480, 420 480, 580 480" />
      </g>

      <g
        fontFamily="JetBrains Mono"
        fontSize="9"
        fill="var(--ink-mute)"
        letterSpacing="0.16em"
      >
        <text x="410" y="212" textAnchor="middle">
          HAWTHORN SCIENCES
        </text>
        <text x="260" y="282" textAnchor="middle">
          LINDEN HALL
        </text>
        <text x="200" y="162" textAnchor="middle">
          PAVILION NORTH
        </text>
        <text x="470" y="372" textAnchor="middle">
          MAGNOLIA ARTS
        </text>
      </g>

      {spaces.map((s) => {
        const status = getStatus(s, currentHour);
        const x = s.pin.x * 600;
        const y = s.pin.y * 700;
        const isSel = selectedId === s.id;
        const color =
          status.kind === "now"
            ? "#0f7a35"
            : status.kind === "soon"
              ? "#ff823c"
              : "#767683";
        return (
          <g
            key={s.id}
            transform={`translate(${x},${y})`}
            style={{ cursor: "pointer" }}
            onClick={() => onSelect(s.id)}
          >
            {isSel && <circle r="20" fill={color} opacity="0.18" />}
            <circle r={isSel ? 11 : 8} fill="#fff" stroke={color} strokeWidth="2" />
            <circle r={isSel ? 5 : 3.5} fill={color} />
            {isSel && (
              <g transform="translate(14,-22)">
                <rect
                  x="0"
                  y="0"
                  width={s.name.length * 6.5 + 16}
                  height="22"
                  rx="11"
                  fill="#1b1b1f"
                />
                <text
                  x="8"
                  y="14"
                  fontFamily="Work Sans"
                  fontSize="11"
                  fill="#fff"
                  fontWeight="600"
                >
                  {s.name}
                </text>
              </g>
            )}
          </g>
        );
      })}
    </svg>
  );
}
