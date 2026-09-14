import { useState, type ReactNode } from 'react'
import { AuthContext } from './AuthContext'
import { getUserIdFromToken } from '../api/jwt'

export function AuthProvider({ children }: { children: ReactNode }) {
  const [token, setToken] = useState<string | null>(null)
  const [customerId, setCustomerId] = useState<string | null>(null)

  function login(newToken: string) {
    setToken(newToken)
    setCustomerId(getUserIdFromToken(newToken))
  }

  function logout() {
    setToken(null)
    setCustomerId(null)
  }

  return (
    <AuthContext.Provider value={{ token, customerId, login, logout }}>
      {children}
    </AuthContext.Provider>
  )
}
