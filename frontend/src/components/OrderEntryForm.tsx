import { useOrderEntry } from '../hooks/useOrderEntry'
import { Button } from './ui/Button'
import { Card } from './ui/Card'
import { Input } from './ui/Input'
import { StatusBadge } from './ui/StatusBadge'
import { Spinner } from './ui/Spinner'

export function OrderEntryForm() {
  const h = useOrderEntry()

  if (h.placed.length > 0) {
    return (
      <Card>
        <div className="mb-6 flex items-baseline justify-between">
          <div>
            <h2 className="text-xl font-semibold text-slate-900">Placed orders</h2>
            <p className="mt-1 text-sm text-slate-500">Live statuses update below.</p>
          </div>
          <StatusBadge status={h.placed.every((o) => o.status === 'SUCCESS') ? 'SUCCESS' : 'PENDING'} />
        </div>
        <ul className="space-y-3">
          {h.placed.map((placedOrder) => (
            <li key={placedOrder.id} className="flex items-center justify-between rounded-xl border border-slate-200 bg-slate-50 px-4 py-3 text-sm">
              <div>
                <p className="font-medium text-slate-900">{h.menu.find((item) => item.itemCode === placedOrder.itemCode)?.name ?? placedOrder.itemCode}</p>
                {(placedOrder.tableNumber || placedOrder.customerName) && (
                  <p className="text-xs text-slate-500">
                    {placedOrder.tableNumber ? `Table ${placedOrder.tableNumber}` : ''}{placedOrder.tableNumber && placedOrder.customerName ? ' · ' : ''}{placedOrder.customerName ?? ''}
                  </p>
                )}
              </div>
              <span className="text-slate-700">×{placedOrder.quantity} — status: {placedOrder.status}</span>
            </li>
          ))}
        </ul>
        <div className="mt-6">
          <Button type="button" variant="secondary" onClick={() => {
            h.startNewOrder()
            h.setPlaced([])
          }}>
            New order
          </Button>
        </div>
      </Card>
    )
  }

  if (h.order) {
    return (
      <Card>
        <div className="flex flex-col gap-4 sm:flex-row sm:items-start sm:justify-between">
          <div>
            <p className="text-sm text-slate-500">Current order</p>
            <p className="mt-1 text-lg font-semibold text-slate-900">
              {h.menu.find((item) => item.itemCode === h.order!.itemCode)?.name ?? h.order!.itemCode}
            </p>
            <p className="mt-0.5 text-sm text-slate-500">Status: {h.order.status}</p>
            {(h.order.tableNumber || h.order.customerName) && (
              <p className="mt-0.5 text-xs text-slate-400">
                {h.order.tableNumber ? `Table ${h.order.tableNumber}` : ''}{h.order.tableNumber && h.order.customerName ? ' · ' : ''}{h.order.customerName ?? ''}
              </p>
            )}
            {h.order.tableName && <p className="mt-0.5 text-xs text-slate-400">Table / customer: {h.order.tableName}</p>}
            <div className="mt-3 flex items-center gap-2 text-xs text-slate-500">
              <Spinner className="h-3 w-3 border-slate-300 border-t-indigo-600" />
              <span>Live updates connected</span>
            </div>
          </div>
          <StatusBadge status={h.order.status} />
        </div>

        <dl className="mt-6 grid grid-cols-3 gap-4 rounded-xl bg-slate-50 p-4 text-sm">
          <div>
            <dt className="text-xs font-medium uppercase tracking-wide text-slate-500">Item</dt>
            <dd className="mt-1 font-medium text-slate-900">{h.order.itemCode}</dd>
          </div>
          <div>
            <dt className="text-xs font-medium uppercase tracking-wide text-slate-500">Quantity</dt>
            <dd className="mt-1 font-medium text-slate-900">{h.order.quantity}</dd>
          </div>
          <div>
            <dt className="text-xs font-medium uppercase tracking-wide text-slate-500">Total</dt>
            <dd className="mt-1 font-medium text-slate-900">{h.order.totalAmount}</dd>
          </div>
        </dl>

        <div className="mt-6">
          <Button type="button" variant="secondary" onClick={h.startNewOrder}>
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

      {h.menu.length > 0 && (
        <div className="space-y-3">
          <div className="flex items-baseline justify-between">
            <h3 className="text-sm font-semibold text-slate-900">Menu (click to fill the order line)</h3>
            <span className="text-xs text-slate-500">{h.menu.length} items</span>
          </div>
          <div className="grid grid-cols-1 gap-3 sm:grid-cols-2">
            {h.menu.map((item) => {
              const selected = item.itemCode === h.itemCode
              return (
                <button
                  key={item.itemCode}
                  type="button"
                  disabled={h.isSubmitting}
                  onClick={() => {
                    h.setItemCode(item.itemCode)
                    h.setTotalAmount((item.price * (Number(h.quantity) || 1)).toFixed(2))
                    h.setCart((current) => {
                      const existing = current.find((line) => line.item.itemCode === item.itemCode)
                      const next = existing
                        ? current.map((line) =>
                            line.item.itemCode === item.itemCode
                              ? { ...line, quantity: line.quantity + 1 }
                              : line,
                          )
                        : [...current, { item, quantity: 1 }]
                      const total = next.reduce((sum, line) => sum + line.item.price * line.quantity, 0)
                      h.setTotalAmount(total.toFixed(2))
                      return next
                    })
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
                </button>
              )
            })}
          </div>
        </div>
      )}

      {h.cart.length > 0 && (
        <div className="rounded-xl border border-indigo-200 bg-indigo-50/50 p-4 text-sm">
          <p className="mb-2 font-semibold text-slate-900">Cart ({h.cart.length})</p>
          <ul className="space-y-3">
            {h.cart.map((line) => (
              <li key={line.item.itemCode} className="flex items-center justify-between gap-3">
                <div className="flex-1">
                  <p className="font-medium text-slate-900">{line.item.name}</p>
                  <p className="text-xs text-slate-500">{line.item.itemCode}</p>
                </div>
                <div className="flex items-center gap-1">
                  <Button
                    type="button"
                    variant="secondary"
                    size="sm"
                    disabled={h.isSubmitting}
                    aria-label="Decrease quantity"
                    onClick={() =>
                      h.setCart((current) =>
                        current
                          .map((l) =>
                            l.item.itemCode === line.item.itemCode ? { ...l, quantity: Math.max(1, l.quantity - 1) } : l,
                          )
                          .filter((l) => l.quantity > 0),
                      )
                    }
                  >
                    -
                  </Button>
                  <span className="w-6 text-center text-sm">{line.quantity}</span>
                  <Button
                    type="button"
                    variant="secondary"
                    size="sm"
                    disabled={h.isSubmitting}
                    aria-label="Increase quantity"
                    onClick={() =>
                      h.setCart((current) => {
                        const next = current.map((l) =>
                          l.item.itemCode === line.item.itemCode ? { ...l, quantity: l.quantity + 1 } : l,
                        )
                        const total = next.reduce((sum, l) => sum + l.item.price * l.quantity, 0)
                        h.setTotalAmount(total.toFixed(2))
                        return next
                      })
                    }
                  >
                    +
                  </Button>
                </div>
                <span className="w-16 text-right font-medium">{(line.item.price * line.quantity).toFixed(2)}</span>
              </li>
            ))}
          </ul>
        </div>
      )}

      <form onSubmit={h.handleSubmit} className="space-y-4">
        {h.menu.length > 0 && (
          <select
            name="menuItem"
            aria-label="Menu item"
            value={h.itemCode}
            disabled={h.isSubmitting}
            required
            onChange={(e) => {
              const code = e.target.value
              h.setItemCode(code)
              const selected = h.menu.find((item) => item.itemCode === code)
              if (selected) h.setTotalAmount((selected.price * (Number(h.quantity) || 1)).toFixed(2))
            }}
            className="h-11 w-full rounded-xl border border-slate-300 bg-white px-3 text-sm text-slate-900 shadow-sm focus:border-indigo-500 focus:outline-none focus:ring-2 focus:ring-indigo-500/20"
          >
            <option value="">Choose a menu item…</option>
            {h.menu.map((item) => (
              <option key={item.itemCode} value={item.itemCode}>
                {item.name} — {item.price.toFixed(2)}
              </option>
            ))}
          </select>
        )}
        <select
          name="tableNumber"
          aria-label="Table number"
          value={h.tableNumber}
          disabled={h.isSubmitting}
          onChange={(e) => h.setTableNumber(e.target.value)}
          className="h-11 w-full rounded-xl border border-slate-300 bg-white px-3 text-sm text-slate-900 shadow-sm focus:border-indigo-500 focus:outline-none focus:ring-2 focus:ring-indigo-500/20"
        >
          <option value="">No table (optional)</option>
          {Array.from({ length: 10 }, (_, i) => `Table ${i + 1}`).map((t) => (
            <option key={t} value={t}>{t}</option>
          ))}
        </select>
        <Input name="customerName" type="text" placeholder="Customer name (optional)" disabled={h.isSubmitting} value={h.customerName} onChange={(e) => h.setCustomerName(e.target.value)} />

        <div>
          <label className="mb-1 block text-xs font-medium uppercase tracking-wide text-slate-500">
            Payment method
          </label>
          <select
            name="paymentMethod"
            aria-label="Payment method"
            value={h.paymentMethod}
            disabled={h.isSubmitting}
            onChange={(e) => h.setPaymentMethod(e.target.value as 'CASH' | 'CARD')}
            className="h-11 w-full rounded-xl border border-slate-300 bg-white px-3 text-sm text-slate-900 shadow-sm focus:border-indigo-500 focus:outline-none focus:ring-2 focus:ring-indigo-500/20"
          >
            <option value="CASH">Cash</option>
            <option value="CARD">Credit / Debit card</option>
          </select>
        </div>

        {h.paymentMethod === 'CARD' && (
          <div className="space-y-3">
            <Input name="cardNumber" type="text" placeholder="Card number" disabled={h.isSubmitting} value={h.cardNumber} onChange={(e) => h.setCardNumber(e.target.value)} />
            <Input name="cardCvv" type="text" placeholder="Security code (CVV)" disabled={h.isSubmitting} value={h.cardCvv} onChange={(e) => h.setCardCvv(e.target.value)} />
          </div>
        )}

        <div>
          <label className="mb-1 block text-xs font-medium uppercase tracking-wide text-slate-500">
            Quantity
          </label>
          <div className="flex items-center gap-2">
            <Button
              type="button"
              variant="secondary"
              size="sm"
              onClick={() => h.setQuantity((v) => String(Math.max(1, (Number(v) || 1) - 1)))}
              disabled={Number(h.quantity) <= 1 || h.isSubmitting}
              aria-label="Decrease quantity"
            >
              −
            </Button>
            <Input
              name="quantity"
              type="number"
              min="1"
              value={h.quantity}
              onChange={(e) => {
                h.setQuantity(e.target.value)
                const selected = h.menu.find((item) => item.itemCode === h.itemCode)
                if (selected) h.setTotalAmount((selected.price * (Number(e.target.value) || 1)).toFixed(2))
              }}
              required
              aria-label="Quantity"
              disabled={h.isSubmitting}
              className="text-center"
            />
            <Button
              type="button"
              variant="secondary"
              size="sm"
              onClick={() => h.setQuantity((v) => String((Number(v) || 0) + 1))}
              aria-label="Increase quantity"
              disabled={h.isSubmitting}
            >
              +
            </Button>
          </div>
        </div>
        <Input
          name="totalAmount"
          type="number"
          placeholder="Total amount"
          disabled={h.isSubmitting}
          min="0.01"
          step="0.01"
          value={h.totalAmount}
          onChange={(e) => h.setTotalAmount(e.target.value)}
          required
        />
        {h.error && <p role="alert" className="text-sm text-red-600">{h.error}</p>}
        <Button type="submit" disabled={h.isSubmitting}>
          {h.isSubmitting && <Spinner className="border-white border-t-transparent" />}
          {h.isSubmitting ? 'Placing…' : 'Place order'}
        </Button>
      </form>
    </Card>
  )
}