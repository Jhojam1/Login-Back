import { Link } from 'react-router-dom';
import { ShieldAlert } from 'lucide-react';

/**
 * Pantalla para cuando alguien autenticado entra a una sección sin tener permiso.
 */
export function ForbiddenPage() {
  return (
    <div className="flex min-h-screen flex-col items-center justify-center bg-slate-50 px-6 text-center">
      <div className="flex size-12 items-center justify-center rounded-full bg-amber-50 text-amber-600">
        <ShieldAlert className="size-6" aria-hidden />
      </div>
      <h1 className="mt-4 text-lg font-semibold text-slate-900">Sin acceso</h1>
      <p className="mt-1 text-sm text-slate-500">No tienes permisos para ver esta sección.</p>
      <Link to="/panel" className="mt-6 text-sm font-medium text-indigo-600 hover:text-indigo-500">
        Volver al inicio
      </Link>
    </div>
  );
}
