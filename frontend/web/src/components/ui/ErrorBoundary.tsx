import { Component } from "react";
import type { ErrorInfo, ReactNode } from "react";

interface ErrorBoundaryProps {
  children: ReactNode;
}

interface ErrorBoundaryState {
  error: Error | null;
}

export default class ErrorBoundary extends Component<ErrorBoundaryProps, ErrorBoundaryState> {
  state: ErrorBoundaryState = { error: null };

  static getDerivedStateFromError(error: Error): ErrorBoundaryState {
    return { error };
  }

  componentDidCatch(error: Error, info: ErrorInfo) {
    console.error("[Reservity] uncaught error:", error, info);
  }

  reset = () => this.setState({ error: null });

  render() {
    if (!this.state.error) return this.props.children;
    return (
      <div
        style={{
          padding: "96px 32px",
          maxWidth: 720,
          margin: "0 auto",
          minHeight: "60vh",
        }}
      >
        <p className="kicker">Something snapped</p>
        <h1 className="h-display" style={{ fontSize: 56, margin: "8px 0 16px" }}>
          That wasn't supposed to happen.
        </h1>
        <p style={{ color: "var(--ink-soft)", fontSize: 17, marginBottom: 24 }}>
          The page hit an error and stopped rendering. Reload to try again, or go back home.
        </p>
        <pre
          style={{
            background: "var(--paper-2)",
            padding: 16,
            borderRadius: 10,
            fontSize: 12,
            color: "var(--ink-soft)",
            overflowX: "auto",
            border: "1px solid var(--line-soft)",
            margin: "0 0 24px",
          }}
        >
          {this.state.error.message}
        </pre>
        <div className="row gap-2">
          <button className="btn btn-primary" onClick={() => window.location.reload()}>
            Reload page
          </button>
          <button
            className="btn btn-ghost"
            onClick={() => {
              this.reset();
              window.location.assign("/");
            }}
          >
            Go home
          </button>
        </div>
      </div>
    );
  }
}
