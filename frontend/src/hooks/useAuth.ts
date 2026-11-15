import { useContext } from 'react';
import { AuthContext, type AuthContextValue } from '../context/AuthContext';

/**
 * Acceso al contexto de autenticación desde cualquier componente.
 *
 * Lanza un error explícito si se usa fuera del proveedor, en lugar de devolver
 * `undefined` y provocar un fallo confuso tres componentes más abajo.
 *
 * @returns el estado y las acciones de la sesión
 */
export function useAuth(): AuthContextValue {
  const context = useContext(AuthContext);

  if (context === undefined) {
    throw new Error('useAuth debe usarse dentro de un <AuthProvider>');
  }

  return context;
}
