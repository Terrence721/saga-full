import { apiFetch, API_BASE_URL } from './httpClient'

export interface MenuItem {
  itemCode: string
  name: string
  description: string
  category: string
  price: number
  available: boolean
}

// Narrow, display-focused view models (ISP): callers depend only on the
// fields they actually need instead of the full catalogue record.
export type MenuItemSummary = Pick<MenuItem, 'itemCode' | 'name' | 'price'>
export type OrderableItem = Pick<MenuItem, 'itemCode' | 'name' | 'description' | 'price' | 'available'>

export function fetchMenu(): Promise<MenuItem[]> {
  return apiFetch<MenuItem[]>('/menu', {
    method: 'GET',
  })
}

export { API_BASE_URL }
