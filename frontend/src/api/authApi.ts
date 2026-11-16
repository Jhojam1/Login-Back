import { apiClient } from './client';
import type {
  AuthResponse,
  CreateUserRequest,
  LoginRequest,
  MenuItem,
  MenuItemRequest,
  Page,
  Permission,
  PermissionRequest,
  RegisterRequest,
  Role,
  RoleRequest,
  User,
} from '../types/auth';

/**
 * Llamadas al backend relacionadas con la autenticación.
 *
 * Agrupar aquí las rutas evita que las cadenas `'/auth/login'` aparezcan repartidas
 * por los componentes: si el backend cambia una ruta, se corrige en un solo sitio.
 */
export const authApi = {
  /**
   * Crea una cuenta nueva.
   *
   * @param payload datos del formulario de registro
   * @returns el usuario creado
   */
  async register(payload: RegisterRequest): Promise<User> {
    const { data } = await apiClient.post<User>('/auth/register', payload);
    return data;
  },

  /**
   * Inicia sesión.
   *
   * @param payload usuario o correo y contraseña
   * @returns los tokens y los datos del usuario
   */
  async login(payload: LoginRequest): Promise<AuthResponse> {
    const { data } = await apiClient.post<AuthResponse>('/auth/login', payload);
    return data;
  },

  /**
   * Segundo paso del inicio de sesión: envía el código recibido por correo.
   *
   * @param challengeId identificador que devolvió el login
   * @param code        código que escribió la persona
   * @returns los tokens y los datos del usuario
   */
  async verifyCode(challengeId: string, code: string): Promise<AuthResponse> {
    const { data } = await apiClient.post<AuthResponse>('/auth/verify-code', {
      challengeId,
      code,
    });
    return data;
  },

  /**
   * Pide que se vuelva a enviar el código.
   *
   * @param challengeId identificador del intento en curso
   * @returns la respuesta con el intento nuevo, todavía sin tokens
   */
  async resendCode(challengeId: string): Promise<AuthResponse> {
    const { data } = await apiClient.post<AuthResponse>('/auth/resend-code', { challengeId });
    return data;
  },

  /**
   * Cierra la sesión del dispositivo actual revocando el token de refresco.
   *
   * @param refreshToken token a invalidar en el servidor
   */
  async logout(refreshToken: string): Promise<void> {
    await apiClient.post('/auth/logout', { refreshToken });
  },

  /**
   * Devuelve el perfil del usuario autenticado, con sus roles y permisos actuales.
   *
   * @returns los datos del usuario
   */
  async me(): Promise<User> {
    const { data } = await apiClient.get<User>('/auth/me');
    return data;
  },

  /**
   * Cambia la contraseña del usuario autenticado.
   *
   * @param currentPassword contraseña actual
   * @param newPassword     contraseña nueva
   */
  async changePassword(currentPassword: string, newPassword: string): Promise<void> {
    await apiClient.post('/auth/change-password', { currentPassword, newPassword });
  },
};

/**
 * Llamadas al backend para administrar usuarios, roles y permisos.
 */
