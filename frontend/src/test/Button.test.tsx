import { describe, expect, it, vi } from 'vitest'
import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { Button } from '../components/ui/Button'

describe('Button', () => {
  it('renders its children', () => {
    render(<Button>Click me</Button>)

    expect(screen.getByRole('button', { name: 'Click me' })).toBeInTheDocument()
  })

  it('renders as a <button> by default', () => {
    render(<Button>Save</Button>)

    expect(screen.getByRole('button')).toHaveTextContent('Save')
  })

  it('forwards arbitrary button HTML attributes', () => {
    render(
      <Button type="submit" id="submit-btn" aria-label="submit form" data-testid="submit">
        Go
      </Button>,
    )

    const button = screen.getByTestId('submit')
    expect(button).toHaveAttribute('type', 'submit')
    expect(button).toHaveAttribute('id', 'submit-btn')
    expect(button).toHaveAttribute('aria-label', 'submit form')
  })

  it('invokes onClick when clicked', async () => {
    const user = userEvent.setup()
    const onClick = vi.fn()

    render(<Button onClick={onClick}>Press</Button>)

    await user.click(screen.getByRole('button', { name: 'Press' }))

    expect(onClick).toHaveBeenCalledOnce()
  })

  it('does not invoke onClick when disabled', async () => {
    const user = userEvent.setup()
    const onClick = vi.fn()

    render(
      <Button disabled onClick={onClick}>
        Press
      </Button>,
    )

    await user.click(screen.getByRole('button', { name: 'Press' }))

    expect(onClick).not.toHaveBeenCalled()
  })

  it('applies the primary variant classes by default', () => {
    render(<Button>Primary</Button>)

    expect(screen.getByRole('button')).toHaveClass('bg-indigo-600')
  })

  it('applies variant classes based on the variant prop', () => {
    const { rerender } = render(<Button variant="secondary">Secondary</Button>)
    expect(screen.getByRole('button')).toHaveClass('bg-slate-100')

    rerender(<Button variant="ghost">Ghost</Button>)
    expect(screen.getByRole('button')).toHaveClass('bg-transparent')

    rerender(<Button variant="primary">Primary</Button>)
    expect(screen.getByRole('button')).toHaveClass('bg-indigo-600')
  })

  it('applies size classes based on the size prop', () => {
    const { rerender } = render(<Button size="sm">Small</Button>)
    expect(screen.getByRole('button')).toHaveClass('min-h-[36px]')

    rerender(<Button size="md">Medium</Button>)
    expect(screen.getByRole('button')).toHaveClass('min-h-[44px]')

    rerender(<Button size="lg">Large</Button>)
    expect(screen.getByRole('button')).toHaveClass('min-h-[48px]')
  })

  it('uses default size medium classes when size is omitted', () => {
    render(<Button>Default</Button>)

    expect(screen.getByRole('button')).toHaveClass('min-h-[44px]', 'px-4', 'py-2', 'text-sm')
  })

  it('merges a custom className onto the generated class list', () => {
    render(<Button className="my-extra-class">Custom</Button>)

    const button = screen.getByRole('button')
    expect(button).toHaveClass('my-extra-class')
    // base classes should still be present after merging
    expect(button).toHaveClass('inline-flex')
  })

  it('renders disabled state with the disabled attribute', () => {
    render(<Button disabled>Disabled</Button>)

    expect(screen.getByRole('button')).toBeDisabled()
  })

  it('renders a native button element', () => {
    render(<Button>Ref</Button>)

    expect(screen.getByRole('button', { name: 'Ref' })).toBeInstanceOf(HTMLButtonElement)
  })

  it('renders without variant and size (undefined) without crashing', () => {
    render(<Button variant={undefined} size={undefined}>Fallback</Button>)

    const button = screen.getByRole('button')
    expect(button).toHaveClass('bg-indigo-600', 'min-h-[44px]')
  })
})