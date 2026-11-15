import clsx from 'clsx';
import type { ReactNode } from 'react';

interface BadgeProps {
  /** Tono de la etiqueta. */
  tone?: 'indigo' | 'slate' | 'emerald' | 'amber' | 'rose';
  children: ReactNode;
}

/** Clases de cada tono. */
const TONES: Record<NonNullable<BadgeProps['tone']>, string> = {
  indigo: 'bg-indigo-50 text-indigo-700 ring-indigo-200',
  slate: 'bg-slate-100 text-slate-600 ring-slate-200',
  emerald: 'bg-emerald-50 text-emerald-700 ring-emerald-200',
  amber: 'bg-amber-50 text-amber-700 ring-amber-200',
  rose: 'bg-rose-50 text-rose-700 ring-rose-200',
};

/**
 * Etiqueta pequeña para roles y estados en las tablas.
 *
 * @param props tono y contenido
 */
export function Badge({ tone = 'slate', children }: BadgeProps) {
  return (
    <span
      className={clsx(
        'inline-flex items-center gap-1 rounded-md px-2 py-0.5 text-xs font-medium ring-1 ring-inset',
        TONES[tone],
      )}
    >
      {children}
    </span>
  );
}
