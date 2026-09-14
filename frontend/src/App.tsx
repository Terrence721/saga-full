import { AuthProvider } from './context/AuthProvider'
import { useAuth } from './context/useAuth'
import { LoginForm } from './components/LoginForm'
import { OrderEntryForm } from './components/OrderEntryForm'

function AppContent() {
  const { token } = useAuth()

  if (!token) {
    return <LoginForm />
  }

  return (
    <div>
      <h1>Saga POS</h1>
      <OrderEntryForm />
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
