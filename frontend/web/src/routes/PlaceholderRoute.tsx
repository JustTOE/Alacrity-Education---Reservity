interface PlaceholderRouteProps {
  title: string;
  body?: string;
}

export default function PlaceholderRoute({ title, body }: PlaceholderRouteProps) {
  return (
    <main className="container" style={{ padding: "96px 32px", minHeight: "60vh" }}>
      <p className="kicker">Coming next milestone</p>
      <h1 className="h-display" style={{ fontSize: 64, margin: "8px 0 16px" }}>
        {title}
      </h1>
      <p style={{ color: "var(--ink-soft)", fontSize: 17, maxWidth: 520 }}>
        {body ?? "This section is scaffolded but not yet implemented."}
      </p>
    </main>
  );
}
