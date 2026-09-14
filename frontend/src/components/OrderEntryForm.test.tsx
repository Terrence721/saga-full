import { beforeEach, describe, expect, it, vi } from 'vitest'
import { act, render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { OrderEntryForm } from './OrderEntryForm'
import { AuthContext, type AuthState } from '../context/AuthContext'
import { createOrder, streamOrder, type Order } from '../api/orderClient'
import { ApiError } from '../api/httpClient'

vi.mock('../api/orderClient', async (importOriginal) => {
  const actual = await importOriginal<typeof import('../api/orderClient')>()
  return { ...actual, createOrder: vi.fn(), streamOrder: vi.fn() }
})

const loggedInAuth: AuthState = {
  token: 'test-token',
  customerId: 'customer-1',
  login: vi.fn(),
  logout: vi.fn(),
}

function renderForm(auth: AuthState = loggedInAuth) {
  return render(
    <AuthContext.Provider value={auth}>
      <OrderEntryForm />
    </AuthContext.Provider>,
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

async function fillAndSubmit(user: ReturnType<typeof userEvent.setup>, itemCode = 'BURGER') {
  // paste, not type - user.type() simulates one keystroke at a time, which is what
  // makes the 300-character validation-error test realistic, but also slow enough
  // to flake past the default test timeout.
  await user.click(screen.getByPlaceholderText('Item code'))
  await user.paste(itemCode)
  await user.type(screen.getByPlaceholderText('Total amount'), '5.00')
  await user.click(screen.getByRole('button', { name: 'Place order' }))
}

describe('OrderEntryForm', () => {
  const stopStream = vi.fn()

  beforeEach(() => {
    vi.mocked(createOrder).mockReset()
    vi.mocked(streamOrder).mockReset().mockReturnValue(stopStream)
    stopStream.mockReset()
  })

  it('submits the form, shows the created order, and starts streaming its status', async () => {
    vi.mocked(createOrder).mockResolvedValue(sampleOrder)
    const user = userEvent.setup()
    renderForm()

    await fillAndSubmit(user)

    expect(await screen.findByText(/status: PENDING/)).toBeInTheDocument()
    expect(createOrder).toHaveBeenCalledWith(
      { customerId: 'customer-1', itemCode: 'BURGER', quantity: 1, totalAmount: 5 },
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

    await fillAndSubmit(user, 'x'.repeat(300))

    expect(await screen.findByRole('alert')).toHaveTextContent('Bad Request')
    expect(streamOrder).not.toHaveBeenCalled()
  })

  it('"New order" stops the stream and returns to the empty form', async () => {
    vi.mocked(createOrder).mockResolvedValue(sampleOrder)
    const user = userEvent.setup()
    renderForm()

    await fillAndSubmit(user)
    await screen.findByText(/status: PENDING/)
    await user.click(screen.getByRole('button', { name: 'New order' }))

    expect(stopStream).toHaveBeenCalledOnce()
    expect(screen.getByPlaceholderText('Item code')).toBeInTheDocument()
  })

  it('refuses to submit without a logged-in customerId, and never calls createOrder', async () => {
    const user = userEvent.setup()
    renderForm({ token: null, customerId: null, login: vi.fn(), logout: vi.fn() })

    await fillAndSubmit(user)

    expect(await screen.findByRole('alert')).toHaveTextContent('You must be logged in to place an order')
    expect(createOrder).not.toHaveBeenCalled()
  })
})
