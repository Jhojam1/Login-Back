import clsx from 'clsx';
import { Loader2 } from 'lucide-react';
import type { ButtonHTMLAttributes, ReactNode } from 'react';

interface ButtonProps extends ButtonHTMLAttributes<HTMLButtonElement> {
  /** Importancia del botón en la pantalla. */
  variant?: 'primary' | 'secondary' | 'ghost' | 'danger';
  /** Tamaño del botón. */
  size?: 'sm' | 'md';
  /** Muestra un indicador de carga y desactiva el botón. */
  isLoading?: boolean;
  /** Ocupa todo el ancho disponible. */
  fullWidth?: boolean;
  /** Icono a la izquierda del texto. */
  icon?: ReactNode;
  children?: ReactNode;
}

/** Clases de cada variante. Solo el principal lleva color de fondo. */
const VARIANTS: Record<NonNullable<ButtonProps['variant']>, string> = {
  primary: 'bg-indigo-600 text-white shadow-sm hover:bg-indigo-500',
  secondary: 'border border-slate-200 bg-white text-slate-700 shadow-sm hover:bg-slate-50',
  ghost: 'text-slate-600 hover:bg-slate-100 hover:text-slate-900',
  danger: 'text-rose-600 hover:bg-rose-50',
};

/** Clases de cada tamaño. */
const SIZES: Record<NonNullable<ButtonProps['size']>, string> = {
  sm: 'h-8 gap-1.5 px-3 text-xs',
  md: 'h-10 gap-2 px-4 text-sm',
};

/**
 * Botón de la aplicación.
 *
 * @param props propiedades del botón, incluidas las nativas de HTML
 */
export function Button({
  variant = 'primary',
  size = 'md',
  isLoading = false,
  fullWidth = false,
  icon,
  className,
  children,
  disabled,
  ...rest
}: ButtonProps) {
  return (
    <button
      {...rest}
      disabled={disabled || isLoading}
      className={clsx(
        'inline-flex items-center justify-center rounded-lg font-medium transition-colors',
        'disabled:pointer-events-none disabled:opacity-50',
        VARIANTS[variant],
        SIZES[size],
        fullWidth && 'w-full',
        className,
      )}
    >
      {isLoading ? <Loader2 className="size-4 animate-spin" aria-hidden /> : icon}
      {children}
    </button>
  );
}

interface IconButtonProps extends ButtonHTMLAttributes<HTMLButtonElement> {
  /** Texto que se anuncia a los lectores de pantalla y aparece al pasar el ratón. */
  label: string;
  /** Icono a mostrar. */
  icon: ReactNode;
  /** Tono del botón. */
  tone?: 'default' | 'danger';
}

/**
 * Botón de solo icono, para las acciones de cada fila de una tabla.
 *
 * Usar iconos en lugar de texto en las filas evita que tres botones de texto ocupen más
 * espacio que los datos. La etiqueta sigue existiendo para accesibilidad y como tooltip.
 *
 * @param props etiqueta, icono, tono y propiedades nativas
 */
export function IconButton({ label, icon, tone = 'default', className, ...rest }: IconButtonProps) {
  return (
    <button
      type="button"
      {...rest}
      aria-label={label}
      title={rest.title ?? label}
      className={clsx(
        'inline-flex size-8 items-center justify-center rounded-lg transition-colors',
        'disabled:pointer-events-none disabled:opacity-30',
        tone === 'danger'
          ? 'text-slate-400 hover:bg-rose-50 hover:text-rose-600'
          : 'text-slate-400 hover:bg-slate-100 hover:text-slate-700',
        className,
      )}
    >
      {icon}
    </button>
  );
}
