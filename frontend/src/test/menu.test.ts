import { describe, expect, it } from 'vitest'
import { MENU, findMenuItem, categoryItems } from '../data/menu'

describe('menu data', () => {
  it('contains a non-empty catalogue', () => {
    expect(MENU.length).toBeGreaterThan(0)
  })

  it('finds a menu item by code', () => {
    const item = findMenuItem('MARGHERITA')
    expect(item).toBeDefined()
    expect(item?.name).toBe('Margherita')
    expect(item?.price).toBe(8.5)
  })

  it('returns undefined for an unknown code', () => {
    expect(findMenuItem('NOPE')).toBeUndefined()
  })

  it('filters items by category', () => {
    const colas = categoryItems('COLA')
    expect(colas).toHaveLength(1)
    expect(colas[0].itemCode).toBe('COLA')

    const missing = categoryItems('DOES_NOT_EXIST' as never)
    expect(missing).toHaveLength(0)
  })
})