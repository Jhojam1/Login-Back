import { Link } from 'react-router-dom';
import { Compass } from 'lucide-react';

/**
 * Pantalla para rutas que no existen.
 */
export function NotFoundPage() {
  return (
    <div className="flex min-h-screen flex-col items-center justify-center bg-slate-50 px-6 text-center">
      <div className="flex size-12 items-center justify-center rounded-full bg-indigo-50 text-indigo-600">
        <Compass className="size-6" aria-hidden />
      </div>
      <h1 className="mt-4 text-lg font-semibold text-slate-900">Página no encontrada</h1>
      <p className="mt-1 text-sm text-slate-500">El enlace no existe o se movió.</p>
      <Link to="/panel" className="mt-6 text-sm font-medium text-indigo-600 hover:text-indigo-500">
        Ir al inicio
      </Link>
    </div>
  );
}
