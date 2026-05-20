import { useQuery } from "@tanstack/react-query";
import { responseToSpace, spacesApi } from "@/api/spaces";

export function useSpace(slugOrId: string | undefined) {
  return useQuery({
    queryKey: ["space", slugOrId],
    queryFn: async () => {
      if (!slugOrId) throw new Error("missing slug");
      const resp = await spacesApi.get(slugOrId);
      return responseToSpace(resp);
    },
    enabled: !!slugOrId,
    staleTime: 60_000,
  });
}
