import { useCallback, useEffect, useState } from 'react';
import { KeyRound, Loader2, Lock, Pencil, Plus, ShieldCheck, Trash2 } from 'lucide-react';
import { adminApi } from '../api/authApi';
import { getErrorMessage } from '../api/client';
import { Button, IconButton } from '../components/ui/Button';
import { Notice } from '../components/ui/Notice';
import { PageHeader } from '../components/ui/PageHeader';
import { PermissionGate } from '../components/PermissionGate';
import { PermissionDialog } from '../components/roles/PermissionDialog';
import { RoleDialog } from '../components/roles/RoleDialog';
import { useAuth } from '../hooks/useAuth';
import { PERMISSIONS, ROLES, type Permission, type Role } from '../types/auth';
import { roleLabel } from '../utils/labels';

/** Roles que trae el sistema y que el backend no deja borrar. */
const SYSTEM_ROLES: string[] = [ROLES.ADMIN, ROLES.MANAGER, ROLES.USER];

/**
 * Pantalla de gestión de roles y de permisos de interfaz.
 *
 * Solo se listan los permisos de interfaz (los que se crean desde aquí). Los que verifica
 * el backend están declarados en el código y no son gestionables, así que mostrarlos en
 * esta pantalla solo añadiría ruido; siguen apareciendo al editar un rol, que es donde se
 * eligen.
 */
