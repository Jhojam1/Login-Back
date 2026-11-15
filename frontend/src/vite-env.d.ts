/// <reference types="vite/client" />

/**
 * Tipado de las variables de entorno que usa el frontend, para que TypeScript
 * avise si se escribe mal el nombre de una.
 */
interface ImportMetaEnv {
  /** URL base del backend; vacía en desarrollo porque Vite hace de proxy. */
  readonly VITE_API_URL: string;
}

interface ImportMeta {
  readonly env: ImportMetaEnv;
}
