import { useEffect, useState } from 'react';
import { menuApi } from '../api/authApi';
import type { MenuItem } from '../types/auth';
import { useAuth } from './useAuth';

/**
 * Carga el menú de navegación del usuario autenticado.
 *
 * El menú viene del backend ya filtrado por permisos, así que aquí no se decide nada:
 * solo se pide y se entrega. Eso es lo que permite añadir una sección desde el panel y
 * verla aparecer sin recompilar el frontend.
 *
 * Se recarga cuando cambia el usuario, porque un cambio de roles cambia lo que debe ver.
 *
 * @returns las entradas del menú y si todavía se están cargando
 */
export function useMenu(): { items: MenuItem[]; isLoading: boolean } {
  const { user } = useAuth();
  const [items, setItems] = useState<MenuItem[]>([]);
  const [isLoading, setIsLoading] = useState(true);

  useEffect(() => {
    if (!user) {
      setItems([]);
      setIsLoading(false);
      return;
    }

    let cancelled = false;
    setIsLoading(true);

    menuApi
      .myMenu()
      .then((result) => {
        if (!cancelled) {
          setItems(result);
        }
      })
      .catch(() => {
        // Si el menú no se puede cargar, se deja vacío en lugar de romper la pantalla:
        // el contenido de la ruta actual sigue siendo accesible y navegable por URL.
        if (!cancelled) {
          setItems([]);
        }
      })
      .finally(() => {
        if (!cancelled) {
          setIsLoading(false);
        }
      });

    return () => {
      cancelled = true;
    };
  }, [user]);

  return { items, isLoading };
}
