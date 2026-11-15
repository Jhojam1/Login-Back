import { Loader2 } from 'lucide-react';

/**
 * Pantalla de carga mientras se comprueba la sesión guardada.
 */
export function FullPageLoader() {
  return (
    <div className="flex min-h-screen items-center justify-center bg-slate-50">
      <Loader2 className="size-6 animate-spin text-indigo-500" role="status" aria-label="Cargando" />
    </div>
  );
}
