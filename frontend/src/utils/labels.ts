/**
 * Traducción de los códigos internos a texto legible.
 *
 * El backend trabaja con constantes como `ROLE_ADMIN` o `USER_DELETE` porque es lo que
 * comprueba el código. A quien usa la aplicación eso no le dice nada, así que la
 * interfaz nunca muestra el código a secas: lo traduce aquí.
 */

/** Nombres legibles de los roles que trae el sistema. */
const ROLE_LABELS: Record<string, string> = {
  ROLE_ADMIN: 'Administrador',
  ROLE_MANAGER: 'Gestor',
  ROLE_USER: 'Usuario',
};

/** Nombres legibles de los permisos del catálogo. */
const PERMISSION_LABELS: Record<string, string> = {
  USER_READ: 'Ver usuarios',
  USER_CREATE: 'Crear usuarios',
  USER_UPDATE: 'Editar usuarios',
  USER_DELETE: 'Eliminar usuarios',
  ROLE_READ: 'Ver roles',
  ROLE_WRITE: 'Crear y editar roles',
  ROLE_ASSIGN: 'Asignar roles',
  PERMISSION_READ: 'Ver permisos',
  PERMISSION_WRITE: 'Crear permisos de interfaz',
  MENU_WRITE: 'Gestionar el menú',
  FILE_UPLOAD: 'Subir archivos',
  FILE_READ: 'Ver y descargar archivos',
  FILE_DELETE: 'Eliminar archivos',
};

/**
 * Devuelve el nombre legible de un rol.
 *
 * Si es un rol creado a mano desde el panel y no está en la tabla, se limpia el código:
 * `ROLE_AUDITOR` pasa a ser `Auditor`. Así un rol nuevo se ve bien sin tocar este archivo.
 *
 * @param role nombre interno del rol, por ejemplo `ROLE_ADMIN`
 * @returns texto para mostrar en pantalla
 */
export function roleLabel(role: string): string {
  if (ROLE_LABELS[role]) {
    return ROLE_LABELS[role];
  }

  const bare = role.replace(/^ROLE_/, '').toLowerCase().replace(/_/g, ' ');
  return bare.charAt(0).toUpperCase() + bare.slice(1);
}

/**
 * Devuelve el nombre legible de un permiso.
 *
 * @param permission nombre interno del permiso, por ejemplo `USER_DELETE`
 * @returns texto para mostrar en pantalla
 */
export function permissionLabel(permission: string): string {
  if (PERMISSION_LABELS[permission]) {
    return PERMISSION_LABELS[permission];
  }

  const bare = permission.toLowerCase().replace(/_/g, ' ');
  return bare.charAt(0).toUpperCase() + bare.slice(1);
}

/**
 * Agrupa los permisos por el recurso al que afectan, para mostrarlos ordenados en el
 * formulario de roles en lugar de como una lista plana de once casillas.
 *
 * @param permissions nombres de permiso a agrupar
 * @returns pares de [nombre del grupo, permisos de ese grupo]
 */
export function groupPermissions(permissions: string[]): Array<[string, string[]]> {
  const GROUPS: Record<string, string> = {
    USER: 'Usuarios',
    ROLE: 'Roles',
    PERMISSION: 'Permisos',
    MENU: 'Menú',
    FILE: 'Archivos',
  };

  const grouped = new Map<string, string[]>();

  for (const permission of [...permissions].sort()) {
    const prefix = permission.split('_')[0];
    const group = GROUPS[prefix] ?? 'Otros';
    grouped.set(group, [...(grouped.get(group) ?? []), permission]);
  }

  return [...grouped.entries()];
}

/**
 * Da formato a una fecha ISO del backend.
 *
 * @param isoDate fecha en formato ISO-8601, o `null`
 * @returns fecha legible en español, o un guion si no hay dato
 */
export function formatDate(isoDate: string | null): string {
  if (!isoDate) {
    return '—';
  }

  return new Date(isoDate).toLocaleDateString('es-ES', {
    day: 'numeric',
    month: 'long',
    year: 'numeric',
  });
}
