import { useQuery } from "@tanstack/react-query";
import { spacesApi, summaryToSpace, type SpacesQuery } from "@/api/spaces";
import type { Space } from "@/types";

export function useSpaces(query: SpacesQuery = {}) {
  return useQuery({
    queryKey: ["spaces", query],
    queryFn: async () => {
      const page = await spacesApi.list(query);
      const spaces: Space[] = page.content.map(summaryToSpace);
      return { spaces, page };
    },
    staleTime: 30_000,
  });
}
