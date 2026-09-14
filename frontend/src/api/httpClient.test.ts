import { afterEach, describe, expect, it, vi } from 'vitest'
import { apiFetch, ApiError } from './httpClient'

function mockFetchOnce(response: { ok: boolean; status: number; json: () => Promise<unknown> }) {
  vi.stubGlobal('fetch', vi.fn().mockResolvedValue(response))
}

describe('apiFetch', () => {
  afterEach(() => {
    vi.unstubAllGlobals()
  })

  it('returns the parsed JSON body on a successful response', async () => {
    mockFetchOnce({ ok: true, status: 200, json: () => Promise.resolve({ hello: 'world' }) })

    await expect(apiFetch('/anything')).resolves.toEqual({ hello: 'world' })
  })

  it('uses the message field when the server provides one (api-gateway-service GlobalExceptionHandler shape)', async () => {
    mockFetchOnce({
      ok: false,
      status: 401,
      json: () =>
        Promise.resolve({ error: 'Unauthorized', message: 'Invalid credentials', timestamp: '2026-01-01T00:00:00Z' }),
    })

    await expect(apiFetch('/auth/login')).rejects.toMatchObject({ message: 'Invalid credentials', status: 401 })
  })

  it("falls back to the error field when there is no message (order-service's real default Spring Boot error shape)", async () => {
    mockFetchOnce({
      ok: false,
      status: 400,
      json: () => Promise.resolve({ timestamp: '2026-01-01T00:00:00Z', status: 400, error: 'Bad Request', path: '/orders' }),
    })

    await expect(apiFetch('/orders')).rejects.toMatchObject({ message: 'Bad Request', status: 400 })
  })

  it("falls back to a generic message naming the status when the body has neither (order-service's real empty 404 body)", async () => {
    mockFetchOnce({
      ok: false,
      status: 404,
      json: () => Promise.reject(new SyntaxError('Unexpected end of JSON input')),
    })

    await expect(apiFetch('/orders/does-not-exist')).rejects.toMatchObject({
      message: 'Request failed with status 404',
      status: 404,
    })
  })

  it('throws a real ApiError instance with the parsed body attached', async () => {
    mockFetchOnce({ ok: false, status: 500, json: () => Promise.resolve({ error: 'Internal Server Error' }) })

    await expect(apiFetch('/whatever')).rejects.toBeInstanceOf(ApiError)
  })
})
