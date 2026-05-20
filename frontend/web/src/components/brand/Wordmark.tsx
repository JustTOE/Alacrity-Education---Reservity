import Logomark from "./Logomark";

interface WordmarkProps {
  size?: number;
}

export default function Wordmark({ size = 22 }: WordmarkProps) {
  return (
    <div className="row" style={{ gap: 10 }}>
      <Logomark size={size + 6} />
      <span
        className="h-display"
        style={{ fontSize: size, fontWeight: 700, letterSpacing: "-0.02em" }}
      >
        Reservity
      </span>
    </div>
  );
}
