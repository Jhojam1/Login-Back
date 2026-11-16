import axios, {
  AxiosError,
  type AxiosInstance,
  type InternalAxiosRequestConfig,
} from 'axios';
import { tokenStorage } from './tokenStorage';
import type { ApiError, AuthResponse } from '../types/auth';

/**
 * Cliente HTTP único de la aplicación.
 *
 * Resuelve dos cosas de forma transparente para el resto del código:
 *
 * 1. Añade la cabecera `Authorization: Bearer ...` a cada petición.
 * 2. Cuando el backend responde 401 porque el token caducó, pide uno nuevo con el
 *    token de refresco y REINTENTA la petición original. Para la persona usuaria
 *    el proceso es invisible: no se le expulsa cada 15 minutos.
 */

/** URL base del backend; vacía en desarrollo porque Vite hace de proxy. */
const API_URL = import.meta.env.VITE_API_URL ?? '';

/** Marca interna para no entrar en un bucle infinito de reintentos. */
interface RetriableRequest extends InternalAxiosRequestConfig {
  _retry?: boolean;
}

export const apiClient: AxiosInstance = axios.create({
  baseURL: `${API_URL}/api/v1`,
  headers: { 'Content-Type': 'application/json' },
  timeout: 15000,
});

/**
 * Función que la capa de autenticación registra para ser avisada cuando la sesión
 * deja de ser válida y hay que devolver a la persona a la pantalla de acceso.
 */
let onSessionExpired: (() => void) | null = null;

/**
 * Registra el manejador que se ejecuta cuando la sesión expira definitivamente.
 *
 * @param handler función a ejecutar, normalmente el `logout` del contexto
 */
export function setSessionExpiredHandler(handler: () => void): void {
  onSessionExpired = handler;
}

// ---------------------------------------------------------------------------
// Interceptor de petición: añade el token a cada llamada
// ---------------------------------------------------------------------------
apiClient.interceptors.request.use((config) => {
  const token = tokenStorage.getAccessToken();
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

// ---------------------------------------------------------------------------
// Renovación de token con cola de espera
// ---------------------------------------------------------------------------

/** Indica si ya hay una renovación en curso. */
let isRefreshing = false;

/** Peticiones que llegaron mientras se renovaba y esperan el token nuevo. */
let pendingQueue: Array<{
  resolve: (token: string) => void;
  reject: (error: unknown) => void;
}> = [];

/**
 * Libera las peticiones que estaban esperando a que terminara la renovación.
 *
 * Sin esta cola, si la página lanza cinco llamadas a la vez y todas reciben 401,
 * se dispararían cinco refrescos simultáneos; como el backend rota los tokens,
 * cuatro de ellos fallarían y cerrarían la sesión sin motivo.
 *
 * @param error error ocurrido, o `null` si la renovación fue bien
 * @param token token de acceso nuevo
 */
function flushQueue(error: unknown, token: string | null): void {
  pendingQueue.forEach((pending) => {
    if (token) {
      pending.resolve(token);
    } else {
      pending.reject(error);
    }
  });
  pendingQueue = [];
}

/**
 * Pide un token de acceso nuevo usando el token de refresco.
 *
 * Usa `axios` directamente y no `apiClient` a propósito: así la llamada no pasa por
 * estos interceptores y no puede provocar una recursión infinita.
 *
 * @returns el token de acceso nuevo
 * @throws si el token de refresco ya no es válido
 */
async function requestNewAccessToken(): Promise<string> {
  const refreshToken = tokenStorage.getRefreshToken();
  if (!refreshToken) {
    throw new Error('No hay token de refresco disponible');
  }

  const { data } = await axios.post<AuthResponse>(
    `${API_URL}/api/v1/auth/refresh`,
    { refreshToken },
    { headers: { 'Content-Type': 'application/json' } },
  );

  // El refresco siempre devuelve tokens: la verificación en dos pasos solo ocurre
  // en el login inicial. Aun así se comprueba, porque los campos son opcionales en
  // el tipo y un fallo silencioso aquí dejaría la sesión en un estado imposible.
  if (!data.accessToken || !data.refreshToken) {
    throw new Error('El servidor no devolvió los tokens al refrescar la sesión');
  }

  tokenStorage.save(data.accessToken, data.refreshToken);
  return data.accessToken;
}

// ---------------------------------------------------------------------------
// Interceptor de respuesta: renueva el token ante un 401 y reintenta
// ---------------------------------------------------------------------------
apiClient.interceptors.response.use(
  (response) => response,
  async (error: AxiosError<ApiError>) => {
    const originalRequest = error.config as RetriableRequest | undefined;

    const shouldAttemptRefresh =
      error.response?.status === 401 &&
      originalRequest &&
      !originalRequest._retry &&
      !originalRequest.url?.includes('/auth/login') &&
      !originalRequest.url?.includes('/auth/refresh');

    if (!shouldAttemptRefresh) {
      return Promise.reject(error);
    }

    // Si ya hay una renovación en marcha, esta petición espera su turno.
    if (isRefreshing) {
      return new Promise((resolve, reject) => {
        pendingQueue.push({
          resolve: (token: string) => {
            originalRequest.headers.Authorization = `Bearer ${token}`;
            resolve(apiClient(originalRequest));
          },
          reject,
        });
      });
    }

    originalRequest._retry = true;
    isRefreshing = true;

    try {
      const newToken = await requestNewAccessToken();
      flushQueue(null, newToken);

      originalRequest.headers.Authorization = `Bearer ${newToken}`;
      return apiClient(originalRequest);
    } catch (refreshError) {
      // El token de refresco tampoco sirve: la sesión terminó de verdad.
      flushQueue(refreshError, null);
      tokenStorage.clear();
      onSessionExpired?.();
      return Promise.reject(refreshError);
    } finally {
      isRefreshing = false;
    }
  },
);

/**
 * Traduce cualquier error de red o de la API a un mensaje legible en español.
 *
 * @param error error capturado en un bloque `catch`
 * @returns texto listo para mostrar en pantalla
 */
export function getErrorMessage(error: unknown): string {
  if (axios.isAxiosError<ApiError>(error)) {
    if (error.code === 'ECONNABORTED') {
      return 'El servidor tardó demasiado en responder. Inténtalo de nuevo.';
    }
    if (!error.response) {
      return 'No se pudo conectar con el servidor. Revisa tu conexión.';
    }

    const data = error.response.data;

    // Si hay errores de validación campo a campo, se muestran todos juntos.
    if (data?.details?.length) {
      return data.details.map((detail) => detail.message).join('. ');
    }
    if (data?.message) {
      return data.message;
    }
  }

  if (error instanceof Error) {
    return error.message;
  }

  return 'Ha ocurrido un error inesperado.';
}
