import { createContext, useContext, useState, useEffect } from 'react';

const AuthContext = createContext(null);

export function AuthProvider({ children }) {
  const [auth, setAuth] = useState(() => {
    const stored = localStorage.getItem('sam_auth');
    return stored ? JSON.parse(stored) : null;
  });

  const login = (data) => {
    localStorage.setItem('sam_auth', JSON.stringify(data));
    setAuth(data);
  };
  
  const logout = () => {
    localStorage.removeItem('sam_auth');
    setAuth(null);
  };
  const isAuthenticated = !!auth?.token;
  const perfil = auth?.perfil || null;
  const email = auth?.email || null;

  return (
    <AuthContext.Provider value={{ auth, login, logout, isAuthenticated, perfil, email }}>
      {children}
    </AuthContext.Provider>
  );
}

export const useAuth = () => useContext(AuthContext);
