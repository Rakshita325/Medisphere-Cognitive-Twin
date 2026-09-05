import React, { createContext, useContext, useState, useCallback } from 'react';
import { getCurrentUser, clearAuth, loginUser, signupUser } from './api';

const AuthContext = createContext(null);

export function AuthProvider({ children }) {
  const [user, setUser] = useState(() => getCurrentUser());

  const login = useCallback(async (username, password) => {
    const userProfile = await loginUser(username, password);
    setUser(userProfile);
    return userProfile;
  }, []);

  const signup = useCallback(async (fullName, username, password, role) => {
    const createdUser = await signupUser(fullName, username, password, role);
    return createdUser;
  }, []);

  const logout = useCallback(() => {
    clearAuth();
    setUser(null);
  }, []);

  const isAuthenticated = !!user;

  const hasRole = useCallback((roles) => {
    if (!user || !user.role) return false;
    if (typeof roles === 'string') return user.role === roles;
    return roles.includes(user.role);
  }, [user]);

  return (
    <AuthContext.Provider value={{ user, isAuthenticated, login, signup, logout, hasRole }}>
      {children}
    </AuthContext.Provider>
  );
}

export function useAuth() {
  const context = useContext(AuthContext);
  if (!context) {
    throw new Error('useAuth must be used within an AuthProvider');
  }
  return context;
}
