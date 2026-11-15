import clsx from 'clsx';
import { Check } from 'lucide-react';

interface CheckOptionProps {
  /** Texto principal de la opción. */
  label: string;
  /** Texto secundario opcional. */
  hint?: string;
  /** Si está marcada. */
  checked: boolean;
  /** Se ejecuta al marcarla o desmarcarla. */
  onToggle: () => void;
}

/**
 * Opción marcable con aspecto de tarjeta, para elegir roles y permisos.
 *
 * Toda la tarjeta es clicable, no solo la casilla: con listas de diez o más opciones,
 * apuntar a un cuadrado de 16 píxeles es incómodo. Por debajo sigue siendo un
 * `<input type="checkbox">` real, así que funciona con teclado y lectores de pantalla.
 *
 * @param props texto, estado y acción
 */
export function CheckOption({ label, hint, checked, onToggle }: CheckOptionProps) {
  return (
    <label
      className={clsx(
        'flex cursor-pointer items-start gap-3 rounded-lg border px-3 py-2.5 transition-colors',
        checked ? 'border-indigo-300 bg-indigo-50/60' : 'border-slate-200 hover:bg-slate-50',
      )}
    >
      <input type="checkbox" checked={checked} onChange={onToggle} className="peer sr-only" />
      <span
        aria-hidden
        className={clsx(
          'mt-0.5 flex size-4 shrink-0 items-center justify-center rounded border transition-colors',
          'peer-focus-visible:ring-2 peer-focus-visible:ring-indigo-400 peer-focus-visible:ring-offset-1',
          checked ? 'border-indigo-600 bg-indigo-600 text-white' : 'border-slate-300 bg-white',
        )}
      >
        {checked && <Check className="size-3" strokeWidth={3} />}
      </span>
      <span className="min-w-0">
        <span className="block text-sm font-medium text-slate-800">{label}</span>
        {hint && <span className="mt-0.5 block text-xs text-slate-500">{hint}</span>}
      </span>
    </label>
  );
}
