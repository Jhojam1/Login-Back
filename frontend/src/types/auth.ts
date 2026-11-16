/**
 * Tipos que describen el contrato con el backend.
 *
 * Tenerlos escritos permite que TypeScript avise en tiempo de compilación si el
 * backend cambia un campo y el frontend se queda desactualizado, en lugar de
 * descubrirlo con un `undefined` en producción.
 */

/** Datos públicos de un usuario, tal como los devuelve `/api/v1/auth/me`. */
export interface User {
  id: number;
  username: string;
  email: string;
  firstName: string | null;
  lastName: string | null;
  fullName: string;
  enabled: boolean;
  locked: boolean;
  roles: string[];
  permissions: string[];
  createdAt: string;
  lastLoginAt: string | null;
}

/** Respuesta de un login o un refresco correctos. */
/**
 * Respuesta del inicio de sesión. Tiene dos formas.
 *
 * Si la verificación en dos pasos está activada, el primer paso NO devuelve tokens:
 * solo `mfaRequired: true` y el `challengeId` con el que continuar. Los tokens llegan
 * en el segundo paso, al enviar el código correcto.
 */
export interface AuthResponse {
  accessToken?: string;
  refreshToken?: string;
  tokenType?: string;
  expiresIn?: number;
  user?: User;
  /** `true` si hay que introducir el código enviado por correo. */
  mfaRequired: boolean;
  /** Identificador del intento, necesario para verificar el código. */
  challengeId?: string;
  /** Correo parcialmente tapado, para decir a dónde se envió el código. */
  maskedEmail?: string;
  /** Segundos que dura el código. */
  codeExpiresInSeconds?: number;
}

/** Credenciales del formulario de acceso. */
export interface LoginRequest {
  identifier: string;
  password: string;
}

/** Datos del formulario de registro. */
export interface RegisterRequest {
  username: string;
  email: string;
  password: string;
  firstName?: string;
  lastName?: string;
}

/**
 * Datos para crear un usuario desde el panel.
 *
 * Es distinto de `RegisterRequest` porque aquí sí se eligen los roles: el registro
 * público nunca permite elegirlos.
 */
export interface CreateUserRequest {
  username: string;
  email: string;
  password: string;
  firstName?: string;
  lastName?: string;
  roles: string[];
}

/** Datos para crear o modificar un rol. */
export interface RoleRequest {
  name: string;
  description: string;
  permissions: string[];
}

/** Rol con el conjunto de permisos que concede. */
export interface Role {
  id: number;
  name: string;
  description: string;
  permissions: string[];
}

/** Permiso individual del catálogo. */
export interface Permission {
  id: number;
  name: string;
  description: string;
  /**
   * `true` si el permiso solo decide qué muestra la interfaz.
   *
   * Los de `false` los verifica el backend con `@PreAuthorize` y protegen una
   * operación real; los de `true` se crearon desde el panel y solo sirven para
   * mostrar u ocultar menús y secciones.
   */
  uiOnly: boolean;
}

/** Datos para crear un permiso de solo interfaz. */
export interface PermissionRequest {
  name: string;
  description: string;
}

/** Entrada del menú de navegación, tal como llega del backend. */
export interface MenuItem {
  id: number;
  label: string;
  path: string;
  /** Nombre del icono del catálogo de `utils/icons`. */
  icon?: string;
  /** Permiso necesario para verla; ausente si la ve cualquiera. */
  requiredPermission?: string;
  displayOrder: number;
  visible: boolean;
  /** `true` si la creó el sistema y por tanto no se puede eliminar. */
  systemItem: boolean;
}

/** Datos para crear o modificar una entrada del menú. */
export interface MenuItemRequest {
  label: string;
  path: string;
  icon: string;
  requiredPermission: string;
  displayOrder: number;
  visible: boolean;
}

/** Página de resultados devuelta por los endpoints paginados de Spring Data. */
export interface Page<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  number: number;
  size: number;
  first: boolean;
  last: boolean;
}

/** Estructura común de todos los errores de la API. */
export interface ApiError {
  timestamp: string;
  status: number;
  error: string;
  code: string;
  message: string;
  path: string;
  details?: Array<{ field: string; message: string }>;
}

/**
 * Catálogo de permisos del backend.
 *
 * Se declaran como constantes para que el editor autocomplete y para que un error
 * de tipeo en un nombre de permiso no pase desapercibido.
 */
export const PERMISSIONS = {
  USER_READ: 'USER_READ',
  USER_CREATE: 'USER_CREATE',
  USER_UPDATE: 'USER_UPDATE',
  USER_DELETE: 'USER_DELETE',
  ROLE_READ: 'ROLE_READ',
  ROLE_WRITE: 'ROLE_WRITE',
  ROLE_ASSIGN: 'ROLE_ASSIGN',
  PERMISSION_READ: 'PERMISSION_READ',
  PERMISSION_WRITE: 'PERMISSION_WRITE',
  MENU_WRITE: 'MENU_WRITE',
  FILE_UPLOAD: 'FILE_UPLOAD',
  FILE_READ: 'FILE_READ',
  FILE_DELETE: 'FILE_DELETE',
} as const;

/** Catálogo de roles del backend. */
export const ROLES = {
  ADMIN: 'ROLE_ADMIN',
  MANAGER: 'ROLE_MANAGER',
  USER: 'ROLE_USER',
} as const;

/** Datos del segundo paso del inicio de sesión. */
export interface VerifyCodeRequest {
  challengeId: string;
  code: string;
}
