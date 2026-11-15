import { useCallback, useEffect, useState } from 'react';
import { Eye, EyeOff, Loader2, Lock, Pencil, Plus, Trash2 } from 'lucide-react';
import clsx from 'clsx';
import { adminApi, menuApi } from '../api/authApi';
import { getErrorMessage } from '../api/client';
import { Badge } from '../components/ui/Badge';
import { Button, IconButton } from '../components/ui/Button';
import { Notice } from '../components/ui/Notice';
import { PageHeader } from '../components/ui/PageHeader';
import { MenuItemDialog } from '../components/menu/MenuItemDialog';
import type { MenuItem, Permission } from '../types/auth';
import { menuIcon } from '../utils/icons';
import { permissionLabel } from '../utils/labels';

/**
 * Pantalla de gestión del menú lateral: añadir, editar, ocultar y eliminar entradas.
 *
 * Lo que se guarda aquí cambia lo que ve cada persona en su siguiente carga, sin
 * recompilar ni desplegar nada.
 */
export function MenuPage() {
  const [items, setItems] = useState<MenuItem[]>([]);
  const [permissions, setPermissions] = useState<Permission[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);
  const [busyItemId, setBusyItemId] = useState<number | null>(null);

  const [isDialogOpen, setIsDialogOpen] = useState(false);
  const [itemBeingEdited, setItemBeingEdited] = useState<MenuItem | null>(null);

  /**
   * Carga las entradas del menú y el catálogo de permisos para el desplegable.
   */
  const loadData = useCallback(async () => {
    setIsLoading(true);
    setErrorMessage(null);

    try {
      const [loadedItems, loadedPermissions] = await Promise.all([
        menuApi.listAll(),
        adminApi.listPermissions(),
      ]);
      setItems(loadedItems);
      setPermissions(loadedPermissions);
    } catch (error) {
      setErrorMessage(getErrorMessage(error));
    } finally {
      setIsLoading(false);
    }
  }, []);

  useEffect(() => {
    void loadData();
  }, [loadData]);

  /**
   * Ejecuta una acción sobre una fila mostrando su estado de carga y recargando después.
   *
   * @param itemId fila afectada
   * @param action llamada al backend
   */
  async function runRowAction(itemId: number, action: () => Promise<unknown>) {
    setBusyItemId(itemId);
    setErrorMessage(null);

    try {
      await action();
      await loadData();
    } catch (error) {
      setErrorMessage(getErrorMessage(error));
    } finally {
      setBusyItemId(null);
    }
  }

  /**
   * Muestra u oculta una entrada sin borrarla.
   *
   * @param item entrada sobre la que se actúa
   */
  function toggleVisible(item: MenuItem) {
    void runRowAction(item.id, () =>
      menuApi.update(item.id, {
        label: item.label,
        path: item.path,
        icon: item.icon ?? '',
        requiredPermission: item.requiredPermission ?? '',
        displayOrder: item.displayOrder,
        visible: !item.visible,
      }),
    );
  }

  /**
   * Elimina una entrada tras confirmarlo.
   *
   * @param item entrada a eliminar
   */
  function handleDelete(item: MenuItem) {
    if (window.confirm(`¿Eliminar "${item.label}" del menú?`)) {
      void runRowAction(item.id, () => menuApi.remove(item.id));
    }
  }

  return (
    <div className="animate-rise">
      <PageHeader
        title="Menú"
        actions={
          <Button
            icon={<Plus className="size-4" />}
            onClick={() => {
              setItemBeingEdited(null);
              setIsDialogOpen(true);
            }}
          >
            Nueva entrada
          </Button>
        }
      />

      {errorMessage && (
        <div className="mb-4">
          <Notice>{errorMessage}</Notice>
        </div>
      )}

      <div className="card overflow-hidden">
        {isLoading ? (
          <div className="flex justify-center py-16">
            <Loader2 className="size-5 animate-spin text-slate-400" aria-label="Cargando" />
          </div>
        ) : (
          <ul className="divide-y divide-slate-100">
            {items.map((item) => {
              const Icon = menuIcon(item.icon);
              const isBusy = busyItemId === item.id;

              return (
                <li
                  key={item.id}
                  className={clsx(
                    'flex items-center gap-4 px-5 py-3.5 transition-colors hover:bg-slate-50/60',
                    !item.visible && 'opacity-50',
                  )}
                >
                  <div className="flex size-9 shrink-0 items-center justify-center rounded-lg bg-slate-100 text-slate-600">
                    <Icon className="size-4.5" aria-hidden />
                  </div>

                  <div className="min-w-0 flex-1">
                    <p className="truncate text-sm font-medium text-slate-900">{item.label}</p>
                    <p className="truncate font-mono text-xs text-slate-400">{item.path}</p>
                  </div>

                  <div className="hidden sm:block">
                    {item.requiredPermission ? (
                      <Badge tone="indigo">{permissionLabel(item.requiredPermission)}</Badge>
                    ) : (
                      <Badge>Todos</Badge>
                    )}
                  </div>

                  <div className="flex shrink-0 items-center gap-0.5">
                    {isBusy ? (
                      <Loader2 className="mx-2 size-4 animate-spin text-slate-400" />
                    ) : (
                      <>
                        <IconButton
                          label="Editar"
                          icon={<Pencil className="size-4" />}
                          onClick={() => {
                            setItemBeingEdited(item);
                            setIsDialogOpen(true);
                          }}
                        />
                        <IconButton
                          label={item.visible ? 'Ocultar' : 'Mostrar'}
                          icon={item.visible ? <Eye className="size-4" /> : <EyeOff className="size-4" />}
                          onClick={() => toggleVisible(item)}
                        />
                        {item.systemItem ? (
                          <IconButton
                            label="Entrada del sistema: se puede ocultar, no eliminar"
                            icon={<Lock className="size-4" />}
                            disabled
                          />
                        ) : (
                          <IconButton
                            label="Eliminar"
                            tone="danger"
                            icon={<Trash2 className="size-4" />}
                            onClick={() => handleDelete(item)}
                          />
                        )}
                      </>
                    )}
                  </div>
                </li>
              );
            })}
          </ul>
        )}
      </div>

      <MenuItemDialog
        isOpen={isDialogOpen}
        item={itemBeingEdited}
        permissions={permissions}
        nextOrder={items.length + 1}
        onClose={() => setIsDialogOpen(false)}
        onSaved={() => {
          setIsDialogOpen(false);
          void loadData();
        }}
      />
    </div>
  );
}
