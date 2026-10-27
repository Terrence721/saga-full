import { useState, type FormEvent } from 'react'
import { login } from '../api/authClient'
import { ApiError } from '../api/httpClient'
import { useAuth } from '../context/useAuth'
import { Button } from './ui/Button'
import { Card } from './ui/Card'
import { Input } from './ui/Input'

export function LoginForm() {
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [error, setError] = useState<string | null>(null)
  const auth = useAuth()

  async function handleSubmit(event: FormEvent) {
    event.preventDefault()
    setError(null)
    try {
      const response = await login({ email, password })
      auth.login(response.token)
    } catch (err) {
      setError(err instanceof ApiError ? err.message : 'Login failed')
    }
  }

  return (
    <Card className="w-full max-w-md">
      <div className="mb-6">
        <h1 className="text-2xl font-semibold text-slate-900">Sign in</h1>
        <p className="mt-1 text-sm text-slate-500">Cashier access to the register.</p>
      </div>
      <form onSubmit={handleSubmit} className="space-y-4">
        <Input
          type="email"
          placeholder="Email"
          value={email}
          onChange={(e) => setEmail(e.target.value)}
          required
        />
        <Input
          type="password"
          placeholder="Password"
          value={password}
          onChange={(e) => setPassword(e.target.value)}
          required
        />
        {error && <p role="alert" className="text-sm text-red-600">{error}</p>}
        <Button type="submit" className="w-full">
          Log in
        </Button>
      </form>
    </Card>
  )
}
