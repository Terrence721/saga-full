import { describe, expect, it, vi } from 'vitest'
import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { Input } from '../components/ui/Input'

describe('Input', () => {
  it('renders an <input> element', () => {
    render(<Input />)

    expect(screen.getByRole('textbox')).toBeInTheDocument()
  })

  it('applies the base styling classes', () => {
    render(<Input />)

    const input = screen.getByRole('textbox')
    expect(input).toHaveClass('h-11', 'w-full', 'rounded-xl', 'border-slate-300', 'bg-white')
  })

  it('merges a custom className', () => {
    render(<Input className="extra-input" />)

    const input = screen.getByRole('textbox')
    expect(input).toHaveClass('extra-input')
    expect(input).toHaveClass('h-11')
  })

  it('forwards arbitrary input attributes', () => {
    render(
      <Input
        type="email"
        name="email"
        id="email-field"
        placeholder="you@example.com"
        maxLength={50}
        required
        aria-invalid
      />,
    )

    const input = screen.getByRole('textbox') as HTMLInputElement
    expect(input).toHaveAttribute('type', 'email')
    expect(input).toHaveAttribute('name', 'email')
    expect(input).toHaveAttribute('id', 'email-field')
    expect(input).toHaveAttribute('placeholder', 'you@example.com')
    expect(input).toHaveAttribute('maxlength', '50')
    expect(input).toBeRequired()
    expect(input).toHaveAttribute('aria-invalid', 'true')
  })

  it('updates its value when typed into', async () => {
    const user = userEvent.setup()
    render(<Input />)

    const input = screen.getByRole('textbox')
    await user.type(input, 'hello world')

    expect(input).toHaveValue('hello world')
  })

  it('calls onChange when the value changes', async () => {
    const user = userEvent.setup()
    const onChange = vi.fn()

    render(<Input onChange={onChange} />)

    await user.type(screen.getByRole('textbox'), 'a')

    expect(onChange).toHaveBeenCalled()
  })

  it('supports a controlled value', () => {
    render(<Input value="controlled" readOnly />)

    const input = screen.getByRole('textbox')
    expect(input).toHaveValue('controlled')
  })

  it('renders as disabled when disabled', () => {
    render(<Input disabled />)

    expect(screen.getByRole('textbox')).toBeDisabled()
  })

  it('supports ref forwarding', () => {
    const ref = { current: null as HTMLInputElement | null }
    render(<Input ref={ref} />)

    expect(ref.current).toBeInstanceOf(HTMLInputElement)
  })

  it('supports defaultValue for uncontrolled usage', () => {
    render(<Input defaultValue="initial" />)

    expect(screen.getByRole('textbox')).toHaveValue('initial')
  })

  it('renders without crashing when no props provided', () => {
    render(<Input />)

    expect(screen.getByRole('textbox')).toBeInTheDocument()
  })
})