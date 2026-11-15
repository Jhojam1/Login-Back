import { useState, type ChangeEvent, type FormEvent } from 'react';
import { adminApi } from '../../api/authApi';
import { getErrorMessage } from '../../api/client';
import { Button } from '../ui/Button';
import { CheckOption } from '../ui/CheckOption';
import { Input } from '../ui/Input';
import { Modal, ModalFooter } from '../ui/Modal';
import { Notice } from '../ui/Notice';
import { ROLES, type Role } from '../../types/auth';
import { roleLabel } from '../../utils/labels';

interface CreateUserDialogProps {
  /** Si el diálogo está visible. */
  isOpen: boolean;
  /** Roles disponibles para asignar. */
  roles: Role[];
  /** Se ejecuta al cerrar sin guardar. */
  onClose: () => void;
  /** Se ejecuta cuando la cuenta se creó correctamente. */
  onCreated: () => void;
}

/** Estado inicial del formulario. */
const EMPTY_FORM = {
  firstName: '',
  lastName: '',
  username: '',
  email: '',
  password: '',
};

/**
 * Diálogo para crear una cuenta desde el panel, eligiendo sus roles.
 *
 * El backend lo permite solo con `USER_CREATE` y usa un DTO distinto al del registro
 * público, para que nadie pueda asignarse `ROLE_ADMIN` desde el formulario abierto.
 *
 * @param props estado, roles disponibles y acciones de cierre
 */
export function CreateUserDialog({ isOpen, roles, onClose, onCreated }: CreateUserDialogProps) {
  const [form, setForm] = useState(EMPTY_FORM);
  const [selectedRoles, setSelectedRoles] = useState<string[]>([ROLES.USER]);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);
  const [isSubmitting, setIsSubmitting] = useState(false);

  /**
   * Devuelve el manejador de cambio de un campo de texto.
   *
   * @param field nombre del campo
   * @returns manejador del evento `change`
   */
  function handleChange(field: keyof typeof EMPTY_FORM) {
    return (event: ChangeEvent<HTMLInputElement>) => {
      setForm((current) => ({ ...current, [field]: event.target.value }));
    };
  }

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
   * Deja el formulario como estaba y cierra el diálogo.
   */
  function handleClose() {
    setForm(EMPTY_FORM);
    setSelectedRoles([ROLES.USER]);
    setErrorMessage(null);
    onClose();
  }

  /**
   * Envía la cuenta nueva al backend.
   *
   * @param event evento de envío del formulario
   */
  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setErrorMessage(null);

    if (selectedRoles.length === 0) {
      setErrorMessage('Selecciona al menos un rol.');
      return;
    }

    setIsSubmitting(true);

    try {
      await adminApi.createUser({
        username: form.username.trim(),
        email: form.email.trim(),
        password: form.password,
        firstName: form.firstName.trim() || undefined,
        lastName: form.lastName.trim() || undefined,
        roles: selectedRoles,
      });

      setForm(EMPTY_FORM);
      setSelectedRoles([ROLES.USER]);
      onCreated();
    } catch (error) {
      setErrorMessage(getErrorMessage(error));
    } finally {
      setIsSubmitting(false);
    }
  }

  return (
    <Modal isOpen={isOpen} title="Nuevo usuario" onClose={handleClose}>
      <form onSubmit={handleSubmit} className="space-y-4" noValidate>
        {errorMessage && <Notice>{errorMessage}</Notice>}

        <div className="grid grid-cols-2 gap-3">
          <Input label="Nombre" value={form.firstName} onChange={handleChange('firstName')} />
          <Input label="Apellido" value={form.lastName} onChange={handleChange('lastName')} />
        </div>

        <Input
          label="Usuario"
          required
          autoComplete="off"
          value={form.username}
          onChange={handleChange('username')}
        />

        <Input
          label="Correo"
          type="email"
          required
          autoComplete="off"
          value={form.email}
          onChange={handleChange('email')}
        />

        <Input
          label="Contraseña inicial"
          type="password"
          required
          autoComplete="new-password"
          value={form.password}
          onChange={handleChange('password')}
          error={
            form.password && form.password.length < 8
              ? 'Mínimo 8 caracteres, con mayúscula, minúscula y número'
              : undefined
          }
        />

        <fieldset>
          <legend className="mb-2 text-sm font-medium text-slate-700">Roles</legend>
          <div className="grid gap-2 sm:grid-cols-2">
            {roles.map((role) => (
              <CheckOption
                key={role.id}
                label={roleLabel(role.name)}
                checked={selectedRoles.includes(role.name)}
                onToggle={() => toggleRole(role.name)}
              />
            ))}
          </div>
        </fieldset>

        <ModalFooter>
          <Button type="button" variant="secondary" onClick={handleClose}>
            Cancelar
          </Button>
          <Button type="submit" isLoading={isSubmitting}>
            Crear usuario
          </Button>
        </ModalFooter>
      </form>
    </Modal>
  );
}
