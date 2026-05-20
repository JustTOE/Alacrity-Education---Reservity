import { useMutation, useQueryClient } from "@tanstack/react-query";
import { reservationsApi } from "@/api/reservations";
import type {
  ApiCreateReservationRequest,
  ApiReservationResponse,
} from "@/api/types";

export interface SubmitReservationVars {
  slugOrId: string;
  body: ApiCreateReservationRequest;
}

export function useSubmitReservation() {
  const qc = useQueryClient();
  return useMutation<ApiReservationResponse, Error, SubmitReservationVars>({
    mutationFn: ({ slugOrId, body }) => reservationsApi.submit(slugOrId, body),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ["availability:batch"] });
      qc.invalidateQueries({ queryKey: ["availability:range"] });
      qc.invalidateQueries({ queryKey: ["reservations", "mine"] });
      qc.invalidateQueries({ queryKey: ["space"] });
    },
  });
}
