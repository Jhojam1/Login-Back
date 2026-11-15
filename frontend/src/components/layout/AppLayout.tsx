import { useEffect, useState } from 'react';
import { NavLink, Outlet, useLocation, useNavigate } from 'react-router-dom';
import clsx from 'clsx';
import { LogOut, Menu as MenuIcon, ShieldCheck, X } from 'lucide-react';
import { useAuth } from '../../hooks/useAuth';
import { useMenu } from '../../hooks/useMenu';
import { menuIcon } from '../../utils/icons';
import { roleLabel } from '../../utils/labels';

/**
 * Estructura común de las pantallas que hay tras el login: barra lateral y contenido.
 *
 * Se usa barra lateral porque las entradas del menú se crean desde el panel y su número
 * crece: en una barra lateral caben veinte sin problema, en una barra superior no.
 *
 * Las entradas NO están escritas aquí. Llegan del backend, ya filtradas por los permisos
 * de quien pregunta, así que una sección nueva aparece sin tocar este archivo.
 *
 * En móvil la barra se oculta y se abre como panel deslizante.
 */
export function AppLayout() {
  const { user, logout } = useAuth();
  const { items } = useMenu();
  const navigate = useNavigate();
  const location = useLocation();
  const [isSidebarOpen, setIsSidebarOpen] = useState(false);

  /** Al cambiar de página en móvil, el panel se cierra solo. */
  useEffect(() => {
    setIsSidebarOpen(false);
  }, [location.pathname]);

  /**
   * Cierra la sesión y vuelve a la pantalla de acceso.
   */
  async function handleLogout() {
    await logout();
    navigate('/login', { replace: true });
  }

  const initials = (user?.firstName?.[0] ?? user?.username[0] ?? '?').toUpperCase()
    + (user?.lastName?.[0] ?? '').toUpperCase();

  return (
    <div className="min-h-screen bg-slate-50">
      {/* Fondo oscuro tras el panel en móvil */}
      {isSidebarOpen && (
        <div
          className="fixed inset-0 z-30 animate-fade bg-slate-900/40 lg:hidden"
          onClick={() => setIsSidebarOpen(false)}
          aria-hidden
        />
      )}

      <aside
        className={clsx(
          'fixed inset-y-0 left-0 z-40 flex w-64 flex-col border-r border-slate-200 bg-white',
          'transition-transform duration-200 lg:translate-x-0',
          isSidebarOpen ? 'translate-x-0' : '-translate-x-full',
        )}
      >
        <div className="flex h-16 items-center gap-2.5 px-5">
          <div className="flex size-8 items-center justify-center rounded-lg bg-indigo-600 text-white">
            <ShieldCheck className="size-4.5" />
          </div>
          <span className="font-semibold text-slate-900">Panel</span>

          <button
            type="button"
            onClick={() => setIsSidebarOpen(false)}
            aria-label="Cerrar menú"
            className="ml-auto rounded-lg p-1 text-slate-400 hover:bg-slate-100 lg:hidden"
          >
            <X className="size-5" />
          </button>
        </div>

        <nav className="flex-1 space-y-0.5 overflow-y-auto px-3 py-4">
          {items.map((item) => {
            const Icon = menuIcon(item.icon);

            return (
              <NavLink
                key={item.id}
                to={item.path}
                end={item.path === '/panel'}
                className={({ isActive }) =>
                  clsx(
                    'group flex items-center gap-3 rounded-lg px-3 py-2 text-sm font-medium transition-colors',
                    isActive
                      ? 'bg-indigo-50 text-indigo-700'
                      : 'text-slate-600 hover:bg-slate-100 hover:text-slate-900',
                  )
                }
              >
                {({ isActive }) => (
                  <>
                    <Icon
                      className={clsx(
                        'size-4.5 shrink-0',
                        isActive ? 'text-indigo-600' : 'text-slate-400 group-hover:text-slate-600',
                      )}
                      aria-hidden
                    />
                    <span className="truncate">{item.label}</span>
                  </>
                )}
              </NavLink>
            );
          })}
        </nav>

        <div className="border-t border-slate-200 p-3">
          <div className="flex items-center gap-3 rounded-lg px-2 py-2">
            <div className="flex size-9 shrink-0 items-center justify-center rounded-full bg-gradient-to-br from-indigo-500 to-violet-500 text-xs font-semibold text-white">
              {initials}
            </div>
            <div className="min-w-0 flex-1">
              <p className="truncate text-sm font-medium text-slate-900">{user?.fullName}</p>
              <p className="truncate text-xs text-slate-500">
                {user?.roles.map(roleLabel).join(', ')}
              </p>
            </div>
            <button
              type="button"
              onClick={handleLogout}
              aria-label="Cerrar sesión"
              title="Cerrar sesión"
              className="rounded-lg p-1.5 text-slate-400 transition-colors hover:bg-slate-100 hover:text-slate-700"
            >
              <LogOut className="size-4" />
            </button>
          </div>
        </div>
      </aside>

      <div className="lg:pl-64">
        {/* Barra superior solo en móvil, para abrir el menú */}
        <header className="sticky top-0 z-20 flex h-14 items-center gap-3 border-b border-slate-200 bg-white/90 px-4 backdrop-blur lg:hidden">
          <button
            type="button"
            onClick={() => setIsSidebarOpen(true)}
            aria-label="Abrir menú"
            className="rounded-lg p-1.5 text-slate-600 hover:bg-slate-100"
          >
            <MenuIcon className="size-5" />
          </button>
          <span className="font-semibold text-slate-900">Panel</span>
        </header>

        <main className="mx-auto max-w-6xl px-4 py-8 sm:px-8">
          <Outlet />
        </main>
      </div>
    </div>
  );
}
