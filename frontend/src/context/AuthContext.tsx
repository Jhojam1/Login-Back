import {
  createContext,
  useCallback,
  useEffect,
  useMemo,
  useState,
  type ReactNode,
} from 'react';
import { authApi } from '../api/authApi';
import { setSessionExpiredHandler } from '../api/client';
import { tokenStorage } from '../api/tokenStorage';
import type { AuthResponse, LoginRequest, RegisterRequest, User } from '../types/auth';

/**
 * Estado y acciones de autenticación que cualquier componente puede consumir.
 */
export interface AuthContextValue {
  /** Usuario autenticado, o `null` si no hay sesión. */
  user: User | null;
  /** `true` mientras se comprueba la sesión guardada al cargar la aplicación. */
  isLoading: boolean;
  /** Atajo para saber si hay sesión activa. */
  isAuthenticated: boolean;
  /** Inicia sesión con las credenciales indicadas. */
  /** Primer paso. Si devuelve `mfaRequired`, falta validar el código del correo. */
  login: (credentials: LoginRequest) => Promise<AuthResponse>;
  /** Segundo paso: valida el código y abre la sesión. */
  verifyCode: (challengeId: string, code: string) => Promise<void>;
  /** Registra una cuenta nueva (no inicia sesión automáticamente). */
  register: (payload: RegisterRequest) => Promise<void>;
  /** Cierra la sesión y limpia los tokens. */
  logout: () => Promise<void>;
  /** Vuelve a pedir el perfil al servidor (útil tras cambiar de roles). */
  refreshProfile: () => Promise<void>;
  /** Comprueba si el usuario tiene un permiso concreto. */
  hasPermission: (permission: string) => boolean;
  /** Comprueba si el usuario tiene al menos uno de los permisos indicados. */
  hasAnyPermission: (permissions: string[]) => boolean;
  /** Comprueba si el usuario tiene un rol concreto. */
  hasRole: (role: string) => boolean;
}

export const AuthContext = createContext<AuthContextValue | undefined>(undefined);

/**
 * Proveedor que mantiene la sesión viva para toda la aplicación.
 *
 * Es la única fuente de verdad sobre quién está conectado: los componentes no leen
 * `localStorage` ni deciden por su cuenta si alguien puede ver algo, se lo preguntan
 * a este contexto.
 *
 * @param children árbol de componentes que tendrá acceso a la sesión
 */
