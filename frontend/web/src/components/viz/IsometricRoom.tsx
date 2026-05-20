import type { Space } from "@/types";

interface IsometricRoomProps {
  space?: Space;
  width?: number;
  height?: number;
}

export default function IsometricRoom({ width = 360, height = 240 }: IsometricRoomProps) {
  return (
    <svg viewBox="0 0 360 240" width={width} height={height} aria-hidden="true">
      <g className="stroke stroke-mid">
        <path d="M70 160 L180 100 L290 160 L180 220 Z" />
        <path d="M70 160 L70 80 L180 20 L180 100 Z" />
        <path d="M180 100 L180 20 L290 80 L290 160 Z" />
        <path d="M130 130 L200 95 L250 120 L180 155 Z" />
        <path d="M130 130 L130 138 L180 163 L180 155" />
        <path d="M250 120 L250 128 L180 163" />
        <path d="M170 100 L210 80 L210 60 L170 80 Z" />
        <path d="M150 168 L185 152 L210 165 L175 181 Z" />
      </g>
      <g className="stroke stroke-thin" opacity="0.5">
        <line x1="14" y1="100" x2="70" y2="72" />
        <line x1="20" y1="98" x2="14" y2="102" />
      </g>
      <text
        x="42"
        y="92"
        fontFamily="JetBrains Mono"
        fontSize="9"
        fill="var(--indigo)"
        letterSpacing="0.12em"
        opacity="0.7"
      >
        2.4M
      </text>
    </svg>
  );
}
