import { clsx } from 'clsx'
import { cva, type VariantProps } from 'class-variance-authority'
import type { ButtonHTMLAttributes } from 'react'

const button = cva(
  'inline-flex items-center justify-center gap-2 rounded-xl font-medium transition-colors focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-offset-2 disabled:pointer-events-none disabled:opacity-50 min-h-[44px] px-4 py-2 text-sm',
  {
    variants: {
      variant: {
        primary: 'bg-indigo-600 text-white hover:bg-indigo-500 focus-visible:ring-indigo-500',
        secondary: 'bg-slate-100 text-slate-900 hover:bg-slate-200 focus-visible:ring-slate-400',
        ghost: 'bg-transparent text-slate-700 hover:bg-slate-100 focus-visible:ring-slate-400',
      },
      size: {
        sm: 'min-h-[36px] px-3 py-1.5 text-xs',
        md: 'min-h-[44px] px-4 py-2 text-sm',
        lg: 'min-h-[48px] px-5 py-2.5 text-base',
      },
    },
    defaultVariants: {
      variant: 'primary',
      size: 'md',
    },
  },
)

export interface ButtonProps
  extends ButtonHTMLAttributes<HTMLButtonElement>,
    VariantProps<typeof button> {}

export function Button({ className, variant, size, ...props }: ButtonProps) {
  return <button className={clsx(button({ variant, size }), className)} {...props} />
}
