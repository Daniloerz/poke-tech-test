import { createContext, useCallback, useContext, useEffect, useMemo, useState } from 'react';
import { login as loginRequest } from '../api/authApi.js';
import { setUnauthorizedHandler, tokenStorage } from '../api/client.js';

const AuthContext = createContext(null);

// Reads the username from the token payload (the "sub" claim). The backend verifies the token, not the browser.
function usernameFromToken(token) {
  try {
    const payload = token.split('.')[1].replace(/-/g, '+').replace(/_/g, '/');
    return JSON.parse(atob(payload)).sub;
  } catch {
    return null;
  }
}

export function AuthProvider({ children }) {
  const [token, setToken] = useState(() => tokenStorage.get());

  const login = useCallback(async (username, password) => {
    const { accessToken } = await loginRequest(username, password);
    tokenStorage.set(accessToken);
    setToken(accessToken);
  }, []);

  const logout = useCallback(() => {
    tokenStorage.clear();
    setToken(null);
  }, []);

  useEffect(() => {
    setUnauthorizedHandler(logout);
  }, [logout]);

  const value = useMemo(
    () => ({
      isAuthenticated: Boolean(token),
      username: token ? usernameFromToken(token) : null,
      login,
      logout,
    }),
    [token, login, logout],
  );

  return <AuthContext value={value}>{children}</AuthContext>;
}

export function useAuth() {
  return useContext(AuthContext);
}
