interface SparklineProps {
  booked?: number[];
  startHour?: number;
  hours?: number;
  width?: number;
  height?: number;
}

export default function Sparkline({
  booked = [],
  startHour = 8,
  hours = 12,
  width = 96,
  height = 24,
}: SparklineProps) {
  const barW = (width - (hours - 1) * 2) / hours;
  return (
    <svg
      width={width}
      height={height}
      viewBox={`0 0 ${width} ${height}`}
      aria-label="Next 12 hours availability"
    >
      {Array.from({ length: hours }).map((_, i) => {
        const hr = startHour + i;
        const isBooked = booked.includes(hr);
        return (
          <rect
            key={i}
            x={i * (barW + 2)}
            y={2}
            width={barW}
            height={height - 4}
            rx="1.5"
            fill={isBooked ? "var(--indigo)" : "rgba(75,82,167,0.18)"}
          />
        );
      })}
    </svg>
  );
}
