import { clsx } from 'clsx'

export function Spinner({ className }: { className?: string }) {
  return (
    <span
      aria-hidden="true"
      className={clsx(
        'inline-block h-4 w-4 animate-spin rounded-full border-2 border-slate-300 border-t-indigo-600',
        className,
      )}
    />
  )
}
