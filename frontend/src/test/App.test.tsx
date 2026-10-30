import { describe, expect, it } from 'vitest'
import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import App from '../App'

describe('App (landing page)', () => {
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

  it('opens the order modal and shows the empty cart', async () => {
    const user = userEvent.setup()
    render(<App />)

    await user.click(screen.getByRole('button', { name: /Order online/i }))

    expect(screen.getByText('Your pickup')).toBeInTheDocument()
    expect(screen.getByText('Your cart is empty.')).toBeInTheDocument()
  })

  it('adds an item to the pickup cart', async () => {
    const user = userEvent.setup()
    render(<App />)

    await user.click(screen.getByRole('button', { name: /Order online/i }))
    await user.click(screen.getByRole('button', { name: /Add Margherita to order/i }))

    expect(screen.getByText(/1 × Margherita/)).toBeInTheDocument()
  })

  it('opens the reservation modal', async () => {
    const user = userEvent.setup()
    render(<App />)

    await user.click(screen.getByRole('button', { name: /Request a table/i }))

    expect(screen.getByRole('heading', { name: /Request a table/i })).toBeInTheDocument()
  })

  it('submits a reservation request', async () => {
    const user = userEvent.setup()
    render(<App />)

    await user.click(screen.getByRole('button', { name: /Request a table/i }))
    await user.type(screen.getByPlaceholderText('Your full name'), 'Ada')
    await user.type(screen.getByPlaceholderText('you@example.com'), 'ada@example.com')
    await user.click(screen.getByRole('button', { name: /Send table request/i }))

    expect(screen.getByText(/We’ll be in touch/i)).toBeInTheDocument()
  })

  it('requires card details before checking out with a card', async () => {
    const user = userEvent.setup()
    render(<App />)

    await user.click(screen.getByRole('button', { name: /Order online/i }))
    await user.click(screen.getByRole('button', { name: /Add Margherita to order/i }))
    await user.type(screen.getByPlaceholderText('Your name'), 'Ada')
    await user.click(screen.getByLabelText(/Card online/i))
    await user.click(screen.getByRole('button', { name: /Place pickup order/i }))

    expect(screen.getByRole('button', { name: /Place pickup order/i })).toBeDisabled()
  })

  it('checks out successfully with a card', async () => {
    const user = userEvent.setup()
    render(<App />)

    await user.click(screen.getByRole('button', { name: /Order online/i }))
    await user.click(screen.getByRole('button', { name: /Add Margherita to order/i }))
    await user.type(screen.getByPlaceholderText('Your name'), 'Ada')
    await user.click(screen.getByLabelText(/Card online/i))
    await user.type(screen.getByPlaceholderText('1234 5678 9012 3456'), '4242424242424242')
    await user.type(screen.getByPlaceholderText('MM/YY'), '12/30')
    await user.type(screen.getByPlaceholderText('CVV'), '123')
    await user.click(screen.getByRole('button', { name: /Place pickup order/i }))

    expect(screen.getByText(/We’ll start the oven/i)).toBeInTheDocument()
  })

  it('checks out successfully paying at pickup', async () => {
    const user = userEvent.setup()
    render(<App />)

    await user.click(screen.getByRole('button', { name: /Order online/i }))
    await user.click(screen.getByRole('button', { name: /Add Margherita to order/i }))
    await user.type(screen.getByPlaceholderText('Your name'), 'Ada')
    await user.click(screen.getByRole('button', { name: /Place pickup order/i }))

    expect(screen.getByText(/We’ll start the oven/i)).toBeInTheDocument()
  })
})
