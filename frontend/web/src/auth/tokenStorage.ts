/**
 * Browser-localStorage wrapper for the JWT pair. Centralized so the api/ client
 * and auth/ context don't both reach into raw `localStorage`.
 */

const ACCESS_KEY = "rv-access-token";
const REFRESH_KEY = "rv-refresh-token";

export const tokenStorage = {
  getAccessToken(): string | null {
    if (typeof window === "undefined") return null;
    return window.localStorage.getItem(ACCESS_KEY);
  },
  getRefreshToken(): string | null {
    if (typeof window === "undefined") return null;
    return window.localStorage.getItem(REFRESH_KEY);
  },
  set(accessToken: string, refreshToken: string): void {
    window.localStorage.setItem(ACCESS_KEY, accessToken);
    window.localStorage.setItem(REFRESH_KEY, refreshToken);
  },
  clear(): void {
    window.localStorage.removeItem(ACCESS_KEY);
    window.localStorage.removeItem(REFRESH_KEY);
  },
  hasAny(): boolean {
    return this.getAccessToken() !== null || this.getRefreshToken() !== null;
  },
};
