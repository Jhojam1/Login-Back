import clsx from 'clsx';
import { Eye, EyeOff } from 'lucide-react';
import { useId, useState, type InputHTMLAttributes, type ReactNode } from 'react';

interface InputProps extends Omit<InputHTMLAttributes<HTMLInputElement>, 'id'> {
  /** Etiqueta visible del campo. */
  label: string;
  /** Mensaje de error a mostrar bajo el campo. */
  error?: string;
  /** Icono decorativo a la izquierda. */
  icon?: ReactNode;
}

/**
 * Campo de formulario con etiqueta, icono opcional y mensaje de error.
 *
 * Los campos de contraseña incluyen un botón para mostrarla. La etiqueta se asocia al
 * campo con un `id` generado por React y el error con `aria-describedby`.
 *
 * @param props propiedades del campo, incluidas las nativas de HTML
 */
export function Input({ label, error, icon, className, type = 'text', ...rest }: InputProps) {
  const inputId = useId();
  const errorId = `${inputId}-error`;
  const [isVisible, setIsVisible] = useState(false);

  const isPassword = type === 'password';

  return (
    <div>
      <label htmlFor={inputId} className="mb-1.5 block text-sm font-medium text-slate-700">
        {label}
      </label>

      <div className="relative">
        {icon && (
          <span
            className="pointer-events-none absolute left-3 top-1/2 -translate-y-1/2 text-slate-400"
            aria-hidden
          >
            {icon}
          </span>
        )}

        <input
          {...rest}
          id={inputId}
          type={isPassword && isVisible ? 'text' : type}
          aria-invalid={Boolean(error)}
          aria-describedby={error ? errorId : undefined}
          className={clsx(
            'h-10 w-full rounded-lg border bg-white text-sm text-slate-900 shadow-sm transition',
            'placeholder:text-slate-400 focus:outline-none focus:ring-4',
            'disabled:bg-slate-50 disabled:text-slate-500',
            icon ? 'pl-9' : 'pl-3',
            isPassword ? 'pr-10' : 'pr-3',
            error
              ? 'border-rose-300 focus:border-rose-400 focus:ring-rose-100'
              : 'border-slate-200 focus:border-indigo-400 focus:ring-indigo-100',
            className,
          )}
        />

        {isPassword && (
          <button
            type="button"
            onClick={() => setIsVisible((visible) => !visible)}
            aria-label={isVisible ? 'Ocultar contraseña' : 'Mostrar contraseña'}
            className="absolute right-2 top-1/2 -translate-y-1/2 rounded-md p-1 text-slate-400 hover:text-slate-600"
          >
            {isVisible ? <EyeOff className="size-4" /> : <Eye className="size-4" />}
          </button>
        )}
      </div>

      {error && (
        <p id={errorId} className="mt-1.5 text-xs text-rose-600">
          {error}
        </p>
      )}
    </div>
  );
}
