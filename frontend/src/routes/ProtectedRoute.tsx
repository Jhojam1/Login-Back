import { Navigate, Outlet, useLocation } from 'react-router-dom';
import { useAuth } from '../hooks/useAuth';
import { FullPageLoader } from '../components/ui/FullPageLoader';

interface ProtectedRouteProps {
  /** Permisos aceptados; basta con tener uno. Si se omite, solo se exige estar autenticado. */
  requiredPermissions?: string[];
  /** Roles aceptados; basta con tener uno. */
  requiredRoles?: string[];
}

/**
 * Guarda de rutas: decide si se puede entrar a una sección de la aplicación.
 *
 * Distingue tres situaciones:
 *
 * - Todavía no se sabe si hay sesión (se está consultando al servidor): muestra un
 *   cargador. Sin esto, al recargar la página se expulsaría a la persona por un
 *   instante antes de confirmar su sesión.
 * - No hay sesión: lleva al login recordando a dónde quería ir, para devolverla
 *   allí después de entrar.
 * - Hay sesión pero faltan permisos: lleva a la página de acceso denegado.
 *
 * Importante: esto es comodidad de interfaz, NO seguridad. La seguridad real está en
 * el backend con `@PreAuthorize`; aquí solo se evita mostrar botones que fallarían.
 *
 * @param props permisos o roles exigidos por la ruta
 */
export function ProtectedRoute({ requiredPermissions, requiredRoles }: ProtectedRouteProps) {
  const { isAuthenticated, isLoading, hasAnyPermission, hasRole } = useAuth();
  const location = useLocation();

  if (isLoading) {
    return <FullPageLoader />;
  }

  if (!isAuthenticated) {
    return <Navigate to="/login" state={{ from: location }} replace />;
  }

  const permissionOk = !requiredPermissions || hasAnyPermission(requiredPermissions);
  const roleOk = !requiredRoles || requiredRoles.some((role) => hasRole(role));

  if (!permissionOk || !roleOk) {
    return <Navigate to="/sin-acceso" replace />;
  }

  return <Outlet />;
}

/**
 * Guarda inversa: impide ver el login o el registro a quien ya inició sesión.
 *
 * Sin ella, pulsar "atrás" tras entrar devolvería al formulario de acceso.
 */
export function PublicOnlyRoute() {
  const { isAuthenticated, isLoading } = useAuth();

  if (isLoading) {
    return <FullPageLoader />;
  }

  return isAuthenticated ? <Navigate to="/panel" replace /> : <Outlet />;
}
