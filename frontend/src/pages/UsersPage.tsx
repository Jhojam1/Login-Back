import { useCallback, useEffect, useState } from 'react';
import { ChevronLeft, ChevronRight, Loader2, Plus, ShieldCheck, Trash2, UserCheck, UserX } from 'lucide-react';
import { adminApi } from '../api/authApi';
import { getErrorMessage } from '../api/client';
import { Badge } from '../components/ui/Badge';
import { Button, IconButton } from '../components/ui/Button';
import { Notice } from '../components/ui/Notice';
import { PageHeader } from '../components/ui/PageHeader';
import { PermissionGate } from '../components/PermissionGate';
import { CreateUserDialog } from '../components/users/CreateUserDialog';
import { EditRolesDialog } from '../components/users/EditRolesDialog';
import { useAuth } from '../hooks/useAuth';
import { PERMISSIONS, ROLES, type Page, type Role, type User } from '../types/auth';
import { roleLabel } from '../utils/labels';

/** Usuarios por página. */
const PAGE_SIZE = 10;

/**
 * Iniciales para el avatar de una fila.
 *
 * @param user usuario de la fila
 * @returns una o dos letras en mayúscula
 */
function initialsOf(user: User): string {
  const first = user.firstName?.[0] ?? user.username[0];
  const last = user.lastName?.[0] ?? '';
  return (first + last).toUpperCase();
}

/**
 * Etiquetas con los roles de un usuario.
 *
 * @param props nombres internos de los roles
 */
function RoleBadges({ roles }: { roles: string[] }) {
  return (
    <>
      {roles.map((role) => (
        <Badge key={role} tone={role === ROLES.ADMIN ? 'indigo' : 'slate'}>
          {roleLabel(role)}
        </Badge>
      ))}
    </>
  );
}

/**
 * Etiqueta con el estado de la cuenta.
 *
 * @param props usuario cuyo estado se muestra
 */
function StatusBadge({ user }: { user: User }) {
  if (user.locked) {
    return <Badge tone="amber">Bloqueada</Badge>;
  }
  return user.enabled ? <Badge tone="emerald">Activa</Badge> : <Badge tone="rose">Inactiva</Badge>;
}

/**
 * Pantalla de administración de usuarios.
 *
 * Las acciones de cada fila aparecen según los permisos de quien mira. Es comodidad de
 * interfaz: quien decide de verdad es el backend.
 */
