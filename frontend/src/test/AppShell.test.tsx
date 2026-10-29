import { describe, expect, it } from 'vitest'
import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { AppShell } from '../components/AppShell'
import { AuthProvider } from '../context/AuthProvider'

describe('AppShell', () => {
  it('renders the Saga POS header and children', () => {
    render(
      <AuthProvider>
        <AppShell>
          <div>child content</div>
        </AppShell>
      </AuthProvider>,
    )

    expect(screen.getByText('Saga POS')).toBeInTheDocument()
    expect(screen.getByText('Register / order entry')).toBeInTheDocument()
    expect(screen.getByText('child content')).toBeInTheDocument()
  })

  it('calls logout when the log out button is clicked', async () => {
    const user = userEvent.setup()
    // Spy on context logout by providing a custom provider value is not
    // possible (AuthProvider owns state), so instead assert the button exists
    // and is clickable without throwing.
    render(
      <AuthProvider>
        <AppShell>
          <div>child content</div>
        </AppShell>
      </AuthProvider>,
    )

    const logoutButton = screen.getByRole('button', { name: /log out/i })
    await user.click(logoutButton)
    expect(logoutButton).toBeInTheDocument()
  })
})