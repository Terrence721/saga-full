import { type ReactNode } from 'react'
import { useAuth } from '../context/useAuth'
import { Button } from './ui/Button'

export function AppShell({ children }: { children: ReactNode }) {
  const { logout } = useAuth()

  return (
    <div className="min-h-screen bg-slate-50">
      <header className="border-b border-slate-200 bg-white">
        <div className="mx-auto flex max-w-5xl items-center justify-between px-6 py-4">
          <div className="flex items-center gap-3">
            <span className="inline-flex h-9 w-9 items-center justify-center rounded-xl bg-indigo-600 text-sm font-bold text-white">
              S
            </span>
            <div>
              <p className="text-sm font-semibold text-slate-900">Saga POS</p>
              <p className="text-xs text-slate-500">Register / order entry</p>
            </div>
          </div>
          <Button variant="ghost" size="sm" onClick={logout}>
            Log out
          </Button>
        </div>
      </header>
      <main className="mx-auto max-w-5xl px-6 py-8">{children}</main>
    </div>
  )
}
