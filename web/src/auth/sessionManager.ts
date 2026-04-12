import type { User, AuthData } from '../types';

const TOKEN_KEY = 'rc_access_token';
const REFRESH_TOKEN_KEY = 'rc_refresh_token';
const USER_KEY = 'rc_user';

export const SessionManager = {
  // Save token
  saveToken: (token: string): void => {
    localStorage.setItem(TOKEN_KEY, token);
  },

  // Save refresh token
  saveRefreshToken: (refreshToken: string): void => {
    localStorage.setItem(REFRESH_TOKEN_KEY, refreshToken);
  },

  // Save user
  saveUser: (user: User): void => {
    localStorage.setItem(USER_KEY, JSON.stringify(user));
  },

  // Save auth data
  saveAuthData: (authData: AuthData): void => {
    SessionManager.saveToken(authData.token);
    if (authData.refreshToken) {
      SessionManager.saveRefreshToken(authData.refreshToken);
    }
    SessionManager.saveUser(authData.user);
  },

  // Get token
  getToken: (): string | null => {
    return localStorage.getItem(TOKEN_KEY);
  },

  // Get refresh token
  getRefreshToken: (): string | null => {
    return localStorage.getItem(REFRESH_TOKEN_KEY);
  },

  // Get user
  getUser: (): User | null => {
    const userStr = localStorage.getItem(USER_KEY);
    if (!userStr) return null;
    try {
      return JSON.parse(userStr);
    } catch {
      return null;
    }
  },

  // Check if logged in
  isLoggedIn: (): boolean => {
    return SessionManager.getToken() !== null && SessionManager.getUser() !== null;
  },

  // Clear session
  clear: (): void => {
    localStorage.removeItem(TOKEN_KEY);
    localStorage.removeItem(REFRESH_TOKEN_KEY);
    localStorage.removeItem(USER_KEY);
  },
};
