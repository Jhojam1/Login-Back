import clsx from 'clsx';
import { CircleAlert, CircleCheck } from 'lucide-react';
import type { ReactNode } from 'react';

interface NoticeProps {
  /** Tono del mensaje. */
  variant?: 'error' | 'success';
  children: ReactNode;
}

/**
 * Mensaje de resultado de una acción.
 *
 * `role="alert"` hace que los lectores de pantalla lo anuncien al aparecer.
 *
 * @param props tono y contenido del mensaje
 */
export function Notice({ variant = 'error', children }: NoticeProps) {
  const Icon = variant === 'error' ? CircleAlert : CircleCheck;

  return (
    <div
      role="alert"
      className={clsx(
        'flex animate-fade items-start gap-2.5 rounded-lg border px-3.5 py-2.5 text-sm',
        variant === 'error'
          ? 'border-rose-200 bg-rose-50 text-rose-700'
          : 'border-emerald-200 bg-emerald-50 text-emerald-700',
      )}
    >
      <Icon className="mt-0.5 size-4 shrink-0" aria-hidden />
      <span>{children}</span>
    </div>
  );
}
