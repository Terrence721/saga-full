import { renderHook, waitFor } from '@testing-library/react'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { createElement, type ReactNode } from 'react'
import { createElement as ce } from 'react'
import { vi, beforeEach, describe, expect, it } from 'vitest'
import { AuthContext, type AuthState } from '../context/AuthContext'
import { useOrderEntry } from './useOrderEntry'
import { fetchMenu } from '../api/menuClient'

vi.mock('../api/orderClient', async (importOriginal) => {
  const actual = await importOriginal<typeof import('../api/orderClient')>()
  return { ...actual, createOrder: vi.fn(), streamOrder: vi.fn() }
})

vi.mock('../api/menuClient', () => ({
  fetchMenu: vi.fn(),
}))

function wrapper(auth: AuthState) {
  const queryClient = new QueryClient()
  return ({ children }: { children: ReactNode }) =>
    ce(QueryClientProvider, { client: queryClient },
      ce(AuthContext.Provider, { value: auth }, children),
    )
}

const loggedInAuth: AuthState = {
  token: 'test-token',
  customerId: 'customer-1',
  login: vi.fn(),
  logout: vi.fn(),
}

describe('useOrderEntry', () => {
  beforeEach(() => {
    vi.mocked(fetchMenu).mockReset()
  })

  it('keeps the fallback menu when the API returns an empty list', async () => {
    vi.mocked(fetchMenu).mockResolvedValue([])
    const { result } = renderHook(() => useOrderEntry(), { wrapper: wrapper(loggedInAuth) })
    await waitFor(() => expect(vi.mocked(fetchMenu)).toHaveBeenCalled())
    expect(result.current.menu.length).toBeGreaterThan(0)
  })

  it('uses menu items returned by the API', async () => {
    vi.mocked(fetchMenu).mockResolvedValue([
      { itemCode: 'COLA', name: 'Cola', description: '330 ml', category: 'COLA', price: 2.5, available: true },
    ])
    const { result } = renderHook(() => useOrderEntry(), { wrapper: wrapper(loggedInAuth) })
    await waitFor(() => expect(result.current.menu).toHaveLength(1))
  })

  it('does not fetch when there is no auth token', () => {
    const { result } = renderHook(() => useOrderEntry(), {
      wrapper: wrapper({ token: null, customerId: null, login: vi.fn(), logout: vi.fn() }),
    })
    expect(vi.mocked(fetchMenu)).not.toHaveBeenCalled()
    expect(result.current.menu.length).toBeGreaterThan(0)
  })
})