interface VerbUnderlineProps {
  color?: string;
  stroke?: number;
}

export default function VerbUnderline({
  color = "#ff823c",
  stroke = 4,
}: VerbUnderlineProps) {
  return (
    <svg viewBox="0 0 200 18" preserveAspectRatio="none" aria-hidden="true">
      <path
        d="M2 12 C 40 4, 90 4, 130 10 S 195 14, 198 8"
        stroke={color}
        strokeWidth={stroke}
        fill="none"
        strokeLinecap="round"
      />
    </svg>
  );
}
