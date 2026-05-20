import { api } from "./client";
import type {
  ApiAuthResponse,
  ApiLoginRequest,
  ApiRegisterRequest,
  ApiUserResponse,
} from "./types";

export const authApi = {
  register: (req: ApiRegisterRequest) =>
    api.post<ApiAuthResponse>("/auth/register", req, { skipAuthRedirect: true }),

  login: (req: ApiLoginRequest) =>
    api.post<ApiAuthResponse>("/auth/login", req, { skipAuthRedirect: true }),

  logout: (refreshToken: string | null) =>
    api.post<void>(
      "/auth/logout",
      refreshToken ? { refreshToken } : null,
      { skipAuthRedirect: true },
    ),

  me: () => api.get<ApiUserResponse>("/auth/me", { skipAuthRedirect: true }),
};
