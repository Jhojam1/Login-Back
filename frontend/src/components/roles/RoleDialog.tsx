import { useEffect, useState, type FormEvent } from 'react';
import { adminApi } from '../../api/authApi';
import { getErrorMessage } from '../../api/client';
import { Button } from '../ui/Button';
import { CheckOption } from '../ui/CheckOption';
import { Input } from '../ui/Input';
import { Modal, ModalFooter } from '../ui/Modal';
import { Notice } from '../ui/Notice';
import type { Permission, Role } from '../../types/auth';
import { groupPermissions, permissionLabel } from '../../utils/labels';

interface RoleDialogProps {
  /** Si el diálogo está visible. */
  isOpen: boolean;
  /** Rol a editar, o `null` para crear uno nuevo. */
  role: Role | null;
  /** Catálogo completo de permisos disponibles. */
  permissions: Permission[];
  /** Se ejecuta al cerrar sin guardar. */
  onClose: () => void;
  /** Se ejecuta cuando el rol se guardó correctamente. */
  onSaved: () => void;
}

/**
 * Formulario de crear y editar roles. El mismo diálogo sirve para los dos casos.
 *
 * Al editar, el nombre queda bloqueado: puede estar escrito en anotaciones del código y
 * en otros servicios, así que renombrarlo rompería esas comprobaciones sin avisar.
 *
 * @param props estado, rol a editar, catálogo de permisos y acciones de cierre
 */
export function RoleDialog({ isOpen, role, permissions, onClose, onSaved }: RoleDialogProps) {
  const [name, setName] = useState('');
  const [description, setDescription] = useState('');
  const [selected, setSelected] = useState<string[]>([]);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);
  const [isSubmitting, setIsSubmitting] = useState(false);

  const isEditing = role !== null;

  /** Precarga el formulario al abrirlo. */
  useEffect(() => {
    if (!isOpen) {
      return;
    }
    setName(role?.name.replace(/^ROLE_/, '') ?? '');
    setDescription(role?.description ?? '');
    setSelected(role?.permissions ?? []);
    setErrorMessage(null);
  }, [isOpen, role]);

  /**
   * Marca o desmarca un permiso.
   *
   * @param permissionName nombre del permiso pulsado
   */
  function togglePermission(permissionName: string) {
    setSelected((current) =>
      current.includes(permissionName)
        ? current.filter((item) => item !== permissionName)
        : [...current, permissionName],
    );
  }

  /**
   * Guarda el rol, creándolo o actualizándolo según el caso.
   *
   * @param event evento de envío del formulario
   */
  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();

    if (selected.length === 0) {
      setErrorMessage('Elige al menos un permiso.');
      return;
    }

    setErrorMessage(null);
    setIsSubmitting(true);

    const payload = {
      name: isEditing ? role.name : name.trim(),
      description: description.trim(),
      permissions: selected,
    };

    try {
      if (isEditing) {
        await adminApi.updateRole(role.id, payload);
      } else {
        await adminApi.createRole(payload);
      }
      onSaved();
    } catch (error) {
      setErrorMessage(getErrorMessage(error));
    } finally {
      setIsSubmitting(false);
    }
  }

  const groups = groupPermissions(permissions.map((permission) => permission.name));

  return (
    <Modal isOpen={isOpen} title={isEditing ? 'Editar rol' : 'Nuevo rol'} onClose={onClose}>
      <form onSubmit={handleSubmit} className="space-y-4" noValidate>
        {errorMessage && <Notice>{errorMessage}</Notice>}

        <Input
          label="Nombre"
          required
          disabled={isEditing}
          placeholder="AUDITOR"
          autoComplete="off"
          value={name}
          onChange={(event) => setName(event.target.value.toUpperCase())}
        />

        <Input
          label="Descripción"
          autoComplete="off"
          value={description}
          onChange={(event) => setDescription(event.target.value)}
        />

        <fieldset className="space-y-4">
          <legend className="text-sm font-medium text-slate-700">
            Permisos <span className="font-normal text-slate-400">· {selected.length}</span>
          </legend>

          {groups.map(([groupName, names]) => (
            <div key={groupName}>
              <p className="mb-2 text-xs font-medium uppercase tracking-wide text-slate-400">
                {groupName}
              </p>
              <div className="grid gap-2 sm:grid-cols-2">
                {names.map((permissionName) => (
                  <CheckOption
                    key={permissionName}
                    label={permissionLabel(permissionName)}
                    checked={selected.includes(permissionName)}
                    onToggle={() => togglePermission(permissionName)}
                  />
                ))}
              </div>
            </div>
          ))}
        </fieldset>

        <ModalFooter>
          <Button type="button" variant="secondary" onClick={onClose}>
            Cancelar
          </Button>
          <Button type="submit" isLoading={isSubmitting}>
            Guardar
          </Button>
        </ModalFooter>
      </form>
    </Modal>
  );
}
