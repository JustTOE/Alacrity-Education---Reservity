import { Navigate, useNavigate, useParams } from "react-router-dom";
import BookingPass from "@/components/booking/BookingPass";
import { useApp } from "@/state/AppStore";
import { useReservationPass } from "@/hooks/useReservation";
import { responseToSpace } from "@/api/spaces";
import type { ApiPassResponse } from "@/api/types";
import type { ConfirmedBooking } from "@/types";
import { ApiError } from "@/api/client";

function passToBooking(p: ApiPassResponse): ConfirmedBooking {
  const space = responseToSpace(p.space);
  const start = new Date(p.startsAt);
  const end = new Date(p.endsAt);
  return {
    id: p.reservationCode,
    space,
    startH: start.getHours(),
    endH: end.getHours(),
    date: new Date(start.getFullYear(), start.getMonth(), start.getDate()),
    recurring: "none",
    confirmedAt: start,
    reservationId: p.id,
    reservationCode: p.reservationCode,
    passToken: p.qrPayload,
    status: "APPROVED",
  };
}

export default function PassRoute() {
  const { id } = useParams<{ id: string }>();
  const { showToast } = useApp();
  const navigate = useNavigate();
  const { data, isLoading, error } = useReservationPass(id);

  if (isLoading) {
    return (
      <div className="page-shell" style={{ padding: "4rem 2rem", textAlign: "center" }}>
        <p style={{ color: "var(--ink-3)" }}>Loading your pass…</p>
      </div>
    );
  }

  if (error) {
    if (error instanceof ApiError && error.status === 404) {
      return <Navigate to="/account" replace />;
    }
    return (
      <div className="page-shell" style={{ padding: "4rem 2rem", textAlign: "center" }}>
        <p style={{ color: "var(--coral, #d6644a)" }}>
          Couldn't load this pass. {error instanceof Error ? error.message : ""}
        </p>
      </div>
    );
  }

  if (!data) return <Navigate to="/account" replace />;

  const booking = passToBooking(data);

  return (
    <BookingPass
      booking={booking}
      onBack={() => navigate("/spaces")}
      onShare={() => showToast("Share link copied (stub).", "info")}
    />
  );
}
