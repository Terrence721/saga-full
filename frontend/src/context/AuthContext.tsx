import { useState, type ReactNode } from 'react'
import { AuthContext } from './authContext'

export function AuthProvider({ children }: { children: ReactNode }) {
  const [token, setToken] = useState<string | null>(null)

  return (
    <AuthContext.Provider value={{ token, login: setToken, logout: () => setToken(null) }}>
      {children}
    </AuthContext.Provider>
  )
}
