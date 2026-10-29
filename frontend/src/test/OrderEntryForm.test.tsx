import { beforeEach, describe, expect, it, vi } from 'vitest'
import { act, render, screen } from '@testing-library/react'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import userEvent from '@testing-library/user-event'
import { OrderEntryForm } from '../components/OrderEntryForm'
import { AuthContext, type AuthState } from '../context/AuthContext'
import { createOrder, streamOrder, type Order } from '../api/orderClient'
import { fetchMenu } from '../api/menuClient'
import { ApiError } from '../api/httpClient'

vi.mock('../api/orderClient', async (importOriginal) => {
  const actual = await importOriginal<typeof import('../api/orderClient')>()
  return { ...actual, createOrder: vi.fn(), streamOrder: vi.fn() }
})

vi.mock('../api/menuClient', () => ({
  fetchMenu: vi.fn().mockResolvedValue([
    { itemCode: 'MARGHERITA', name: 'Margherita', description: 'Tomato, mozzarella, basil', category: 'MARGHERITA', price: 8.5, available: true },
  ]),
}))

const loggedInAuth: AuthState = {
  token: 'test-token',
  customerId: 'customer-1',
  login: vi.fn(),
  logout: vi.fn(),
}

function renderForm(auth: AuthState = loggedInAuth) {
  const queryClient = new QueryClient()
  return render(
    <QueryClientProvider client={queryClient}>
      <AuthContext.Provider value={auth}>
        <OrderEntryForm />
      </AuthContext.Provider>
    </QueryClientProvider>,
  )
}

const sampleOrder: Order = {
  id: 'order-1',
  customerId: 'customer-1',
  totalAmount: 5,
  itemCode: 'BURGER',
  quantity: 1,
  status: 'PENDING',
}

async function fillAndSubmit(user: ReturnType<typeof userEvent.setup>, itemCode = 'MARGHERITA') {
  // Selecting a menu item also auto-fills the total amount from the catalogue;
  // overwrite it with the explicit test value so the expected payload stays 5.
  const menuSelect = await screen.findByRole('combobox', { name: 'Menu item' })
  await user.selectOptions(menuSelect, itemCode)
  await user.clear(screen.getByRole('spinbutton', { name: 'Quantity' }))
  await user.type(screen.getByRole('spinbutton', { name: 'Quantity' }), '1')
  await user.clear(screen.getByPlaceholderText('Total amount'))
  await user.type(screen.getByPlaceholderText('Total amount'), '5.00')
  await user.click(screen.getByRole('button', { name: 'Place order' }))
}

describe('OrderEntryForm', () => {
  const stopStream = vi.fn()

  beforeEach(() => {
    vi.mocked(createOrder).mockReset()
    vi.mocked(streamOrder).mockReset().mockReturnValue(stopStream)
    stopStream.mockReset()
    vi.mocked(fetchMenu).mockReset().mockResolvedValue([
      { itemCode: 'MARGHERITA', name: 'Margherita', description: 'Tomato, mozzarella, basil', category: 'MARGHERITA', price: 8.5, available: true },
    ])
  })

  it('submits the form, shows the created order, and starts streaming its status', async () => {
    vi.mocked(createOrder).mockResolvedValue(sampleOrder)
    const user = userEvent.setup()
    renderForm()

    await fillAndSubmit(user)

    expect(await screen.findByText(/status: PENDING/)).toBeInTheDocument()
    expect(createOrder).toHaveBeenCalledWith(
      { customerId: 'customer-1', itemCode: 'MARGHERITA', quantity: 1, totalAmount: 5, tableNumber: null, customerName: null, paymentMethod: 'CASH', cardNumber: null, cardCvv: null },
      'test-token',
    )
    expect(streamOrder).toHaveBeenCalledWith('order-1', 'test-token', expect.any(Function))
  })

  it('updates the displayed status when the stream pushes a live update', async () => {
    vi.mocked(createOrder).mockResolvedValue(sampleOrder)
    const user = userEvent.setup()
    renderForm()

    await fillAndSubmit(user)
    await screen.findByText(/status: PENDING/)

    const onUpdate = vi.mocked(streamOrder).mock.calls[0][2]
    act(() => {
      onUpdate({ ...sampleOrder, status: 'SUCCESS' })
    })

    expect(await screen.findByText(/status: SUCCESS/)).toBeInTheDocument()
  })

  it('shows the real API error message on a failed order, and never starts streaming', async () => {
    vi.mocked(createOrder).mockRejectedValue(new ApiError(400, { error: 'Bad Request' }))
    const user = userEvent.setup()
    renderForm()

    await fillAndSubmit(user)

    expect(await screen.findByRole('alert')).toHaveTextContent('Bad Request')
    expect(streamOrder).not.toHaveBeenCalled()
  })

  it('updates cart quantities with the steppers', async () => {
    const user = userEvent.setup()
    renderForm()

    await screen.findByRole('combobox', { name: 'Menu item' })
    await user.click(screen.getByRole('button', { name: /Margherita/ }))

    expect(screen.getByText('MARGHERITA')).toBeInTheDocument()

    await user.click(screen.getByRole('button', { name: '+' }))
    expect(screen.getByText('2')).toBeInTheDocument()

    await user.click(screen.getByRole('button', { name: '-' }))
    expect(screen.getByText('1')).toBeInTheDocument()
  })

  it('"New order" stops the stream and returns to the empty form', async () => {
    vi.mocked(createOrder).mockResolvedValue(sampleOrder)
    const user = userEvent.setup()
    renderForm()

    await fillAndSubmit(user)
    await screen.findByText(/status: PENDING/)
    await user.click(screen.getByRole('button', { name: 'New order' }))

    expect(stopStream).toHaveBeenCalledOnce()
    expect(screen.getByRole('combobox', { name: 'Menu item' })).toBeInTheDocument()
  })


  it('steppers quantity up and down and disables decrement at the minimum', async () => {
    const user = userEvent.setup()
    renderForm()

    const quantityInput = screen.getByRole('spinbutton', { name: 'Quantity' })
    expect(quantityInput).toHaveValue(1)
    expect(screen.getByRole('button', { name: 'Decrease quantity' })).toBeDisabled()

    await user.click(screen.getByRole('button', { name: 'Increase quantity' }))
    expect(quantityInput).toHaveValue(2)

    await user.click(screen.getByRole('button', { name: 'Decrease quantity' }))
    expect(quantityInput).toHaveValue(1)
    expect(screen.getByRole('button', { name: 'Decrease quantity' })).toBeDisabled()

    expect(createOrder).not.toHaveBeenCalled()
  })

  it('refuses to submit without a logged-in customerId, and never calls createOrder', async () => {
    const user = userEvent.setup()
    renderForm({ token: null, customerId: null, login: vi.fn(), logout: vi.fn() })

    await fillAndSubmit(user)

    expect(await screen.findByRole('alert')).toHaveTextContent('You must be logged in to place an order')
    expect(createOrder).not.toHaveBeenCalled()
  })
})
