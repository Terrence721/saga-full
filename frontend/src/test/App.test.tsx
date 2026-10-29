import { beforeEach, describe, expect, it, vi } from 'vitest'
import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import App from '../App'
import { apiFetch } from '../api/httpClient'

vi.mock('../api/httpClient', () => ({
  apiFetch: vi.fn().mockResolvedValue({ id: 'reservation-1', status: 'CONFIRMED' }),
  API_BASE_URL: 'http://localhost:8090',
}))

describe('App (landing page)', () => {
  beforeEach(() => {
    vi.mocked(apiFetch).mockReset().mockResolvedValue({ id: 'reservation-1', status: 'CONFIRMED' })
  })

  it('renders the Forno brand and menu items', () => {
    render(<App />)
    expect(screen.getAllByText(/Forno/i).length).toBeGreaterThan(0)
    expect(screen.getByText('Margherita')).toBeInTheDocument()
    expect(screen.getByText('Diavola')).toBeInTheDocument()
  })

  it('filters the menu by category', async () => {
    const user = userEvent.setup()
    render(<App />)

    await user.click(screen.getByRole('tab', { name: 'Drinks' }))
    expect(screen.getByText('House Lemonade')).toBeInTheDocument()
    expect(screen.queryByText('Margherita')).not.toBeInTheDocument()
  })

  it('opens the reservation modal and submits a reservation', async () => {
    const user = userEvent.setup()
    render(<App />)

    await user.click(screen.getByRole('button', { name: /Reserve a table/i }))
    await user.click(screen.getByRole('button', { name: 'Confirm' }))

    expect(apiFetch).toHaveBeenCalledWith('/reservations', expect.objectContaining({ method: 'POST' }))
    expect(await screen.findByText(/Reservation confirmed/i)).toBeInTheDocument()
  })

  it('opens the order online modal without a login screen', async () => {
    const user = userEvent.setup()
    render(<App />)

    await user.click(screen.getByRole('button', { name: /Order online/i }))
    expect(screen.getByText('Your cart')).toBeInTheDocument()
    expect(screen.queryByRole('heading', { name: /Sign in/i })).not.toBeInTheDocument()
  })

  it('adds an item to the online cart', async () => {
    const user = userEvent.setup()
    render(<App />)

    await user.click(screen.getByRole('button', { name: /Order online/i }))
    await user.click(screen.getByRole('button', { name: /Add · \$12.50/ }))

    expect(screen.getByText('Margherita × 1')).toBeInTheDocument()
  })

  it('requires card details before checking out with a card', async () => {
    const user = userEvent.setup()
    render(<App />)

    await user.click(screen.getByRole('button', { name: /Order online/i }))
    await user.click(screen.getByRole('button', { name: /Add · \$12.50/ }))
    await user.selectOptions(screen.getByRole('combobox'), 'CARD')
    await user.click(screen.getByRole('button', { name: 'Checkout' }))

    expect(screen.getByText(/Please enter card number and security code/i)).toBeInTheDocument()
  })

  it('checks out successfully with a card', async () => {
    const user = userEvent.setup()
    render(<App />)

    await user.click(screen.getByRole('button', { name: /Order online/i }))
    await user.click(screen.getByRole('button', { name: /Add · \$12.50/ }))
    await user.selectOptions(screen.getByRole('combobox'), 'CARD')
    await user.type(screen.getByPlaceholderText('Card number'), '4242424242424242')
    await user.type(screen.getByPlaceholderText('Security code (CVV)'), '123')
    await user.click(screen.getByRole('button', { name: 'Checkout' }))

    expect(await screen.findByText(/Order placed \(1 item, card\)/i)).toBeInTheDocument()
  })
})