export function RolesPage() {
  const { hasPermission } = useAuth();

  const [roles, setRoles] = useState<Role[]>([]);
  const [permissions, setPermissions] = useState<Permission[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);
  const [busyId, setBusyId] = useState<string | null>(null);

  const [isRoleDialogOpen, setIsRoleDialogOpen] = useState(false);
  const [roleBeingEdited, setRoleBeingEdited] = useState<Role | null>(null);
  const [isPermissionDialogOpen, setIsPermissionDialogOpen] = useState(false);

  const canReadPermissions = hasPermission(PERMISSIONS.PERMISSION_READ);

  /**
   * Carga los roles y, si se pueden ver, el catálogo de permisos.
   */
  const loadData = useCallback(async () => {
    setIsLoading(true);
    setErrorMessage(null);

    try {
      const [loadedRoles, loadedPermissions] = await Promise.all([
        adminApi.listRoles(),
        canReadPermissions ? adminApi.listPermissions() : Promise.resolve<Permission[]>([]),
      ]);
      setRoles(loadedRoles);
      setPermissions(loadedPermissions);
    } catch (error) {
      setErrorMessage(getErrorMessage(error));
    } finally {
      setIsLoading(false);
    }
  }, [canReadPermissions]);

  useEffect(() => {
    void loadData();
  }, [loadData]);

  /**
   * Ejecuta un borrado tras confirmarlo, mostrando su estado de carga.
   *
   * @param key      identificador de la fila ocupada
   * @param question pregunta de confirmación
   * @param action   llamada al backend
   */
  async function confirmAndDelete(key: string, question: string, action: () => Promise<void>) {
    if (!window.confirm(question)) {
      return;
    }

    setBusyId(key);
    setErrorMessage(null);

    try {
      await action();
      await loadData();
    } catch (error) {
      setErrorMessage(getErrorMessage(error));
    } finally {
      setBusyId(null);
    }
  }

  const uiPermissions = permissions.filter((permission) => permission.uiOnly);

  if (isLoading) {
    return (
      <div className="flex justify-center py-24">
        <Loader2 className="size-5 animate-spin text-slate-400" aria-label="Cargando" />
      </div>
    );
  }

  return (
    <div className="animate-rise space-y-10">
      <section>
        <PageHeader
          title="Roles"
          actions={
            <PermissionGate permissions={[PERMISSIONS.ROLE_WRITE]}>
              <Button
                icon={<Plus className="size-4" />}
                onClick={() => {
                  setRoleBeingEdited(null);
                  setIsRoleDialogOpen(true);
                }}
              >
                Nuevo rol
              </Button>
            </PermissionGate>
          }
        />

        {errorMessage && (
          <div className="mb-4">
            <Notice>{errorMessage}</Notice>
          </div>
        )}

        <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
          {roles.map((role) => {
            const isSystem = SYSTEM_ROLES.includes(role.name);

            return (
              <article key={role.id} className="card flex flex-col p-5">
                <div className="flex items-start justify-between gap-3">
                  <div className="flex size-10 items-center justify-center rounded-lg bg-indigo-50 text-indigo-600">
                    <ShieldCheck className="size-5" aria-hidden />
                  </div>

                  <PermissionGate permissions={[PERMISSIONS.ROLE_WRITE]}>
                    <div className="-mr-1 -mt-1 flex">
                      <IconButton
                        label="Editar"
                        icon={<Pencil className="size-4" />}
                        onClick={() => {
                          setRoleBeingEdited(role);
                          setIsRoleDialogOpen(true);
                        }}
                      />
                      {isSystem ? (
                        <IconButton
                          label="Rol del sistema: no se puede eliminar"
                          icon={<Lock className="size-4" />}
                          disabled
                        />
                      ) : (
                        <IconButton
                          label="Eliminar"
                          tone="danger"
                          icon={
                            busyId === `role-${role.id}` ? (
                              <Loader2 className="size-4 animate-spin" />
                            ) : (
                              <Trash2 className="size-4" />
                            )
                          }
                          onClick={() =>
                            confirmAndDelete(
                              `role-${role.id}`,
                              `¿Eliminar el rol ${roleLabel(role.name)}?`,
                              () => adminApi.deleteRole(role.id),
                            )
                          }
                        />
                      )}
                    </div>
                  </PermissionGate>
                </div>

                <h3 className="mt-4 font-semibold text-slate-900">{roleLabel(role.name)}</h3>
                <p className="mt-1 line-clamp-2 flex-1 text-sm text-slate-500">{role.description}</p>
                <p className="mt-4 text-xs font-medium text-slate-400">
                  {role.permissions.length} {role.permissions.length === 1 ? 'permiso' : 'permisos'}
                </p>
              </article>
            );
          })}
        </div>
      </section>

      {canReadPermissions && (
        <section>
          <div className="mb-4 flex items-center justify-between gap-4">
            <h2 className="text-lg font-semibold text-slate-900">Permisos de interfaz</h2>
            <PermissionGate permissions={[PERMISSIONS.PERMISSION_WRITE]}>
              <Button
                variant="secondary"
                icon={<Plus className="size-4" />}
                onClick={() => setIsPermissionDialogOpen(true)}
              >
                Nuevo permiso
              </Button>
            </PermissionGate>
          </div>

          <div className="card">
            {uiPermissions.length === 0 ? (
              <div className="flex flex-col items-center px-6 py-10 text-center">
                <div className="flex size-10 items-center justify-center rounded-full bg-slate-100 text-slate-400">
                  <KeyRound className="size-5" aria-hidden />
                </div>
                <p className="mt-3 text-sm text-slate-500">Aún no hay permisos de interfaz.</p>
              </div>
            ) : (
              <ul className="divide-y divide-slate-100">
                {uiPermissions.map((permission) => (
                  <li key={permission.id} className="flex items-center gap-4 px-5 py-3.5">
                    <KeyRound className="size-4 shrink-0 text-slate-400" aria-hidden />
                    <div className="min-w-0 flex-1">
                      <p className="font-mono text-sm text-slate-900">{permission.name}</p>
                      <p className="truncate text-xs text-slate-500">{permission.description}</p>
                    </div>
                    <PermissionGate permissions={[PERMISSIONS.PERMISSION_WRITE]}>
                      <IconButton
                        label="Eliminar"
                        tone="danger"
                        icon={
                          busyId === `perm-${permission.id}` ? (
                            <Loader2 className="size-4 animate-spin" />
                          ) : (
                            <Trash2 className="size-4" />
                          )
                        }
                        onClick={() =>
                          confirmAndDelete(
                            `perm-${permission.id}`,
                            `¿Eliminar el permiso ${permission.name}?`,
                            () => adminApi.deletePermission(permission.id),
                          )
                        }
                      />
                    </PermissionGate>
                  </li>
                ))}
              </ul>
            )}
          </div>
        </section>
      )}

      <RoleDialog
        isOpen={isRoleDialogOpen}
        role={roleBeingEdited}
        permissions={permissions}
        onClose={() => setIsRoleDialogOpen(false)}
        onSaved={() => {
          setIsRoleDialogOpen(false);
          void loadData();
        }}
      />

      <PermissionDialog
        isOpen={isPermissionDialogOpen}
        onClose={() => setIsPermissionDialogOpen(false)}
        onCreated={() => {
          setIsPermissionDialogOpen(false);
          void loadData();
        }}
      />
    </div>
  );
}
