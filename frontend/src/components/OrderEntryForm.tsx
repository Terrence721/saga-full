import { useState, type FormEvent } from 'react'
import { createOrder, type Order } from '../api/orderClient'
import { ApiError } from '../api/httpClient'
import { useAuth } from '../context/useAuth'

export function OrderEntryForm() {
  const [itemCode, setItemCode] = useState('')
  const [quantity, setQuantity] = useState('1')
  const [totalAmount, setTotalAmount] = useState('')
  const [error, setError] = useState<string | null>(null)
  const [createdOrder, setCreatedOrder] = useState<Order | null>(null)
  const auth = useAuth()

  async function handleSubmit(event: FormEvent) {
    event.preventDefault()
    setError(null)

    if (!auth.token || !auth.customerId) {
      setError('You must be logged in to place an order')
      return
    }

    try {
      const order = await createOrder(
        {
          customerId: auth.customerId,
          itemCode,
          quantity: Number(quantity),
          totalAmount: Number(totalAmount),
        },
        auth.token,
      )
      setCreatedOrder(order)
    } catch (err) {
      setError(err instanceof ApiError ? err.message : 'Order could not be created')
    }
  }

  if (createdOrder) {
    return (
      <div>
        <p>
          Order {createdOrder.id} created - status: {createdOrder.status}
        </p>
        <button type="button" onClick={() => setCreatedOrder(null)}>
          New order
        </button>
      </div>
    )
  }

  return (
    <form onSubmit={handleSubmit}>
      <input
        type="text"
        placeholder="Item code"
        value={itemCode}
        onChange={(e) => setItemCode(e.target.value)}
        required
      />
      <input
        type="number"
        placeholder="Quantity"
        min="1"
        value={quantity}
        onChange={(e) => setQuantity(e.target.value)}
        required
      />
      <input
        type="number"
        placeholder="Total amount"
        min="0.01"
        step="0.01"
        value={totalAmount}
        onChange={(e) => setTotalAmount(e.target.value)}
        required
      />
      {error && <p role="alert">{error}</p>}
      <button type="submit">Place order</button>
    </form>
  )
}
