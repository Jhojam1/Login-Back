import { ShieldCheck } from 'lucide-react';
import type { ReactNode } from 'react';

interface AuthCardProps {
  /** Título del formulario. */
  title: string;
  /** Línea corta bajo el título. */
  subtitle: string;
  /** Formulario. */
  children: ReactNode;
  /** Enlace o texto bajo la tarjeta. */
  footer: ReactNode;
}

/**
 * Marco común del login y el registro: logotipo, tarjeta centrada y enlace inferior.
 *
 * Compartirlo evita que las dos pantallas se vayan separando visualmente con el tiempo.
 *
 * @param props título, subtítulo, formulario y pie
 */
export function AuthCard({ title, subtitle, children, footer }: AuthCardProps) {
  return (
    <div className="relative flex min-h-screen items-center justify-center overflow-hidden bg-slate-50 px-4 py-12">
      {/* Un único resplandor suave detrás de la tarjeta; nada más de decoración */}
      <div
        aria-hidden
        className="pointer-events-none absolute left-1/2 top-0 h-[480px] w-[720px] -translate-x-1/2 rounded-full bg-indigo-200/40 blur-3xl"
      />

      <div className="relative w-full max-w-sm animate-rise">
        <div className="mb-6 flex flex-col items-center text-center">
          <div className="flex size-11 items-center justify-center rounded-xl bg-indigo-600 text-white shadow-lg shadow-indigo-600/25">
            <ShieldCheck className="size-6" />
          </div>
          <h1 className="mt-5 text-2xl font-semibold tracking-tight text-slate-900">{title}</h1>
          <p className="mt-1.5 text-sm text-slate-500">{subtitle}</p>
        </div>

        <div className="card p-6">{children}</div>

        <div className="mt-6 text-center text-sm text-slate-500">{footer}</div>
      </div>
    </div>
  );
}
