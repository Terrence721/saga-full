import { clsx } from 'clsx'
import type { InputHTMLAttributes } from 'react'

export interface InputProps extends InputHTMLAttributes<HTMLInputElement> {}

export function Input({ className, ...props }: InputProps) {
  return (
    <input
      className={clsx(
        'h-11 w-full rounded-xl border border-slate-300 bg-white px-3 text-sm text-slate-900 shadow-sm placeholder:text-slate-400 focus:border-indigo-500 focus:outline-none focus:ring-2 focus:ring-indigo-500/20 aria-[invalid=true]:border-red-500',
        className,
      )}
      {...props}
    />
  )
}
