interface LogomarkProps {
  size?: number;
  color?: string;
  accent?: string;
  detail?: boolean;
}

export default function Logomark({
  size = 28,
  color = "var(--indigo)",
  accent = "#ff823c",
  detail = true,
}: LogomarkProps) {
  return (
    <svg width={size} height={size} viewBox="0 0 32 32" aria-hidden="true">
      <g fill="none" stroke={color} strokeWidth="1.5" strokeLinecap="round" strokeLinejoin="round">
        <path d="M6 11 L16 6 L26 11 L26 23 L16 28 L6 23 Z" />
        <path d="M16 6 L16 18 L26 23" />
        <path d="M6 11 L16 18" />
        {detail && (
          <>
            <path d="M11 14.5 L11 21.5" opacity=".45" />
            <path d="M21 14.5 L21 21.5" opacity=".45" />
          </>
        )}
        <circle cx="16" cy="18" r="0.9" fill={accent} stroke="none" />
      </g>
    </svg>
  );
}
