import { useEffect, useRef, useState, type FormEvent } from 'react'
import { createOrder, streamOrder, type Order } from '../api/orderClient'
import { ApiError } from '../api/httpClient'
import { useAuth } from '../context/useAuth'

export function OrderEntryForm() {
  const [itemCode, setItemCode] = useState('')
  const [quantity, setQuantity] = useState('1')
  const [totalAmount, setTotalAmount] = useState('')
  const [error, setError] = useState<string | null>(null)
  const [order, setOrder] = useState<Order | null>(null)
  const auth = useAuth()
  const stopStreamRef = useRef<(() => void) | null>(null)

  // Stop listening if the component unmounts mid-order - an open SSE connection
  // with nothing left to update would otherwise keep running in the background.
  useEffect(() => () => stopStreamRef.current?.(), [])

  async function handleSubmit(event: FormEvent) {
    event.preventDefault()
    setError(null)

    if (!auth.token || !auth.customerId) {
      setError('You must be logged in to place an order')
      return
    }

    try {
      const created = await createOrder(
        {
          customerId: auth.customerId,
          itemCode,
          quantity: Number(quantity),
          totalAmount: Number(totalAmount),
        },
        auth.token,
      )
      // Set the just-created order immediately for instant feedback, then let the
      // stream's own first event (the same current snapshot) confirm it and every
      // status change after that arrive live.
      setOrder(created)
      stopStreamRef.current = streamOrder(created.id, auth.token, setOrder)
    } catch (err) {
      setError(err instanceof ApiError ? err.message : 'Order could not be created')
    }
  }

  function startNewOrder() {
    stopStreamRef.current?.()
    stopStreamRef.current = null
    setOrder(null)
  }

  if (order) {
    return (
      <div>
        <p>
          Order {order.id} - status: {order.status}
        </p>
        <button type="button" onClick={startNewOrder}>
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
