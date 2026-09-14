import { apiFetch, API_BASE_URL } from './httpClient'

export interface CreateOrderRequest {
  customerId: string
  totalAmount: number
  itemCode: string
  quantity: number
}

export type OrderStatus = 'PENDING' | 'SUCCESS' | 'CANCELLED'

export interface Order {
  id: string
  customerId: string
  totalAmount: number
  itemCode: string
  quantity: number
  status: OrderStatus
}

export function createOrder(request: CreateOrderRequest, token: string): Promise<Order> {
  return apiFetch<Order>('/orders', {
    method: 'POST',
    headers: { Authorization: `Bearer ${token}` },
    body: JSON.stringify(request),
  })
}

// The native EventSource API can't set an Authorization header, which
// JwtPerimeterGuard requires - so this reads the SSE response body directly via
// fetch() instead, parsing the text/event-stream format (blank-line-delimited
// events, each a "data: <json>" line) by hand.
export function streamOrder(
  orderId: string,
  token: string,
  onUpdate: (order: Order) => void,
): () => void {
  const controller = new AbortController()

  fetch(`${API_BASE_URL}/orders/${orderId}/stream`, {
    headers: { Authorization: `Bearer ${token}` },
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
