import { createContext, useContext, useEffect, useMemo, useState } from 'react';
import { USE_MOCK_API } from '../config/api.js';
import { authService } from '../services/authService.js';

const AuthContext = createContext(null);

const clearStoredSession = () => {
  localStorage.removeItem('ssc_token');
  localStorage.removeItem('ssc_user');
};

const normalizeRole = (role) => String(role || 'RESIDENT').trim().toUpperCase();

const readUser = () => {
  try {
    const token = localStorage.getItem('ssc_token');
    if (!USE_MOCK_API && (!token || token.startsWith('mock-jwt-'))) {
      clearStoredSession();
      return null;
    }
    return JSON.parse(localStorage.getItem('ssc_user'));
  } catch {
    clearStoredSession();
    return null;
  }
};

export function AuthProvider({ children }) {
  const [user, setUser] = useState(readUser);
  const [loading, setLoading] = useState(false);

  useEffect(() => {
    const handleUnauthorized = () => setUser(null);
    window.addEventListener('ssc:unauthorized', handleUnauthorized);
    return () => window.removeEventListener('ssc:unauthorized', handleUnauthorized);
  }, []);

  useEffect(() => {
    if (!user?.id || USE_MOCK_API) return;
    authService.getProfile().then((profile) => {
      if (!profile) return;
      setUser((current) => {
        const next = { ...current, ...profile, role: normalizeRole(profile.role ?? current?.role) };
        localStorage.setItem('ssc_user', JSON.stringify(next));
        return next;
      });
    }).catch(() => { /* Keep the signed-in profile if the refresh is unavailable. */ });
  }, [user?.id]);

  const login = async (credentials) => {
    setLoading(true);
    try {
      const result = await authService.login(credentials);
      if (!result?.token || !result?.user) throw new Error('Backend login response is missing token or user data.');
      localStorage.setItem('ssc_token', result.token);
      localStorage.setItem('ssc_user', JSON.stringify(result.user));
      const nextUser = { ...result.user, role: normalizeRole(result.user.role) };
      localStorage.setItem('ssc_user', JSON.stringify(nextUser));
      setUser(nextUser);
      return nextUser;
    } finally {
      setLoading(false);
    }
  };

  const logout = async () => {
    try {
      await authService.logout();
    } catch {
      // The local session must still be cleared if the server is unavailable.
    } finally {
      clearStoredSession();
      setUser(null);
    }
  };

  const updateProfile = async (updates) => {
    const result = await authService.updateProfile(updates);
    const next = { ...user, ...result };
    localStorage.setItem('ssc_user', JSON.stringify(next));
    setUser(next);
    return next;
  };

  const uploadProfileImage = async (file) => {
    const result = await authService.uploadProfileImage(file);
    const next = { ...user, ...result };
    localStorage.setItem('ssc_user', JSON.stringify(next));
    setUser(next);
    return next;
  };

  const value = useMemo(() => ({ user, loading, login, logout, updateProfile, uploadProfileImage, isAuthenticated: Boolean(user) }), [user, loading]);
  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export const useAuth = () => {
  const context = useContext(AuthContext);
  if (!context) throw new Error('useAuth must be used inside AuthProvider');
  return context;
};
