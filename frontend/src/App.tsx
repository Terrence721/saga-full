import { AuthProvider } from './context/AuthProvider'
import { useAuth } from './context/useAuth'
import { LoginForm } from './components/LoginForm'

function AppContent() {
  const { token } = useAuth()

  if (!token) {
    return <LoginForm />
  }

  return (
    <div>
      <h1>Saga POS</h1>
      <p>Frontend scaffold running.</p>
    </div>
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
