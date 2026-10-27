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
  const [showPassword, setShowPassword] = useState(false)
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
        <div className="relative">
          <Input
            type={showPassword ? 'text' : 'password'}
            placeholder="Password"
            value={password}
            onChange={(e) => setPassword(e.target.value)}
            required
            className="pr-16"
          />
          <button
            type="button"
            onClick={() => setShowPassword((v) => !v)}
            className="absolute right-2 top-1/2 -translate-y-1/2 rounded-lg px-2 py-1 text-xs font-medium text-slate-500 hover:bg-slate-100 hover:text-slate-700"
            aria-label={showPassword ? 'Hide password' : 'Show password'}
          >
            {showPassword ? 'Hide' : 'Show'}
          </button>
        </div>
        {error && <p role="alert" className="text-sm text-red-600">{error}</p>}
        <Button type="submit" className="w-full">
          Log in
        </Button>
      </form>
    </Card>
  )
}
