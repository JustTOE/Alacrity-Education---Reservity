import { useQuery } from "@tanstack/react-query";
import { reservationsApi } from "@/api/reservations";

/**
 * Single reservation by uuid. The frontend route is `/pass/:id` where `:id`
 * is the reservation's UUID (not the human reservation_code).
 */
export function useReservation(id: string | undefined) {
  return useQuery({
    queryKey: ["reservation", id],
    queryFn: () => {
      if (!id) throw new Error("missing reservation id");
      return reservationsApi.get(id);
    },
    enabled: !!id,
  });
}

export function useReservationPass(id: string | undefined) {
  return useQuery({
    queryKey: ["reservation-pass", id],
    queryFn: () => {
      if (!id) throw new Error("missing reservation id");
      return reservationsApi.pass(id);
    },
    enabled: !!id,
  });
}