export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<User | null>(null);
  const [isLoading, setIsLoading] = useState(true);

  /**
   * Borra la sesión del navegador y del estado de React.
   */
  const clearSession = useCallback(() => {
    tokenStorage.clear();
    setUser(null);
  }, []);

  /**
   * Al cargar la aplicación, si había tokens guardados se comprueba contra el
   * servidor que la sesión siga siendo válida.
   *
   * Es importante preguntar al backend y no confiar en lo que hay en el navegador:
   * un administrador pudo desactivar la cuenta mientras la pestaña estaba cerrada.
   */
  useEffect(() => {
    let cancelled = false;

    async function restoreSession() {
      if (!tokenStorage.hasSession()) {
        setIsLoading(false);
        return;
      }

      try {
        const profile = await authApi.me();
        if (!cancelled) {
          setUser(profile);
        }
      } catch {
        if (!cancelled) {
          clearSession();
        }
      } finally {
        if (!cancelled) {
          setIsLoading(false);
        }
      }
    }

    void restoreSession();

    return () => {
      cancelled = true;
    };
  }, [clearSession]);

  /**
   * Conecta el cliente HTTP con este contexto: cuando el token de refresco deja de
   * servir, el interceptor avisa y aquí se limpia la sesión.
   */
  useEffect(() => {
    setSessionExpiredHandler(clearSession);
  }, [clearSession]);

  /**
   * Inicia sesión y guarda los tokens y el perfil.
   *
   * @param credentials usuario o correo y contraseña
   */
  /**
   * Guarda los tokens y el perfil que llegan en una respuesta de login completada.
   *
   * <p>Lo usan tanto el login directo (sin verificación) como el segundo paso, para
   * que la sesión se abra exactamente igual en los dos casos.</p>
   *
   * @param response respuesta que ya contiene los tokens
   */
  const applySession = useCallback((response: AuthResponse) => {
    if (!response.accessToken || !response.refreshToken || !response.user) {
      throw new Error('La respuesta del servidor no trae la sesión completa');
    }
    tokenStorage.save(response.accessToken, response.refreshToken);
    setUser(response.user);
  }, []);

  const login = useCallback(async (credentials: LoginRequest): Promise<AuthResponse> => {
    const response = await authApi.login(credentials);

    // Con la verificación en dos pasos, el primer paso NO trae tokens: solo el
    // identificador del intento. La sesión no se abre hasta validar el código.
    if (!response.mfaRequired) {
      applySession(response);
    }

    return response;
  }, [applySession]);

  /**
   * Completa el inicio de sesión con el código recibido por correo.
   *
   * @param challengeId identificador que devolvió el login
   * @param code        código introducido
   */
  const verifyCode = useCallback(
    async (challengeId: string, code: string) => {
      applySession(await authApi.verifyCode(challengeId, code));
    },
    [applySession],
  );

  /**
   * Registra una cuenta nueva.
   *
   * No inicia sesión automáticamente: así la persona confirma sus credenciales
   * escribiéndolas una vez más, que es lo que esperan la mayoría de las aplicaciones.
   *
   * @param payload datos del formulario de registro
   */
  const register = useCallback(async (payload: RegisterRequest) => {
    await authApi.register(payload);
  }, []);

  /**
   * Cierra la sesión avisando al servidor para que revoque el token de refresco.
   */
  const logout = useCallback(async () => {
    const refreshToken = tokenStorage.getRefreshToken();
    try {
      if (refreshToken) {
        await authApi.logout(refreshToken);
      }
    } catch {
      // Si la llamada falla (por ejemplo sin red) igualmente se cierra la sesión
      // en el navegador: nunca debe quedarse alguien atrapado dentro.
    } finally {
      clearSession();
    }
  }, [clearSession]);

  /**
   * Vuelve a cargar el perfil desde el servidor.
   */
  const refreshProfile = useCallback(async () => {
    const profile = await authApi.me();
    setUser(profile);
  }, []);

  /**
   * Comprueba si el usuario tiene un permiso.
   *
   * @param permission nombre del permiso, por ejemplo `USER_DELETE`
   * @returns `true` si lo tiene
   */
  const hasPermission = useCallback(
    (permission: string) => user?.permissions.includes(permission) ?? false,
    [user],
  );

  /**
   * Comprueba si el usuario tiene al menos uno de los permisos indicados.
   *
   * @param permissions lista de permisos aceptables
   * @returns `true` si tiene alguno
   */
  const hasAnyPermission = useCallback(
    (permissions: string[]) => permissions.some((permission) => hasPermission(permission)),
    [hasPermission],
  );

  /**
   * Comprueba si el usuario tiene un rol.
   *
   * @param role nombre del rol, por ejemplo `ROLE_ADMIN`
   * @returns `true` si lo tiene
   */
  const hasRole = useCallback(
    (role: string) => user?.roles.includes(role) ?? false,
    [user],
  );

  // `useMemo` evita que todos los componentes que consumen el contexto se
  // vuelvan a dibujar en cada render del proveedor.
  const value = useMemo<AuthContextValue>(
    () => ({
      user,
      isLoading,
      isAuthenticated: user !== null,
      login,
      verifyCode,
      register,
      logout,
      refreshProfile,
      hasPermission,
      hasAnyPermission,
      hasRole,
    }),
    [user, isLoading, login, verifyCode, register, logout, refreshProfile,
      hasPermission, hasAnyPermission, hasRole],
  );

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}
