import { Hammer } from 'lucide-react';

/**
 * Pantalla para las entradas de menú cuya sección todavía no está programada.
 *
 * Crear una entrada desde el panel crea el enlace, no la pantalla. Mostrar esto es más
 * honesto que un 404 que parecería un fallo del sistema.
 */
export function EmptySectionPage() {
  return (
    <div className="flex min-h-[60vh] animate-rise flex-col items-center justify-center text-center">
      <div className="flex size-12 items-center justify-center rounded-full bg-slate-100 text-slate-400">
        <Hammer className="size-6" aria-hidden />
      </div>
      <h1 className="mt-4 text-lg font-semibold text-slate-900">Sección en construcción</h1>
      <p className="mt-1 text-sm text-slate-500">Esta página todavía no tiene contenido.</p>
    </div>
  );
}
