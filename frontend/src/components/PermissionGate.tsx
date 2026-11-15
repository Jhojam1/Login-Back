import type { ReactNode } from 'react';
import { useAuth } from '../hooks/useAuth';

interface PermissionGateProps {
  /** Permisos aceptados; basta con tener uno de ellos. */
  permissions?: string[];
  /** Roles aceptados; basta con tener uno de ellos. */
  roles?: string[];
  /** Contenido que se muestra si se cumple la condición. */
  children: ReactNode;
  /** Contenido alternativo si no se cumple; por defecto no se muestra nada. */
  fallback?: ReactNode;
}

/**
 * Muestra u oculta una parte de la interfaz según los permisos del usuario.
 *
 * Permite escribir en las pantallas cosas como:
 *
 * ```tsx
 * <PermissionGate permissions={[PERMISSIONS.USER_DELETE]}>
 *   <Button variant="danger">Eliminar</Button>
 * </PermissionGate>
 * ```
 *
 * Recordatorio: esto solo mejora la experiencia, no protege nada. Quien edite el
 * JavaScript del navegador puede hacer aparecer el botón; lo que lo detendrá es el
 * `@PreAuthorize` del backend, que es donde vive la seguridad de verdad.
 *
 * @param props permisos o roles exigidos y el contenido a proteger
 */
export function PermissionGate({ permissions, roles, children, fallback = null }: PermissionGateProps) {
  const { hasAnyPermission, hasRole } = useAuth();

  const permissionOk = !permissions || hasAnyPermission(permissions);
  const roleOk = !roles || roles.some((role) => hasRole(role));

  return permissionOk && roleOk ? <>{children}</> : <>{fallback}</>;
}
