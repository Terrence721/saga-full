import { clsx } from 'clsx'

const styles: Record<string, string> = {
  PENDING: 'bg-amber-50 text-amber-700 ring-amber-600/20',
  SUCCESS: 'bg-emerald-50 text-emerald-700 ring-emerald-600/20',
  CONFIRMED: 'bg-emerald-50 text-emerald-700 ring-emerald-600/20',
  FAILED: 'bg-red-50 text-red-700 ring-red-600/20',
  REJECTED: 'bg-red-50 text-red-700 ring-red-600/20',
  CANCELLED: 'bg-slate-100 text-slate-700 ring-slate-600/20',
}

export function StatusBadge({ status }: { status: string }) {
  const tone = styles[status] ?? 'bg-slate-100 text-slate-700 ring-slate-600/20'
  return (
    <span
      className={clsx(
        'inline-flex items-center rounded-full px-2.5 py-0.5 text-xs font-medium ring-1 ring-inset',
        tone,
      )}
    >
      {status}
    </span>
  )
}