export function UsersPage() {
  const { user: currentUser, hasPermission } = useAuth();

  const [page, setPage] = useState<Page<User> | null>(null);
  const [pageNumber, setPageNumber] = useState(0);
  const [roles, setRoles] = useState<Role[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);
  const [busyUserId, setBusyUserId] = useState<number | null>(null);

  const [isCreateOpen, setIsCreateOpen] = useState(false);
  const [userBeingEdited, setUserBeingEdited] = useState<User | null>(null);

  /**
   * Pide al backend la página actual de usuarios.
   */
  const loadUsers = useCallback(async () => {
    setIsLoading(true);
    setErrorMessage(null);

    try {
      setPage(await adminApi.listUsers(pageNumber, PAGE_SIZE));
    } catch (error) {
      setErrorMessage(getErrorMessage(error));
    } finally {
      setIsLoading(false);
    }
  }, [pageNumber]);

  useEffect(() => {
    void loadUsers();
  }, [loadUsers]);

  /**
   * Carga los roles disponibles para los diálogos. Solo si se pueden leer: si no, la
   * persona nunca abrirá esos diálogos y la llamada daría un 403.
   */
  useEffect(() => {
    if (!hasPermission(PERMISSIONS.ROLE_READ)) {
      return;
    }

    let cancelled = false;
    adminApi
      .listRoles()
      .then((result) => !cancelled && setRoles(result))
      .catch(() => undefined);

    return () => {
      cancelled = true;
    };
  }, [hasPermission]);

  /**
   * Ejecuta una acción sobre una fila mostrando su estado de carga y recargando después.
   *
   * @param userId fila afectada
   * @param action llamada al backend
   */
  async function runRowAction(userId: number, action: () => Promise<unknown>) {
    setBusyUserId(userId);
    setErrorMessage(null);

    try {
      await action();
      await loadUsers();
    } catch (error) {
      setErrorMessage(getErrorMessage(error));
    } finally {
      setBusyUserId(null);
    }
  }

  /**
   * Elimina un usuario tras confirmarlo.
   *
   * @param target usuario a eliminar
   */
  function handleDelete(target: User) {
    if (window.confirm(`¿Eliminar a ${target.username}? No se puede deshacer.`)) {
      void runRowAction(target.id, () => adminApi.deleteUser(target.id));
    }
  }

  return (
    <div className="animate-rise">
      <PageHeader
        title="Usuarios"
        subtitle={page ? `${page.totalElements} cuentas` : undefined}
        actions={
          <PermissionGate permissions={[PERMISSIONS.USER_CREATE]}>
            <Button icon={<Plus className="size-4" />} onClick={() => setIsCreateOpen(true)}>
              Nuevo usuario
            </Button>
          </PermissionGate>
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
          <div className="overflow-x-auto">
            <table className="w-full text-left text-sm">
              <thead className="border-b border-slate-200 bg-slate-50/60 text-xs font-medium uppercase tracking-wide text-slate-500">
                <tr>
                  <th scope="col" className="px-4 py-3 sm:px-5">Usuario</th>
                  <th scope="col" className="hidden px-5 py-3 md:table-cell">Roles</th>
                  <th scope="col" className="hidden px-5 py-3 sm:table-cell">Estado</th>
                  <th scope="col" className="w-px px-4 py-3 sm:px-5"><span className="sr-only">Acciones</span></th>
                </tr>
              </thead>

              <tbody className="divide-y divide-slate-100">
                {page?.content.map((item) => {
                  const isSelf = item.id === currentUser?.id;
                  const isBusy = busyUserId === item.id;

                  return (
                    <tr key={item.id} className="transition-colors hover:bg-slate-50/60">
                      <td className="max-w-0 px-4 py-3.5 sm:px-5">
                        <div className="flex items-center gap-3">
                          <div className="flex size-9 shrink-0 items-center justify-center rounded-full bg-indigo-100 text-xs font-semibold text-indigo-700">
                            {initialsOf(item)}
                          </div>
                          <div className="min-w-0">
                            <p className="truncate font-medium text-slate-900">
                              {item.fullName}
                              {isSelf && <span className="ml-1.5 font-normal text-slate-400">(tú)</span>}
                            </p>
                            <p className="truncate text-xs text-slate-500">{item.email}</p>

                            {/* En pantallas estrechas las columnas de roles y estado se
                                ocultan, así que su contenido se muestra aquí debajo */}
                            <div className="mt-1.5 flex flex-wrap gap-1 md:hidden">
                              <RoleBadges roles={item.roles} />
                              <span className="sm:hidden">
                                <StatusBadge user={item} />
                              </span>
                            </div>
                          </div>
                        </div>
                      </td>

                      <td className="hidden px-5 py-3.5 md:table-cell">
                        <div className="flex flex-wrap gap-1.5">
                          <RoleBadges roles={item.roles} />
                        </div>
                      </td>

                      <td className="hidden px-5 py-3.5 sm:table-cell">
                        <StatusBadge user={item} />
                      </td>

                      <td className="w-px px-4 py-3.5 sm:px-5">
                        <div className="flex items-center justify-end gap-0.5">
                          {isBusy ? (
                            <Loader2 className="mx-2 size-4 animate-spin text-slate-400" />
                          ) : (
                            <>
                              <PermissionGate permissions={[PERMISSIONS.ROLE_ASSIGN]}>
                                <IconButton
                                  label="Cambiar roles"
                                  icon={<ShieldCheck className="size-4" />}
                                  onClick={() => setUserBeingEdited(item)}
                                />
                              </PermissionGate>

                              <PermissionGate permissions={[PERMISSIONS.USER_UPDATE]}>
                                <IconButton
                                  label={item.enabled ? 'Desactivar' : 'Activar'}
                                  icon={
                                    item.enabled ? <UserX className="size-4" /> : <UserCheck className="size-4" />
                                  }
                                  disabled={isSelf}
                                  onClick={() =>
                                    runRowAction(item.id, () =>
                                      adminApi.setUserEnabled(item.id, !item.enabled),
                                    )
                                  }
                                />
                              </PermissionGate>

                              <PermissionGate permissions={[PERMISSIONS.USER_DELETE]}>
                                <IconButton
                                  label="Eliminar"
                                  tone="danger"
                                  icon={<Trash2 className="size-4" />}
                                  disabled={isSelf}
                                  onClick={() => handleDelete(item)}
                                />
                              </PermissionGate>
                            </>
                          )}
                        </div>
                      </td>
                    </tr>
                  );
                })}
              </tbody>
            </table>
          </div>
        )}

        {page && page.totalPages > 1 && (
          <div className="flex items-center justify-between border-t border-slate-200 px-5 py-3">
            <p className="text-xs text-slate-500">
              Página {page.number + 1} de {page.totalPages}
            </p>
            <div className="flex gap-1">
              <IconButton
                label="Página anterior"
                icon={<ChevronLeft className="size-4" />}
                disabled={page.first}
                onClick={() => setPageNumber((current) => current - 1)}
              />
              <IconButton
                label="Página siguiente"
                icon={<ChevronRight className="size-4" />}
                disabled={page.last}
                onClick={() => setPageNumber((current) => current + 1)}
              />
            </div>
          </div>
        )}
      </div>

      <CreateUserDialog
        isOpen={isCreateOpen}
        roles={roles}
        onClose={() => setIsCreateOpen(false)}
        onCreated={() => {
          setIsCreateOpen(false);
          void loadUsers();
        }}
      />

      <EditRolesDialog
        user={userBeingEdited}
        roles={roles}
        onClose={() => setUserBeingEdited(null)}
        onSaved={() => {
          setUserBeingEdited(null);
          void loadUsers();
        }}
      />
    </div>
  );
}
