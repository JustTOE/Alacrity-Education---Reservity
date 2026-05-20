import { useQuery } from "@tanstack/react-query";
import { buildingsApi } from "@/api/buildings";

export function useBuildings() {
  return useQuery({
    queryKey: ["buildings"],
    queryFn: () => buildingsApi.list(),
    staleTime: 5 * 60_000, // building list rarely changes
  });
}
