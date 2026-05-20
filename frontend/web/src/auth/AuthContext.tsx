import {
  createContext,
  useCallback,
  useContext,
  useEffect,
  useMemo,
  useState,
  type ReactNode,
} from "react";
import { useQueryClient } from "@tanstack/react-query";
import { authApi } from "@/api/auth";
import { ApiError } from "@/api/client";
import type { ApiUserResponse } from "@/api/types";
import { tokenStorage } from "./tokenStorage";

export type AuthStatus = "loading" | "unauthenticated" | "authenticated";

interface AuthContextValue {
  status: AuthStatus;
  user: ApiUserResponse | null;
  login: (email: string, password: string) => Promise<ApiUserResponse>;
  register: (req: {
    email: string;
    password: string;
    displayName: string;
    handle: string;
  }) => Promise<ApiUserResponse>;
  logout: () => Promise<void>;
  refreshMe: () => Promise<void>;
}

const AuthContext = createContext<AuthContextValue | null>(null);

export function useAuth(): AuthContextValue {
  const ctx = useContext(AuthContext);
  if (!ctx) throw new Error("useAuth must be used within AuthProvider");
  return ctx;
}

export function AuthProvider({ children }: { children: ReactNode }) {
  const [status, setStatus] = useState<AuthStatus>("loading");
  const [user, setUser] = useState<ApiUserResponse | null>(null);
  const queryClient = useQueryClient();

  const refreshMe = useCallback(async () => {
    const token = tokenStorage.getAccessToken();
    if (!token) {
      setStatus("unauthenticated");
      setUser(null);
      return;
    }
    try {
      const me = await authApi.me();
      setUser(me);
      setStatus("authenticated");
    } catch (err) {
      if (err instanceof ApiError && (err.status === 401 || err.status === 403)) {
        tokenStorage.clear();
        setUser(null);
        setStatus("unauthenticated");
        return;
      }
      // network error etc — keep tokens, treat as unauth for now
      setStatus("unauthenticated");
      setUser(null);
    }
  }, []);

  useEffect(() => {
    refreshMe();
  }, [refreshMe]);

  const login = useCallback(
    async (email: string, password: string) => {
      const resp = await authApi.login({ email, password });
      tokenStorage.set(resp.accessToken, resp.refreshToken);
      setUser(resp.user);
      setStatus("authenticated");
      // Auth boundary changed — clear cached server data so queries refetch.
      queryClient.clear();
      return resp.user;
    },
    [queryClient],
  );

  const register = useCallback(
    async (req: {
      email: string;
      password: string;
      displayName: string;
      handle: string;
    }) => {
      const resp = await authApi.register(req);
      tokenStorage.set(resp.accessToken, resp.refreshToken);
      setUser(resp.user);
      setStatus("authenticated");
      queryClient.clear();
      return resp.user;
    },
    [queryClient],
  );

  const logout = useCallback(async () => {
    const refresh = tokenStorage.getRefreshToken();
    try {
      await authApi.logout(refresh);
    } catch {
      // Even if backend call fails, sign out locally.
    }
    tokenStorage.clear();
    setUser(null);
    setStatus("unauthenticated");
    queryClient.clear();
  }, [queryClient]);

  const value = useMemo(
    () => ({ status, user, login, register, logout, refreshMe }),
    [status, user, login, register, logout, refreshMe],
  );

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}
