import { describe, expect, it } from 'vitest'
import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { AuthProvider } from '../context/AuthProvider'
import { useAuth } from '../context/useAuth'

function makeToken(userId: string): string {
  const payload = btoa(JSON.stringify({ 'user-id': userId }))
    .replace(/\+/g, '-')
    .replace(/\//g, '_')
    .replace(/=+$/g, '')
  return `header.${payload}.signature`
}

function Consumer() {
  const { token, customerId, login, logout } = useAuth()
  return (
    <div>
      <span>token:{token ?? 'none'}</span>
      <span>customer:{customerId ?? 'none'}</span>
      <button type="button" onClick={() => login(makeToken('customer-1'))}>
        Login
      </button>
      <button type="button" onClick={logout}>
        Logout
      </button>
    </div>
  )
}

describe('useAuth', () => {
  it('throws when used outside of an AuthProvider', () => {
    expect(() => render(<Consumer />)).toThrow()
  })
})

describe('AuthProvider', () => {
  it('provides null token/customerId initially and renders children', () => {
    render(
      <AuthProvider>
        <Consumer />
      </AuthProvider>,
    )
    expect(screen.getByText('token:none')).toBeInTheDocument()
    expect(screen.getByText('customer:none')).toBeInTheDocument()
  })

  it('login stores the token and derives customerId from it', async () => {
    const user = userEvent.setup()
    render(
      <AuthProvider>
        <Consumer />
      </AuthProvider>,
    )

    await user.click(screen.getByRole('button', { name: 'Login' }))

    expect(screen.getByText(`token:${makeToken('customer-1')}`)).toBeInTheDocument()
    expect(screen.getByText('customer:customer-1')).toBeInTheDocument()
  })

  it('logout clears token and customerId', async () => {
    const user = userEvent.setup()
    render(
      <AuthProvider>
        <Consumer />
      </AuthProvider>,
    )

    await user.click(screen.getByRole('button', { name: 'Login' }))
    await user.click(screen.getByRole('button', { name: 'Logout' }))

    expect(screen.getByText('token:none')).toBeInTheDocument()
    expect(screen.getByText('customer:none')).toBeInTheDocument()
  })
})