export const adminApi = {
  /**
   * Crea un usuario con los roles indicados.
   *
   * @param payload datos de la cuenta y roles a asignar
   * @returns el usuario creado
   */
  async createUser(payload: CreateUserRequest): Promise<User> {
    const { data } = await apiClient.post<User>('/users', payload);
    return data;
  },

  /**
   * Lista los usuarios de forma paginada.
   *
   * @param page número de página empezando en 0
   * @param size usuarios por página
   * @returns la página de resultados
   */
  async listUsers(page = 0, size = 10): Promise<Page<User>> {
    const { data } = await apiClient.get<Page<User>>('/users', {
      params: { page, size, sort: 'id,asc' },
    });
    return data;
  },

  /**
   * Activa o desactiva una cuenta.
   *
   * @param id      identificador del usuario
   * @param enabled nuevo estado
   * @returns el usuario actualizado
   */
  async setUserEnabled(id: number, enabled: boolean): Promise<User> {
    const { data } = await apiClient.patch<User>(`/users/${id}/status`, null, {
      params: { enabled },
    });
    return data;
  },

  /**
   * Reemplaza los roles de un usuario.
   *
   * @param id    identificador del usuario
   * @param roles nombres de los roles que quedarán asignados
   * @returns el usuario actualizado
   */
  async assignRoles(id: number, roles: string[]): Promise<User> {
    const { data } = await apiClient.put<User>(`/users/${id}/roles`, { roles });
    return data;
  },

  /**
   * Elimina un usuario.
   *
   * @param id identificador del usuario a borrar
   */
  async deleteUser(id: number): Promise<void> {
    await apiClient.delete(`/users/${id}`);
  },

  /**
   * Lista los roles disponibles con sus permisos.
   *
   * @returns la lista de roles
   */
  async listRoles(): Promise<Role[]> {
    const { data } = await apiClient.get<Role[]>('/roles');
    return data;
  },

  /**
   * Crea un rol nuevo combinando permisos del catálogo.
   *
   * @param payload nombre, descripción y permisos del rol
   * @returns el rol creado
   */
  async createRole(payload: RoleRequest): Promise<Role> {
    const { data } = await apiClient.post<Role>('/roles', payload);
    return data;
  },

  /**
   * Actualiza la descripción y los permisos de un rol.
   *
   * @param id      identificador del rol
   * @param payload nuevos datos del rol
   * @returns el rol actualizado
   */
  async updateRole(id: number, payload: RoleRequest): Promise<Role> {
    const { data } = await apiClient.put<Role>(`/roles/${id}`, payload);
    return data;
  },

  /**
   * Elimina un rol personalizado.
   *
   * @param id identificador del rol
   */
  async deleteRole(id: number): Promise<void> {
    await apiClient.delete(`/roles/${id}`);
  },

  /**
   * Lista el catálogo de permisos.
   *
   * @returns la lista de permisos
   */
  async listPermissions(): Promise<Permission[]> {
    const { data } = await apiClient.get<Permission[]>('/permissions');
    return data;
  },

  /**
   * Crea un permiso de solo interfaz.
   *
   * @param payload nombre y descripción del permiso
   * @returns el permiso creado
   */
  async createPermission(payload: PermissionRequest): Promise<Permission> {
    const { data } = await apiClient.post<Permission>('/permissions', payload);
    return data;
  },

  /**
   * Elimina un permiso de solo interfaz.
   *
   * @param id identificador del permiso
   */
  async deletePermission(id: number): Promise<void> {
    await apiClient.delete(`/permissions/${id}`);
  },
};

/**
 * Llamadas al backend relacionadas con el menú de navegación.
 */
export const menuApi = {
  /**
   * Devuelve el menú que corresponde a la persona autenticada.
   *
   * El backend ya lo filtra por permisos, así que lo que llega es exactamente lo que
   * se debe pintar.
   *
   * @returns las entradas visibles, ya ordenadas
   */
  async myMenu(): Promise<MenuItem[]> {
    const { data } = await apiClient.get<MenuItem[]>('/menu');
    return data;
  },

  /**
   * Devuelve todas las entradas, incluidas las ocultas, para la pantalla de gestión.
   *
   * @returns todas las entradas ordenadas
   */
  async listAll(): Promise<MenuItem[]> {
    const { data } = await apiClient.get<MenuItem[]>('/menu/all');
    return data;
  },

  /**
   * Crea una entrada del menú.
   *
   * @param payload datos de la entrada
   * @returns la entrada creada
   */
  async create(payload: MenuItemRequest): Promise<MenuItem> {
    const { data } = await apiClient.post<MenuItem>('/menu', payload);
    return data;
  },

  /**
   * Actualiza una entrada del menú.
   *
   * @param id      identificador de la entrada
   * @param payload nuevos datos
   * @returns la entrada actualizada
   */
  async update(id: number, payload: MenuItemRequest): Promise<MenuItem> {
    const { data } = await apiClient.put<MenuItem>(`/menu/${id}`, payload);
    return data;
  },

  /**
   * Elimina una entrada del menú.
   *
   * @param id identificador de la entrada
   */
  async remove(id: number): Promise<void> {
    await apiClient.delete(`/menu/${id}`);
  },
};
