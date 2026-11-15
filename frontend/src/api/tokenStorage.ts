/**
 * Único punto del frontend que toca el almacenamiento de los tokens.
 *
 * Concentrarlo aquí significa que si mañana se decide pasar de `localStorage` a
 * cookies `httpOnly` (más seguro frente a XSS, pero exige cambios en el backend),
 * solo hay que reescribir este archivo.
 *
 * Nota de seguridad: `localStorage` es accesible desde JavaScript, así que un XSS
 * podría leer el token. Se mitiga con tokens de acceso de vida corta (15 minutos) y
 * con la posibilidad de revocar el token de refresco desde el servidor.
 */

const ACCESS_TOKEN_KEY = 'auth.accessToken';
const REFRESH_TOKEN_KEY = 'auth.refreshToken';

export const tokenStorage = {
  /**
   * Devuelve el token de acceso guardado.
   *
   * @returns el JWT, o `null` si no hay sesión iniciada
   */
  getAccessToken(): string | null {
    return localStorage.getItem(ACCESS_TOKEN_KEY);
  },

  /**
   * Devuelve el token de refresco guardado.
   *
   * @returns el token opaco, o `null` si no hay sesión iniciada
   */
  getRefreshToken(): string | null {
    return localStorage.getItem(REFRESH_TOKEN_KEY);
  },

  /**
   * Guarda el par de tokens tras un login o un refresco correctos.
   *
   * @param accessToken  JWT de acceso
   * @param refreshToken token de refresco
   */
  save(accessToken: string, refreshToken: string): void {
    localStorage.setItem(ACCESS_TOKEN_KEY, accessToken);
    localStorage.setItem(REFRESH_TOKEN_KEY, refreshToken);
  },

  /**
   * Borra los tokens al cerrar sesión o cuando el refresco falla.
   */
  clear(): void {
    localStorage.removeItem(ACCESS_TOKEN_KEY);
    localStorage.removeItem(REFRESH_TOKEN_KEY);
  },

  /**
   * Indica si hay una sesión guardada en el navegador.
   *
   * @returns `true` si existe un token de acceso
   */
  hasSession(): boolean {
    return Boolean(localStorage.getItem(ACCESS_TOKEN_KEY));
  },
};
