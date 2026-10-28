import { apiFetch, API_BASE_URL } from './httpClient'

export interface MenuItem {
  itemCode: string
  name: string
  description: string
  category: string
  price: number
  available: boolean
}

export function fetchMenu(token: string): Promise<MenuItem[]> {
  return apiFetch<MenuItem[]>('/menu', {
    method: 'GET',
    headers: { Authorization: `Bearer ${token}` },
  })
}

export { API_BASE_URL }
