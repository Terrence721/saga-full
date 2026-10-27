import { AuthProvider } from './context/AuthProvider'
import { useAuth } from './context/useAuth'
import { LoginForm } from './components/LoginForm'
import { OrderEntryForm } from './components/OrderEntryForm'
import { AppShell } from './components/AppShell'

function AppContent() {
  const { token } = useAuth()

  if (!token) {
    return (
      <div className="flex min-h-screen items-center justify-center bg-slate-50 px-6">
        <LoginForm />
      </div>
    )
  }

  return (
    <AppShell>
      <OrderEntryForm />
    </AppShell>
  )
}

function App() {
  return (
    <AuthProvider>
      <AppContent />
    </AuthProvider>
  )
}

export default App
