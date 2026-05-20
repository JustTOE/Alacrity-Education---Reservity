/* global React */
const { useState, useEffect, useMemo, useRef, useCallback } = React;

// ---------- Logomark ----------
function Logomark({ size = 28, color = 'var(--indigo)', accent = '#ff823c', detail = true }) {
  return (
    <svg width={size} height={size} viewBox="0 0 32 32" aria-hidden="true">
      <g fill="none" stroke={color} strokeWidth="1.5" strokeLinecap="round" strokeLinejoin="round">
        <path d="M6 11 L16 6 L26 11 L26 23 L16 28 L6 23 Z" />
        <path d="M16 6 L16 18 L26 23" />
        <path d="M6 11 L16 18" />
        {detail && <>
          <path d="M11 14.5 L11 21.5" opacity=".45" />
          <path d="M21 14.5 L21 21.5" opacity=".45" />
        </>}
        <circle cx="16" cy="18" r="0.9" fill={accent} stroke="none" />
      </g>
    </svg>
  );
}

function Wordmark({ size = 22 }) {
  return (
    <div className="row" style={{ gap: 10 }}>
      <Logomark size={size + 6} />
      <span className="h-display" style={{ fontSize: size, fontWeight: 700, letterSpacing: '-0.02em' }}>Reservity</span>
    </div>
  );
}

// Hand-drawn underline accent
function VerbUnderline({ color = '#ff823c', stroke = 4 }) {
  return (
    <svg viewBox="0 0 200 18" preserveAspectRatio="none" aria-hidden="true">
      <path d="M2 12 C 40 4, 90 4, 130 10 S 195 14, 198 8" stroke={color} strokeWidth={stroke} fill="none" strokeLinecap="round" />
    </svg>
  );
}

// 12-bar availability sparkline
function Sparkline({ booked = [], startHour = 8, hours = 12, width = 96, height = 24 }) {
  const barW = (width - (hours - 1) * 2) / hours;
  return (
    <svg width={width} height={height} viewBox={`0 0 ${width} ${height}`} aria-label="Next 12 hours availability">
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
            fill={isBooked ? 'var(--indigo)' : 'rgba(75,82,167,0.18)'}
          />
        );
      })}
    </svg>
  );
}

// ---------- Status logic ----------
function getStatus(space, currentHour) {
  if (space.fullyBooked) return { kind: 'booked', label: 'Booked till tomorrow', cls: 'pill-booked' };
  const isBookedNow = space.booked.includes(currentHour);
  if (!isBookedNow) return { kind: 'now', label: 'Available now', cls: 'pill-now' };
  // find next free
  let h = currentHour;
  while (space.booked.includes(h) && h < 23) h++;
  if (h - currentHour <= 1) return { kind: 'soon', label: 'Free in 30 min', cls: 'pill-soon' };
  return { kind: 'booked', label: `Booked till ${formatHour(h)}`, cls: 'pill-booked' };
}

function nextFreeHour(space, currentHour) {
  if (space.fullyBooked) return null;
  let h = currentHour;
  if (!space.booked.includes(h)) return h;
  while (space.booked.includes(h) && h < 23) h++;
  return h;
}

function formatHour(h) {
  if (h === 0) return '12am';
  if (h === 12) return 'noon';
  if (h < 12) return `${h}am`;
  return `${h - 12}pm`;
}

// ---------- Line-work scenes ----------
function FloorPlanSketch({ space, width = 280, height = 160 }) {
  // Deterministic mini floor plan based on space type
  const seed = (space?.id || '').charCodeAt(0) || 7;
  const layout = (seed % 3);
  return (
    <svg viewBox="0 0 280 160" width={width} height={height} aria-hidden="true">
      <g className="stroke stroke-mid">
        <rect x="14" y="14" width="252" height="132" />
        {layout === 0 && <>
          <line x1="140" y1="14" x2="140" y2="86" />
          <line x1="140" y1="86" x2="266" y2="86" />
          <path d="M140 50 A 18 18 0 0 1 158 68" />
        </>}
        {layout === 1 && <>
          <line x1="14" y1="80" x2="180" y2="80" />
          <line x1="180" y1="80" x2="180" y2="146" />
          <path d="M140 80 A 18 18 0 0 1 158 98" />
        </>}
        {layout === 2 && <>
          <line x1="100" y1="14" x2="100" y2="146" />
          <line x1="100" y1="80" x2="266" y2="80" />
          <path d="M100 50 A 18 18 0 0 1 118 68" />
        </>}
      </g>
      <g className="stroke stroke-thin" opacity="0.85">
        {/* furniture seeds based on type */}
        {space?.type === 'lab' && <>
          <rect x="30" y="30" width="50" height="14" />
          <rect x="30" y="50" width="50" height="14" />
          <rect x="160" y="30" width="50" height="14" />
          <circle cx="200" cy="110" r="10" />
          <circle cx="230" cy="110" r="10" />
        </>}
        {space?.type === 'pod' && <>
          <rect x="40" y="40" width="60" height="20" />
          <circle cx="70" cy="80" r="8" />
        </>}
        {space?.type === 'open' && <>
          <rect x="30" y="100" width="80" height="14" />
          <rect x="160" y="30" width="80" height="14" />
          <circle cx="50" cy="40" r="6" />
          <circle cx="70" cy="40" r="6" />
          <circle cx="190" cy="110" r="6" />
          <circle cx="210" cy="110" r="6" />
        </>}
        {space?.type === 'studio' && <>
          <rect x="30" y="30" width="20" height="40" />
          <rect x="60" y="30" width="20" height="40" />
          <rect x="160" y="30" width="80" height="14" />
          <rect x="160" y="100" width="80" height="14" />
        </>}
      </g>
      {/* dim line */}
      <g stroke="var(--indigo)" strokeWidth="1" opacity="0.5">
        <line x1="14" y1="154" x2="266" y2="154" />
        <line x1="14" y1="150" x2="14" y2="158" />
        <line x1="266" y1="150" x2="266" y2="158" />
      </g>
      <text x="140" y="160" fontFamily="JetBrains Mono" fontSize="8" fill="var(--indigo)" letterSpacing="0.1em" textAnchor="middle" opacity="0.7">
        {space?.area || 24}M² · {space?.seats || 6} SEATS
      </text>
    </svg>
  );
}

