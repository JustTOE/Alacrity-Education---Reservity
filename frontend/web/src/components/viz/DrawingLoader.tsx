interface DrawingLoaderProps {
  size?: number;
}

export default function DrawingLoader({ size = 64 }: DrawingLoaderProps) {
  return (
    <svg width={size} height={size} viewBox="0 0 64 64" aria-hidden="true">
      <style>{`
        .draw-path { stroke-dasharray: 200; stroke-dashoffset: 200; animation: draw 2s ease-in-out infinite; }
        @keyframes draw {
          0% { stroke-dashoffset: 200 }
          50% { stroke-dashoffset: 0 }
          100% { stroke-dashoffset: -200 }
        }
      `}</style>
      <g fill="none" stroke="var(--indigo)" strokeWidth="1.5" strokeLinecap="round">
        <path className="draw-path" d="M12 22 L32 12 L52 22 L52 46 L32 56 L12 46 Z" />
        <path
          className="draw-path"
          d="M32 12 L32 36 L52 46"
          style={{ animationDelay: "0.4s" }}
        />
        <path
          className="draw-path"
          d="M12 22 L32 36"
          style={{ animationDelay: "0.8s" }}
        />
      </g>
    </svg>
  );
}
