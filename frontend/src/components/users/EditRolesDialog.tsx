import { useEffect, useState, type FormEvent } from 'react';
import { adminApi } from '../../api/authApi';
import { getErrorMessage } from '../../api/client';
import { Button } from '../ui/Button';
import { CheckOption } from '../ui/CheckOption';
import { Modal, ModalFooter } from '../ui/Modal';
import { Notice } from '../ui/Notice';
import type { Role, User } from '../../types/auth';
import { roleLabel } from '../../utils/labels';

interface EditRolesDialogProps {
  /** Usuario cuyos roles se editan, o `null` si el diálogo está cerrado. */
  user: User | null;
  /** Roles disponibles. */
  roles: Role[];
  /** Se ejecuta al cerrar sin guardar. */
  onClose: () => void;
  /** Se ejecuta cuando los roles se guardaron correctamente. */
  onSaved: () => void;
}

/**
 * Diálogo para cambiar los roles de un usuario.
 *
 * Al guardar, el backend revoca las sesiones de esa persona para que el cambio de
 * permisos surta efecto de inmediato en lugar de esperar a que caduque su token.
 *
 * @param props usuario, roles disponibles y acciones de cierre
 */
export function EditRolesDialog({ user, roles, onClose, onSaved }: EditRolesDialogProps) {
  const [selectedRoles, setSelectedRoles] = useState<string[]>([]);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);
  const [isSubmitting, setIsSubmitting] = useState(false);

  /** Al abrir el diálogo, precarga los roles que ya tiene esa persona. */
  useEffect(() => {
    if (user) {
      setSelectedRoles(user.roles);
      setErrorMessage(null);
    }
  }, [user]);

  /**
   * Marca o desmarca un rol.
   *
   * @param roleName nombre del rol pulsado
   */
  function toggleRole(roleName: string) {
    setSelectedRoles((current) =>
      current.includes(roleName) ? current.filter((name) => name !== roleName) : [...current, roleName],
    );
  }

  /**
   * Guarda los roles seleccionados.
   *
   * @param event evento de envío del formulario
   */
  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();

    if (!user) {
      return;
    }
    if (selectedRoles.length === 0) {
      setErrorMessage('Selecciona al menos un rol.');
      return;
    }

    setErrorMessage(null);
    setIsSubmitting(true);

    try {
      await adminApi.assignRoles(user.id, selectedRoles);
      onSaved();
    } catch (error) {
      setErrorMessage(getErrorMessage(error));
    } finally {
      setIsSubmitting(false);
    }
  }

  return (
    <Modal
      isOpen={user !== null}
      title="Roles"
      description={user?.fullName}
      onClose={onClose}
    >
      <form onSubmit={handleSubmit} className="space-y-4">
        {errorMessage && <Notice>{errorMessage}</Notice>}

        <div className="grid gap-2">
          {roles.map((role) => (
            <CheckOption
              key={role.id}
              label={roleLabel(role.name)}
              hint={role.description}
              checked={selectedRoles.includes(role.name)}
              onToggle={() => toggleRole(role.name)}
            />
          ))}
        </div>

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
