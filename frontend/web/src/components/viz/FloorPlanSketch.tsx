import type { Space } from "@/types";

interface FloorPlanSketchProps {
  space?: Pick<Space, "id" | "type" | "area" | "seats">;
  width?: number;
  height?: number;
}

export default function FloorPlanSketch({
  space,
  width = 280,
  height = 160,
}: FloorPlanSketchProps) {
  const seed = (space?.id || "").charCodeAt(0) || 7;
  const layout = seed % 3;
  return (
    <svg viewBox="0 0 280 160" width={width} height={height} aria-hidden="true">
      <g className="stroke stroke-mid">
        <rect x="14" y="14" width="252" height="132" />
        {layout === 0 && (
          <>
            <line x1="140" y1="14" x2="140" y2="86" />
            <line x1="140" y1="86" x2="266" y2="86" />
            <path d="M140 50 A 18 18 0 0 1 158 68" />
          </>
        )}
        {layout === 1 && (
          <>
            <line x1="14" y1="80" x2="180" y2="80" />
            <line x1="180" y1="80" x2="180" y2="146" />
            <path d="M140 80 A 18 18 0 0 1 158 98" />
          </>
        )}
        {layout === 2 && (
          <>
            <line x1="100" y1="14" x2="100" y2="146" />
            <line x1="100" y1="80" x2="266" y2="80" />
            <path d="M100 50 A 18 18 0 0 1 118 68" />
          </>
        )}
      </g>
      <g className="stroke stroke-thin" opacity="0.85">
        {space?.type === "lab" && (
          <>
            <rect x="30" y="30" width="50" height="14" />
            <rect x="30" y="50" width="50" height="14" />
            <rect x="160" y="30" width="50" height="14" />
            <circle cx="200" cy="110" r="10" />
            <circle cx="230" cy="110" r="10" />
          </>
        )}
        {space?.type === "pod" && (
          <>
            <rect x="40" y="40" width="60" height="20" />
            <circle cx="70" cy="80" r="8" />
          </>
        )}
        {space?.type === "open" && (
          <>
            <rect x="30" y="100" width="80" height="14" />
            <rect x="160" y="30" width="80" height="14" />
            <circle cx="50" cy="40" r="6" />
            <circle cx="70" cy="40" r="6" />
            <circle cx="190" cy="110" r="6" />
            <circle cx="210" cy="110" r="6" />
          </>
        )}
        {space?.type === "studio" && (
          <>
            <rect x="30" y="30" width="20" height="40" />
            <rect x="60" y="30" width="20" height="40" />
            <rect x="160" y="30" width="80" height="14" />
            <rect x="160" y="100" width="80" height="14" />
          </>
        )}
      </g>
      <g stroke="var(--indigo)" strokeWidth="1" opacity="0.5">
        <line x1="14" y1="154" x2="266" y2="154" />
        <line x1="14" y1="150" x2="14" y2="158" />
        <line x1="266" y1="150" x2="266" y2="158" />
      </g>
      <text
        x="140"
        y="160"
        fontFamily="JetBrains Mono"
        fontSize="8"
        fill="var(--indigo)"
        letterSpacing="0.1em"
        textAnchor="middle"
        opacity="0.7"
      >
        {space?.area || 24}M² · {space?.seats || 6} SEATS
      </text>
    </svg>
  );
}
