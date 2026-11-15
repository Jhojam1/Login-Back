import type { ReactNode } from 'react';

interface PageHeaderProps {
  /** Título de la página. */
  title: string;
  /** Línea corta bajo el título, opcional. */
  subtitle?: string;
  /** Botones de acción a la derecha. */
  actions?: ReactNode;
}

/**
 * Cabecera común de todas las páginas del panel: título, subtítulo y acciones.
 *
 * Tenerla en un componente garantiza que todas las páginas empiecen igual, en lugar de
 * que cada una invente su propio tamaño de título y separación.
 *
 * @param props título, subtítulo y acciones
 */
export function PageHeader({ title, subtitle, actions }: PageHeaderProps) {
  return (
    <div className="mb-6 flex flex-wrap items-end justify-between gap-4">
      <div>
        <h1 className="text-2xl font-semibold tracking-tight text-slate-900">{title}</h1>
        {subtitle && <p className="mt-1 text-sm text-slate-500">{subtitle}</p>}
      </div>
      {actions && <div className="flex items-center gap-2">{actions}</div>}
    </div>
  );
}
