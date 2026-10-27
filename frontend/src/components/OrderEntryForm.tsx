import { useEffect, useRef, useState, type FormEvent } from 'react'
import { createOrder, streamOrder, type Order } from '../api/orderClient'
import { ApiError } from '../api/httpClient'
import { useAuth } from '../context/useAuth'
import { Button } from './ui/Button'
import { Card } from './ui/Card'
import { Input } from './ui/Input'
import { StatusBadge } from './ui/StatusBadge'

export function OrderEntryForm() {
  const [itemCode, setItemCode] = useState('')
  const [quantity, setQuantity] = useState('1')
  const [totalAmount, setTotalAmount] = useState('')
  const [error, setError] = useState<string | null>(null)
  const [order, setOrder] = useState<Order | null>(null)
  const auth = useAuth()
  const stopStreamRef = useRef<(() => void) | null>(null)

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
      <Card>
        <div className="flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
          <div>
            <p className="text-sm text-slate-500">Current order</p>
            <p className="mt-1 text-lg font-semibold text-slate-900">
              {order.id} — status: {order.status}
            </p>
          </div>
          <StatusBadge status={order.status} />
        </div>
        <div className="mt-6">
          <Button type="button" variant="secondary" onClick={startNewOrder}>
            New order
          </Button>
        </div>
      </Card>
    )
  }

  return (
    <Card className="max-w-xl">
      <div className="mb-6">
        <h2 className="text-xl font-semibold text-slate-900">Place an order</h2>
        <p className="mt-1 text-sm text-slate-500">Capture an item and total, then send it into the saga.</p>
      </div>
      <form onSubmit={handleSubmit} className="space-y-4">
        <Input
          type="text"
          placeholder="Item code"
          value={itemCode}
          onChange={(e) => setItemCode(e.target.value)}
          required
        />
        <Input
          type="number"
          placeholder="Quantity"
          min="1"
          value={quantity}
          onChange={(e) => setQuantity(e.target.value)}
          required
        />
        <Input
          type="number"
          placeholder="Total amount"
          min="0.01"
          step="0.01"
          value={totalAmount}
          onChange={(e) => setTotalAmount(e.target.value)}
          required
        />
        {error && <p role="alert" className="text-sm text-red-600">{error}</p>}
        <Button type="submit">Place order</Button>
      </form>
    </Card>
  )
}
