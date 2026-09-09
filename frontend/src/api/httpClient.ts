const API_BASE_URL = import.meta.env.VITE_API_BASE_URL ?? 'http://localhost:8090'

interface ApiErrorBody {
  error: string
  message: string
  timestamp: number
}

export class ApiError extends Error {
  readonly body: ApiErrorBody

  constructor(body: ApiErrorBody) {
    super(body.message)
    this.name = 'ApiError'
    this.body = body
  }
}

export async function apiFetch<T>(path: string, init?: RequestInit): Promise<T> {
  const response = await fetch(`${API_BASE_URL}${path}`, {
    ...init,
    headers: {
      'Content-Type': 'application/json',
      ...init?.headers,
    },
  })

  if (!response.ok) {
    const body: ApiErrorBody = await response.json()
    throw new ApiError(body)
  }

  return response.json() as Promise<T>
}
