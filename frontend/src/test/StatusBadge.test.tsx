import { describe, expect, it } from 'vitest'
import { render, screen } from '@testing-library/react'
import { StatusBadge } from '../components/ui/StatusBadge'

describe('StatusBadge', () => {
  it('renders the status text', () => {
    render(<StatusBadge status="PENDING" />)

    expect(screen.getByText('PENDING')).toBeInTheDocument()
  })

  it('renders a span element', () => {
    render(<StatusBadge status="SUCCESS" />)

    expect(screen.getByText('SUCCESS').tagName).toBe('SPAN')
  })

  it('applies base classes', () => {
    render(<StatusBadge status="PENDING" />)

    const badge = screen.getByText('PENDING')
    expect(badge).toHaveClass('inline-flex', 'items-center', 'rounded-full', 'text-xs', 'font-medium', 'ring-1', 'ring-inset')
  })

  it.each([
    ['PENDING', 'bg-amber-50', 'text-amber-700'],
    ['SUCCESS', 'bg-emerald-50', 'text-emerald-700'],
    ['CONFIRMED', 'bg-emerald-50', 'text-emerald-700'],
    ['FAILED', 'bg-red-50', 'text-red-700'],
    ['REJECTED', 'bg-red-50', 'text-red-700'],
    ['CANCELLED', 'bg-slate-100', 'text-slate-700'],
  ])('applies the %s tone classes', (status, bg, text) => {
    render(<StatusBadge status={status} />)

    const badge = screen.getByText(status)
    expect(badge).toHaveClass(bg, text)
  })

  it('falls back to slate tone for unknown statuses', () => {
    render(<StatusBadge status="UNKNOWN" />)

    const badge = screen.getByText('UNKNOWN')
    expect(badge).toHaveClass('bg-slate-100', 'text-slate-700', 'ring-slate-600/20')
  })

  it('renders arbitrary status strings as text', () => {
    render(<StatusBadge status="IN_PROGRESS" />)

    expect(screen.getByText('IN_PROGRESS')).toBeInTheDocument()
  })

  it('renders multiple badges independently', () => {
    render(
      <>
        <StatusBadge status="PENDING" />
        <StatusBadge status="FAILED" />
      </>,
    )

    expect(screen.getByText('PENDING')).toBeInTheDocument()
    expect(screen.getByText('FAILED')).toBeInTheDocument()
  })
})