import { useEffect, useRef, useState, type FormEvent } from 'react'
import { createOrder, streamOrder, type Order } from '../api/orderClient'
import { ApiError } from '../api/httpClient'
import { useAuth } from '../context/useAuth'
import { Button } from './ui/Button'
import { fetchMenu, type MenuItem } from '../api/menuClient'
import { Card } from './ui/Card'
import { Input } from './ui/Input'
import { StatusBadge } from './ui/StatusBadge'
import { Spinner } from './ui/Spinner'

export function OrderEntryForm() {
  const [itemCode, setItemCode] = useState('')
  const [quantity, setQuantity] = useState('1')
  const [totalAmount, setTotalAmount] = useState('')
  const [error, setError] = useState<string | null>(null)
  const [menu, setMenu] = useState<MenuItem[]>([])
  const [isSubmitting, setIsSubmitting] = useState(false)
  const [order, setOrder] = useState<Order | null>(null)
  const auth = useAuth()
  const stopStreamRef = useRef<(() => void) | null>(null)

  useEffect(() => () => stopStreamRef.current?.(), [])

  useEffect(() => {
    if (!auth.token) return
    fetchMenu(auth.token).then(setMenu).catch(() => setMenu([]))
  }, [auth.token])

  async function handleSubmit(event: FormEvent) {
    event.preventDefault()
    setError(null)
    setIsSubmitting(true)

    if (!auth.token || !auth.customerId) {
      setError('You must be logged in to place an order')
      setIsSubmitting(false)
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
    } finally {
      setIsSubmitting(false)
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
        <div className="flex flex-col gap-4 sm:flex-row sm:items-start sm:justify-between">
          <div>
            <p className="text-sm text-slate-500">Current order</p>
            <p className="mt-1 text-lg font-semibold text-slate-900">
              {order.id} — status: {order.status}
            </p>
            <div className="mt-3 flex items-center gap-2 text-xs text-slate-500">
              <Spinner className="h-3 w-3 border-slate-300 border-t-indigo-600" />
              <span>Live updates connected</span>
            </div>
          </div>
          <StatusBadge status={order.status} />
        </div>

        <dl className="mt-6 grid grid-cols-3 gap-4 rounded-xl bg-slate-50 p-4 text-sm">
          <div>
            <dt className="text-xs font-medium uppercase tracking-wide text-slate-500">Item</dt>
            <dd className="mt-1 font-medium text-slate-900">{order.itemCode}</dd>
          </div>
          <div>
            <dt className="text-xs font-medium uppercase tracking-wide text-slate-500">Quantity</dt>
            <dd className="mt-1 font-medium text-slate-900">{order.quantity}</dd>
          </div>
          <div>
            <dt className="text-xs font-medium uppercase tracking-wide text-slate-500">Total</dt>
            <dd className="mt-1 font-medium text-slate-900">{order.totalAmount}</dd>
          </div>
        </dl>

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

        {menu.length > 0 && (
          <div className="space-y-3">
            <div className="flex items-baseline justify-between">
              <h3 className="text-sm font-semibold text-slate-900">Menu (click to fill the order line)</h3>
              <span className="text-xs text-slate-500">{menu.length} items</span>
            </div>
            <div className="grid grid-cols-1 gap-3 sm:grid-cols-2">
              {menu.map((item) => {
                const selected = item.itemCode === itemCode
                return (
                  <button
                    key={item.itemCode}
                    type="button"
                    disabled={isSubmitting}
                    onClick={() => {
                      setItemCode(item.itemCode)
                      setTotalAmount((item.price * (Number(quantity) || 1)).toFixed(2))
                    }}
                    className={`rounded-xl border px-4 py-3 text-left transition-colors ${
                      selected
                        ? 'border-indigo-500 bg-indigo-50 ring-1 ring-indigo-500'
                        : 'border-slate-200 bg-white hover:border-indigo-300 hover:bg-slate-50'
                    }`}
                  >
                    <div className="flex items-start justify-between gap-2">
                      <div>
                        <p className="text-sm font-semibold text-slate-900">{item.name}</p>
                        <p className="mt-0.5 text-xs text-slate-500">{item.description}</p>
                      </div>
                      <span className="text-xs font-semibold text-indigo-600">{item.price.toFixed(2)}</span>
                    </div>
                    <p className="mt-2 text-xs text-slate-400">{item.itemCode} · {item.category}</p>
                  </button>
                )
              })}
            </div>
          </div>
        )}

      <form onSubmit={handleSubmit} className="space-y-4">
        {menu.length > 0 && (
          <select
            aria-label="Menu item"
            value={itemCode}
            disabled={isSubmitting}
            onChange={(e) => {
              const code = e.target.value
              setItemCode(code)
              const selected = menu.find((item) => item.itemCode === code)
              if (selected) {
                setTotalAmount((selected.price * (Number(quantity) || 1)).toFixed(2))
              }
            }}
            className="h-11 w-full rounded-xl border border-slate-300 bg-white px-3 text-sm text-slate-900 shadow-sm focus:border-indigo-500 focus:outline-none focus:ring-2 focus:ring-indigo-500/20"
          >
            <option value="">Choose a menu item…</option>
            {menu.map((item) => (
              <option key={item.itemCode} value={item.itemCode}>
                {item.name} — {item.price.toFixed(2)}
              </option>
            ))}
          </select>
        )}
        <Input
          type="text"
          placeholder="Item code"
          list="menu-items"
          disabled={isSubmitting}
          value={itemCode}
          onChange={(e) => setItemCode(e.target.value)}
          required
        />
        <datalist id="menu-items">
          {['PIZZA_01','PIZZA_02','PIZZA_03','PIZZA_04','PIZZA_05',
            'PIZZA_06','PIZZA_07','PIZZA_08','PIZZA_09','PIZZA_10'].map((code) => (
            <option key={code} value={code} />
          ))}
        </datalist>
        <div>
          <label className="mb-1 block text-xs font-medium uppercase tracking-wide text-slate-500">
            Quantity
          </label>
          <div className="flex items-center gap-2">
            <Button
              type="button"
              variant="secondary"
              size="sm"
              onClick={() => setQuantity((v) => String(Math.max(1, (Number(v) || 1) - 1)))}
              disabled={Number(quantity) <= 1 || isSubmitting}
              aria-label="Decrease quantity"
            >
              −
            </Button>
            <Input
              type="number"
              min="1"
              value={quantity}
              onChange={(e) => setQuantity(e.target.value)}
              required
              aria-label="Quantity"
              disabled={isSubmitting}
              className="text-center"
            />
            <Button
              type="button"
              variant="secondary"
              size="sm"
              onClick={() => setQuantity((v) => String((Number(v) || 0) + 1))}
              aria-label="Increase quantity"
              disabled={isSubmitting}
            >
              +
            </Button>
          </div>
        </div>
        <Input
          type="number"
          placeholder="Total amount"
          disabled={isSubmitting}
          min="0.01"
          step="0.01"
          value={totalAmount}
          onChange={(e) => setTotalAmount(e.target.value)}
          required
        />
        {error && <p role="alert" className="text-sm text-red-600">{error}</p>}
        <Button type="submit" disabled={isSubmitting}>
          {isSubmitting && <Spinner className="border-white border-t-transparent" />}
          {isSubmitting ? 'Placing…' : 'Place order'}
        </Button>
      </form>
    </Card>
  )
}
