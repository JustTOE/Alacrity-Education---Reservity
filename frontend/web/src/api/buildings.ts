import { api } from "./client";
import type { ApiBuildingResponse } from "./types";

export const buildingsApi = {
  list: () => api.get<ApiBuildingResponse[]>("/buildings"),
};
