import { afterEach, describe, expect, it, vi } from 'vitest'
import { createOrder, streamOrder, type Order } from './orderClient'

const sampleOrder: Order = {
  id: 'order-1',
  customerId: 'customer-1',
  totalAmount: 9.5,
  itemCode: 'BURGER',
  quantity: 1,
  status: 'PENDING',
}

afterEach(() => {
  vi.unstubAllGlobals()
})

describe('createOrder', () => {
  it('POSTs to /orders with the Authorization header and returns the created order', async () => {
    const fetchMock = vi.fn().mockResolvedValue({ ok: true, status: 201, json: () => Promise.resolve(sampleOrder) })
    vi.stubGlobal('fetch', fetchMock)

    const result = await createOrder(
      { customerId: 'customer-1', totalAmount: 9.5, itemCode: 'BURGER', quantity: 1 },
      'my-token',
    )

    expect(result).toEqual(sampleOrder)
    const [url, init] = fetchMock.mock.calls[0]
    expect(url).toContain('/orders')
    expect(init.method).toBe('POST')
    expect(init.headers.Authorization).toBe('Bearer my-token')
    expect(JSON.parse(init.body)).toEqual({
      customerId: 'customer-1',
      totalAmount: 9.5,
      itemCode: 'BURGER',
      quantity: 1,
    })
  })
})

// Real Response bodies stream incrementally; a test double built from a fixed array
// of chunks (rather than a template string) is what actually exercises that, and lets
// one test deliberately split a single SSE event across a chunk boundary.
function fakeSseResponse(chunks: string[]): Response {
  const encoder = new TextEncoder()
  let index = 0
  return {
    ok: true,
    body: {
      getReader: () => ({
        read: () => {
          if (index < chunks.length) {
            const value = encoder.encode(chunks[index])
            index += 1
            return Promise.resolve({ done: false, value })
          }
          return Promise.resolve({ done: true, value: undefined })
        },
      }),
    },
  } as unknown as Response
}

describe('streamOrder', () => {
  it('parses each "data: <json>" event and calls onUpdate for each one', async () => {
    const updated = { ...sampleOrder, status: 'SUCCESS' as const }
    const body = `data: ${JSON.stringify(sampleOrder)}\n\ndata: ${JSON.stringify(updated)}\n\n`
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue(fakeSseResponse([body])))

    const updates: Order[] = []
    streamOrder(sampleOrder.id, 'tok', (order) => updates.push(order))

    await vi.waitFor(() => expect(updates).toHaveLength(2))
    expect(updates[0]).toEqual(sampleOrder)
    expect(updates[1]).toEqual(updated)
  })

  it('reassembles a single event split across two chunks (a real network delivery pattern)', async () => {
    const full = `data: ${JSON.stringify(sampleOrder)}\n\n`
    const splitPoint = Math.floor(full.length / 2)
    vi.stubGlobal(
      'fetch',
      vi.fn().mockResolvedValue(fakeSseResponse([full.slice(0, splitPoint), full.slice(splitPoint)])),
    )

    const updates: Order[] = []
    streamOrder(sampleOrder.id, 'tok', (order) => updates.push(order))

    await vi.waitFor(() => expect(updates).toHaveLength(1))
    expect(updates[0]).toEqual(sampleOrder)
  })

  it('requests the stream URL with the Authorization header', () => {
    const fetchMock = vi.fn().mockResolvedValue(fakeSseResponse([]))
    vi.stubGlobal('fetch', fetchMock)

    streamOrder('order-123', 'my-token', () => {})

    const [url, init] = fetchMock.mock.calls[0]
    expect(url).toContain('/orders/order-123/stream')
    expect(init.headers.Authorization).toBe('Bearer my-token')
  })

  it('the returned cleanup function aborts the underlying request', () => {
    const fetchMock = vi.fn().mockResolvedValue(fakeSseResponse([]))
    vi.stubGlobal('fetch', fetchMock)

    const stop = streamOrder('order-123', 'tok', () => {})
    stop()

    const [, init] = fetchMock.mock.calls[0]
    expect((init.signal as AbortSignal).aborted).toBe(true)
  })
})
