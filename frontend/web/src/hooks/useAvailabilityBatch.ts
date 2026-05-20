import { useQueries } from "@tanstack/react-query";
import { useMemo } from "react";
import { spacesApi } from "@/api/spaces";
import type { ApiBatchAvailabilityResponse } from "@/api/types";
import {
  emptyLocalDay,
  localIsoDate,
  projectUtcDaysToLocal,
  utcIsoDate,
  type LocalDay,
} from "./localTime";

export { utcIsoDate, localIsoDate };

export interface LocalAvailabilityResult {
  date: string; // local YYYY-MM-DD
  spaces: Record<string, LocalDay>;
}

/**
 * Returns availability for a LOCAL date across many spaces. The backend
 * endpoint buckets by UTC days, so we fetch the two consecutive UTC days that
 * could overlap the local date and project the result onto the user's wall
 * clock.
 */
export function useAvailabilityBatch(
  slugs: string[],
  localDate: Date | null,
): { data: LocalAvailabilityResult | undefined; isLoading: boolean } {
  const sortedSlugs = useMemo(() => [...slugs].sort(), [slugs]);

  const localKey = localDate ? localIsoDate(localDate) : null;

  // Fetch the UTC date that contains LOCAL midnight, and the next UTC date.
  // Together they cover any local date regardless of zone offset.
  const utcDates = useMemo(() => {
    if (!localDate) return [];
    const localMid = new Date(
      localDate.getFullYear(),
      localDate.getMonth(),
      localDate.getDate(),
      0, 0, 0, 0,
    );
    const next = new Date(localMid.getTime() + 86_400_000);
    return [utcIsoDate(localMid), utcIsoDate(next)];
  }, [localDate]);

  const queries = useQueries({
    queries: utcDates.map((d) => ({
      queryKey: ["availability:batch", d, sortedSlugs] as const,
      queryFn: () => {
        if (sortedSlugs.length === 0) {
          return Promise.resolve({
            date: d,
            zone: "UTC",
            spaces: {} as Record<string, never>,
          } as ApiBatchAvailabilityResponse);
        }
        return spacesApi.availabilityBatch({ slugs: sortedSlugs, date: d });
      },
      staleTime: 30_000,
      refetchOnWindowFocus: true,
    })),
  });

  const isLoading = queries.some((q) => q.isLoading);

  const data = useMemo<LocalAvailabilityResult | undefined>(() => {
    if (!localKey) return undefined;
    if (queries.some((q) => !q.data)) return undefined;

    const spacesOut: Record<string, LocalDay> = {};
    for (const slug of sortedSlugs) {
      const utcDays = queries
        .map((q) => {
          const day = q.data?.spaces[slug];
          return day ?? null;
        })
        .filter((d): d is NonNullable<typeof d> => d !== null);
      const projected = projectUtcDaysToLocal(utcDays);
      spacesOut[slug] = projected.get(localKey) ?? emptyLocalDay(localKey);
    }
    return { date: localKey, spaces: spacesOut };
  }, [queries, localKey, sortedSlugs]);

  return { data, isLoading };
}