function IsometricRoom({ space, width = 360, height = 240 }) {
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
      <text x="42" y="92" fontFamily="JetBrains Mono" fontSize="9" fill="var(--indigo)" letterSpacing="0.12em" opacity="0.7">2.4M</text>
    </svg>
  );
}

// Map placeholder (architectural campus drawing)
function CampusMap({ spaces, currentHour, selectedId, onSelect }) {
  return (
    <svg viewBox="0 0 600 700" style={{ width: '100%', height: '100%', display: 'block' }} aria-label="Campus map">
      <defs>
        <pattern id="mapgrid" width="24" height="24" patternUnits="userSpaceOnUse">
          <path d="M 24 0 L 0 0 0 24" fill="none" stroke="rgba(75,82,167,0.08)" strokeWidth="1" />
        </pattern>
      </defs>
      <rect width="600" height="700" fill="#fbf8fe" />
      <rect width="600" height="700" fill="url(#mapgrid)" />

      {/* Buildings */}
      <g className="stroke stroke-mid" opacity="0.85">
        {/* Hawthorn Sciences */}
        <rect x="320" y="220" width="180" height="140" rx="4" fill="rgba(75,82,167,0.04)" />
        <rect x="340" y="240" width="60" height="40" />
        <rect x="420" y="240" width="60" height="40" />
        <rect x="340" y="300" width="140" height="50" />
        {/* Linden Hall */}
        <rect x="180" y="290" width="160" height="120" rx="4" fill="rgba(75,82,167,0.04)" />
        <line x1="260" y1="290" x2="260" y2="410" />
        {/* Pavilion North */}
        <rect x="120" y="170" width="160" height="100" rx="4" fill="rgba(75,82,167,0.04)" />
        <line x1="200" y1="170" x2="200" y2="270" />
        {/* Magnolia Arts */}
        <rect x="380" y="380" width="180" height="120" rx="4" fill="rgba(75,82,167,0.04)" />
        <line x1="380" y1="440" x2="560" y2="440" />
      </g>

      {/* paths */}
      <g stroke="rgba(75,82,167,0.35)" strokeWidth="1.2" strokeDasharray="3 4" fill="none">
        <path d="M60 360 C 200 360, 280 360, 380 360" />
        <path d="M300 80 C 300 200, 300 350, 320 460" />
        <path d="M120 480 C 280 480, 420 480, 580 480" />
      </g>

      {/* Building labels */}
      <g fontFamily="JetBrains Mono" fontSize="9" fill="var(--ink-mute)" letterSpacing="0.16em">
        <text x="410" y="212" textAnchor="middle">HAWTHORN SCIENCES</text>
        <text x="260" y="282" textAnchor="middle">LINDEN HALL</text>
        <text x="200" y="162" textAnchor="middle">PAVILION NORTH</text>
        <text x="470" y="372" textAnchor="middle">MAGNOLIA ARTS</text>
      </g>

      {/* Pins */}
      {spaces.map(s => {
        const status = getStatus(s, currentHour);
        const x = s.pin.x * 600;
        const y = s.pin.y * 700;
        const isSel = selectedId === s.id;
        const color = status.kind === 'now' ? '#0f7a35' : status.kind === 'soon' ? '#ff823c' : '#767683';
        return (
          <g key={s.id} transform={`translate(${x},${y})`} style={{ cursor: 'pointer' }} onClick={() => onSelect(s.id)}>
            {isSel && <circle r="20" fill={color} opacity="0.18" />}
            <circle r={isSel ? 11 : 8} fill="#fff" stroke={color} strokeWidth="2" />
            <circle r={isSel ? 5 : 3.5} fill={color} />
            {isSel && (
              <g transform="translate(14,-22)">
                <rect x="0" y="0" width={s.name.length * 6.5 + 16} height="22" rx="11" fill="#1b1b1f" />
                <text x="8" y="14" fontFamily="Work Sans" fontSize="11" fill="#fff" fontWeight="600">{s.name}</text>
              </g>
            )}
          </g>
        );
      })}
    </svg>
  );
}

// ---------- Animated drawing line (loading state) ----------
function DrawingLoader({ size = 64 }) {
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
        <path className="draw-path" d="M32 12 L32 36 L52 46" style={{ animationDelay: '0.4s' }}/>
        <path className="draw-path" d="M12 22 L32 36" style={{ animationDelay: '0.8s' }}/>
      </g>
    </svg>
  );
}

// Export all
Object.assign(window, {
  Logomark, Wordmark, VerbUnderline, Sparkline,
  FloorPlanSketch, IsometricRoom, CampusMap, DrawingLoader,
  getStatus, nextFreeHour, formatHour,
});
