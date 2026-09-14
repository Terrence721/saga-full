const API_BASE_URL = import.meta.env.VITE_API_BASE_URL ?? 'http://localhost:8090'

interface ApiErrorBody {
  error?: string
  message?: string
  timestamp?: string | number
}

export class ApiError extends Error {
  readonly status: number
  readonly body: ApiErrorBody

  constructor(status: number, body: ApiErrorBody) {
    super(body.message ?? body.error ?? `Request failed with status ${status}`)
    this.name = 'ApiError'
    this.status = status
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
    // Not every error body matches api-gateway-service's own {error, message, timestamp}
    // shape (GlobalExceptionHandler builds that one by hand) - order-service's default
    // Spring Boot error page never includes `message` at all (server.error.include-message
    // isn't configured there), and its 404 has no body whatsoever.
    let body: ApiErrorBody = {}
    try {
      body = await response.json()
    } catch {
      // empty or non-JSON body - fall through with body left as {}
    }
    throw new ApiError(response.status, body)
  }

  return response.json() as Promise<T>
}
