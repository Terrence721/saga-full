import { beforeEach, describe, expect, it, vi } from 'vitest'
import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { LoginForm } from './LoginForm'
import { AuthProvider } from '../context/AuthProvider'
import { useAuth } from '../context/useAuth'
import { login } from '../api/authClient'
import { ApiError } from '../api/httpClient'

vi.mock('../api/authClient')

// LoginForm never reads the token back out itself - it only calls auth.login() -
// so a small probe component is what actually proves AuthProvider's state updated,
// rather than just that login() got called.
function AuthProbe() {
  const { token } = useAuth()
  return <div data-testid="token">{token ?? 'no-token'}</div>
}

function renderLoginForm() {
  return render(
    <AuthProvider>
      <LoginForm />
      <AuthProbe />
    </AuthProvider>,
  )
}

describe('LoginForm', () => {
  beforeEach(() => {
    vi.mocked(login).mockReset()
  })

  it('logs the user in and updates auth state on success', async () => {
    vi.mocked(login).mockResolvedValue({ token: 'real-jwt-token', type: 'Bearer', expiresIn: 3600 })
    const user = userEvent.setup()
    renderLoginForm()

    await user.type(screen.getByPlaceholderText('Email'), 'cashier@test.local')
    await user.type(screen.getByPlaceholderText('Password'), 'Password123!')
    await user.click(screen.getByRole('button', { name: 'Log in' }))

    await waitFor(() => expect(screen.getByTestId('token')).toHaveTextContent('real-jwt-token'))
    expect(login).toHaveBeenCalledWith({ email: 'cashier@test.local', password: 'Password123!' })
  })

  it('shows the real API error message on a failed login, without logging in', async () => {
    vi.mocked(login).mockRejectedValue(new ApiError(401, { message: 'Invalid credentials' }))
    const user = userEvent.setup()
    renderLoginForm()

    await user.type(screen.getByPlaceholderText('Email'), 'cashier@test.local')
    await user.type(screen.getByPlaceholderText('Password'), 'wrong-password')
    await user.click(screen.getByRole('button', { name: 'Log in' }))

    expect(await screen.findByRole('alert')).toHaveTextContent('Invalid credentials')
    expect(screen.getByTestId('token')).toHaveTextContent('no-token')
  })

  it('falls back to a generic message for a non-ApiError failure', async () => {
    vi.mocked(login).mockRejectedValue(new Error('network down'))
    const user = userEvent.setup()
    renderLoginForm()

    await user.type(screen.getByPlaceholderText('Email'), 'cashier@test.local')
    await user.type(screen.getByPlaceholderText('Password'), 'Password123!')
    await user.click(screen.getByRole('button', { name: 'Log in' }))

    expect(await screen.findByRole('alert')).toHaveTextContent('Login failed')
  })
})
