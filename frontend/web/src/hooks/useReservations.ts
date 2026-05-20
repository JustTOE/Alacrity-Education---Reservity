import { useQuery } from "@tanstack/react-query";
import { reservationsApi } from "@/api/reservations";
import { useAuth } from "@/auth/AuthContext";

export function useReservations(params: { upcoming?: boolean } = {}) {
  const auth = useAuth();
  const enabled = auth.status === "authenticated";
  return useQuery({
    queryKey: ["reservations", "mine", params],
    queryFn: () => reservationsApi.mine({ ...params, page: 0, size: 50 }),
    enabled,
    staleTime: 30_000,
  });
}
