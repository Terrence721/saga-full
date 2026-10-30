import { describe, expect, it } from 'vitest'
import { render } from '@testing-library/react'
import { Spinner } from '../components/ui/Spinner'

describe('Spinner', () => {
  it('renders a span element', () => {
    const { container } = render(<Spinner />)

    expect(container.querySelector('span')).toBeInTheDocument()
  })

  it('is marked aria-hidden', () => {
    const { container } = render(<Spinner />)

    expect(container.querySelector('span')).toHaveAttribute('aria-hidden', 'true')
  })

  it('applies the base animation and sizing classes', () => {
    const { container } = render(<Spinner />)

    const span = container.querySelector('span')!
    expect(span).toHaveClass(
      'inline-block',
      'h-4',
      'w-4',
      'animate-spin',
      'rounded-full',
      'border-2',
      'border-slate-300',
      'border-t-indigo-600',
    )
  })

  it('merges a custom className', () => {
    const { container } = render(<Spinner className="text-red-500" />)

    const span = container.querySelector('span')!
    expect(span).toHaveClass('text-red-500')
    expect(span).toHaveClass('animate-spin')
  })

  it('renders without crashing when no props provided', () => {
    const { container } = render(<Spinner />)

    expect(container.firstChild).toBeInTheDocument()
  })

  it('renders multiple spinners independently', () => {
    const { container } = render(
      <>
        <Spinner />
        <Spinner className="extra" />
      </>,
    )

    const spans = container.querySelectorAll('span')
    expect(spans).toHaveLength(2)
    expect(spans[1]).toHaveClass('extra')
  })
})