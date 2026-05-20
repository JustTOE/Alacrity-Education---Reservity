import { useEffect, useState } from "react";

/**
 * Returns a `Date` that ticks every 60 seconds while the document is visible.
 * Pauses on `visibilitychange → hidden`, force-syncs on return-to-visible.
 *
 * Replaces the deterministic `NOW = new Date(2026, 4, 2, 14, 12)` fixture
 * after the API swap.
 */
export function useNow(): {
  now: Date;
  currentHour: number;
  currentMinute: number;
} {
  const [now, setNow] = useState(() => new Date());

  useEffect(() => {
    let id: ReturnType<typeof setInterval> | null = null;

    const tick = () => setNow(new Date());

    const start = () => {
      tick();
      if (id !== null) clearInterval(id);
      id = setInterval(tick, 60_000);
    };
    const stop = () => {
      if (id !== null) {
        clearInterval(id);
        id = null;
      }
    };

    const onVis = () => {
      if (document.visibilityState === "visible") start();
      else stop();
    };

    if (document.visibilityState === "visible") start();
    document.addEventListener("visibilitychange", onVis);
    window.addEventListener("focus", tick);

    return () => {
      stop();
      document.removeEventListener("visibilitychange", onVis);
      window.removeEventListener("focus", tick);
    };
  }, []);

  return {
    now,
    currentHour: now.getHours(),
    currentMinute: now.getMinutes(),
  };
}
