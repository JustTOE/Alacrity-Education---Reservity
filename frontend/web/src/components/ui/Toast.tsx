import { useApp } from "@/state/AppStore";

export default function Toast() {
  const { state } = useApp();
  if (!state.toast) return null;
  const { msg, kind } = state.toast;
  const bg =
    kind === "success"
      ? "linear-gradient(135deg,#0f7a35,#4b52a7)"
      : kind === "error"
        ? "linear-gradient(135deg,#9f4200,#ff823c)"
        : undefined;
  return (
    <div className="toast" style={bg ? { background: bg } : undefined}>
      {msg}
    </div>
  );
}
