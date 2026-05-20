/**
 * Fetch wrapper for the Reservity backend.
 *
 * Responsibilities:
 *  - Inject `Authorization: Bearer <accessToken>` when present.
 *  - On 401, attempt a single-flight token refresh; retry the original call once.
 *  - On refresh failure, clear tokens and redirect to /login with a returnTo.
 *  - Surface server errors as `ApiError` with status + parsed body.
 *
 * No business logic lives here. Endpoint-specific helpers in
 * `api/auth.ts`, `api/spaces.ts`, etc. compose this.
 */

import { tokenStorage } from "@/auth/tokenStorage";

const API_BASE = "/api";

export class ApiError extends Error {
  constructor(
    public status: number,
    public body: unknown,
    message: string,
  ) {
    super(message);
    this.name = "ApiError";
  }
}

let refreshPromise: Promise<boolean> | null = null;

async function tryRefresh(): Promise<boolean> {
  if (refreshPromise) return refreshPromise;
  refreshPromise = doRefresh().finally(() => {
    refreshPromise = null;
  });
  return refreshPromise;
}

async function doRefresh(): Promise<boolean> {
  const refreshToken = tokenStorage.getRefreshToken();
  if (!refreshToken) return false;
  try {
    const res = await fetch(`${API_BASE}/auth/refresh`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ refreshToken }),
    });
    if (!res.ok) return false;
    const body = (await res.json()) as {
      accessToken: string;
      refreshToken: string;
    };
    tokenStorage.set(body.accessToken, body.refreshToken);
    return true;
  } catch {
    return false;
  }
}

function redirectToLogin() {
  if (typeof window === "undefined") return;
  const here = window.location.pathname + window.location.search;
  if (window.location.pathname === "/login") return;
  window.location.assign(`/login?returnTo=${encodeURIComponent(here)}`);
}

interface RequestOptions extends Omit<RequestInit, "body"> {
  body?: unknown;
  /** When true, do not redirect to /login on 401 — let the caller handle it. */
  skipAuthRedirect?: boolean;
}

async function request<T>(path: string, init: RequestOptions = {}): Promise<T> {
  const { body, skipAuthRedirect, headers, ...rest } = init;
  const token = tokenStorage.getAccessToken();
  const finalHeaders: Record<string, string> = {
    Accept: "application/json",
    ...(headers as Record<string, string> | undefined),
  };
  if (body !== undefined && !(body instanceof FormData)) {
    finalHeaders["Content-Type"] = "application/json";
  }
  if (token) finalHeaders["Authorization"] = `Bearer ${token}`;

  const res = await fetch(`${API_BASE}${path}`, {
    ...rest,
    headers: finalHeaders,
    body:
      body === undefined
        ? undefined
        : body instanceof FormData
          ? body
          : JSON.stringify(body),
  });

  if (res.status === 401 && token) {
    const refreshed = await tryRefresh();
    if (refreshed) {
      // retry once with the new token
      return request<T>(path, { ...init, skipAuthRedirect: true });
    }
    tokenStorage.clear();
    if (!skipAuthRedirect) redirectToLogin();
    throw new ApiError(401, null, "Session expired");
  }

  if (!res.ok) {
    let parsed: unknown = null;
    try {
      parsed = await res.json();
    } catch {
      // ignore non-JSON error bodies
    }
    const message =
      (parsed as { message?: string } | null)?.message ?? res.statusText;
    throw new ApiError(res.status, parsed, message);
  }

  if (res.status === 204) return undefined as T;
  // Handle empty bodies from POST/PATCH that return 200 with nothing
  const text = await res.text();
  if (!text) return undefined as T;
  return JSON.parse(text) as T;
}

export const api = {
  get: <T>(path: string, init?: RequestOptions) =>
    request<T>(path, { ...init, method: "GET" }),
  post: <T>(path: string, body?: unknown, init?: RequestOptions) =>
    request<T>(path, { ...init, method: "POST", body }),
  patch: <T>(path: string, body?: unknown, init?: RequestOptions) =>
    request<T>(path, { ...init, method: "PATCH", body }),
  put: <T>(path: string, body?: unknown, init?: RequestOptions) =>
    request<T>(path, { ...init, method: "PUT", body }),
  delete: <T>(path: string, init?: RequestOptions) =>
    request<T>(path, { ...init, method: "DELETE" }),
};
