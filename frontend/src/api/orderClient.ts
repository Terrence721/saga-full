import { apiFetch, API_BASE_URL } from './httpClient'

export interface CreateOrderRequest {
  customerId: string
  totalAmount: number
  itemCode: string
  quantity: number
  tableNumber?: string | null
  customerName?: string | null
  paymentMethod?: 'CASH' | 'CARD'
  cardNumber?: string | null
  cardCvv?: string | null
  cardBrand?: string | null
  cardLast4?: string | null
}

export type OrderStatus = 'PENDING' | 'SUCCESS' | 'CANCELLED'

export interface Order {
  id: string
  customerId: string
  tableNumber?: string | null
  customerName?: string | null
  tableName?: string | null
  totalAmount: number
  itemCode: string
  quantity: number
  status: OrderStatus
}

export function createOrder(request: CreateOrderRequest): Promise<Order> {
  return apiFetch<Order>('/orders', {
    method: 'POST',
    body: JSON.stringify(request),
  })
}

// The native EventSource API can't set an Authorization header, which
// JwtPerimeterGuard requires - so this reads the SSE response body directly via
// fetch() instead, parsing the text/event-stream format (blank-line-delimited
// events, each a "data: <json>" line) by hand.
export function streamOrder(
  orderId: string,
  onUpdate: (order: Order) => void,
): () => void {
  const controller = new AbortController()

  fetch(`${API_BASE_URL}/orders/${orderId}/stream`, {
    signal: controller.signal,
  })
    .then(async (response) => {
      if (!response.ok || !response.body) {
        return
      }

      const reader = response.body.getReader()
      const decoder = new TextDecoder()
      let buffer = ''

      for (;;) {
        const { done, value } = await reader.read()
        if (done) {
          const dataLine = buffer.split('\\n').find((line) => line.startsWith('data:'))
          if (dataLine) {
            onUpdate(JSON.parse(dataLine.slice('data:'.length).trim()) as Order)
          }
          return
        }

        buffer += decoder.decode(value, { stream: true })
        const events = buffer.split('\n\n')
        buffer = events.pop() ?? ''

        for (const event of events) {
          const dataLine = event.split('\n').find((line) => line.startsWith('data:'))
          if (dataLine) {
            onUpdate(JSON.parse(dataLine.slice('data:'.length).trim()) as Order)
          }
        }
      }
    })
    .catch(() => {
      // Aborted via the returned cleanup function, or the connection dropped -
      // either way there's nothing to do here; the caller stops listening.
    })

  return () => controller.abort()
}
