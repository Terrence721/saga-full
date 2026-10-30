import { describe, expect, it, vi } from 'vitest'
import { render, screen } from '@testing-library/react'
import { Card } from '../components/ui/Card'

describe('Card', () => {
  it('renders its children', () => {
    render(<Card>Card content</Card>)

    expect(screen.getByText('Card content')).toBeInTheDocument()
  })

  it('renders as a <div> element', () => {
    render(<Card>Content</Card>)

    expect(screen.getByText('Content').tagName).toBe('DIV')
  })

  it('applies the base styling classes', () => {
    render(<Card>Styled</Card>)

    const card = screen.getByText('Styled')
    expect(card).toHaveClass('rounded-2xl', 'border', 'border-slate-200', 'bg-white', 'p-6', 'shadow-sm')
  })

  it('merges a custom className onto the base classes', () => {
    render(<Card className="custom-card">Custom</Card>)

    const card = screen.getByText('Custom')
    expect(card).toHaveClass('custom-card')
    expect(card).toHaveClass('rounded-2xl', 'bg-white')
  })

  it('forwards arbitrary HTML attributes', () => {
    render(
      <Card id="card-id" data-testid="card" aria-labelledby="title" role="region">
        Attrs
      </Card>,
    )

    const card = screen.getByTestId('card')
    expect(card).toHaveAttribute('id', 'card-id')
    expect(card).toHaveAttribute('aria-labelledby', 'title')
    expect(card).toHaveAttribute('role', 'region')
  })

  it('renders nested elements', () => {
    render(
      <Card>
        <span>inner</span>
        <p>paragraph</p>
      </Card>,
    )

    expect(screen.getByText('inner')).toBeInTheDocument()
    expect(screen.getByText('paragraph')).toBeInTheDocument()
  })

  it('renders with no children without crashing', () => {
    render(<Card data-testid="empty-card" />)

    expect(screen.getByTestId('empty-card')).toBeInTheDocument()
  })

  it('supports ref forwarding', () => {
    const ref = { current: null as HTMLDivElement | null }
    render(<Card {...({ ref } as any)}>Ref card</Card>)

    expect(ref.current).toBeInstanceOf(HTMLDivElement)
    expect(ref.current).toHaveTextContent('Ref card')
  })

  it('handles click handler when provided', async () => {
    const onClick = vi.fn()
    render(<Card onClick={onClick}>Clickable</Card>)

    screen.getByText('Clickable').click()
    expect(onClick).toHaveBeenCalledOnce()
  })
})