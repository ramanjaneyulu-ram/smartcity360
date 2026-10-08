import { createContext, useContext, useState } from 'react';
import client from '../api/client';

const AuthContext = createContext(null);

export function AuthProvider({ children }) {
  const [user, setUser] = useState(() => {
    const raw = localStorage.getItem('sc360_user');
    return raw ? JSON.parse(raw) : null;
  });

  async function login(email, password) {
    const { data } = await client.post('/auth/login', { email, password });
    persist(data);
  }

  async function register(payload) {
    const { data } = await client.post('/auth/register', payload);
    persist(data);
  }

  function persist(data) {
    localStorage.setItem('sc360_token', data.token);
    const u = { id: data.userId, name: data.name, email: data.email, role: data.role, department: data.department };
    localStorage.setItem('sc360_user', JSON.stringify(u));
    setUser(u);
  }

  function logout() {
    localStorage.removeItem('sc360_token');
    localStorage.removeItem('sc360_user');
    setUser(null);
  }

  return (
    <AuthContext.Provider value={{ user, login, register, logout }}>
      {children}
    </AuthContext.Provider>
  );
}

export function useAuth() {
  return useContext(AuthContext);
}
