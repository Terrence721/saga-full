import { apiFetch } from './httpClient'

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
