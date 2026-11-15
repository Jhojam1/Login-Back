import { useEffect, useState, type FormEvent } from 'react';
import { adminApi } from '../../api/authApi';
import { getErrorMessage } from '../../api/client';
import { Button } from '../ui/Button';
import { Input } from '../ui/Input';
import { Modal, ModalFooter } from '../ui/Modal';
import { Notice } from '../ui/Notice';

interface PermissionDialogProps {
  /** Si el diálogo está visible. */
  isOpen: boolean;
  /** Se ejecuta al cerrar sin guardar. */
  onClose: () => void;
  /** Se ejecuta cuando el permiso se creó correctamente. */
  onCreated: () => void;
}

/**
 * Formulario para crear un permiso de interfaz.
 *
 * Estos permisos sirven para mostrar u ocultar partes de la interfaz (por ejemplo una
 * entrada del menú). No protegen operaciones del servidor: para eso hay que declararlos
 * en el backend.
 *
 * @param props estado y acciones de cierre
 */
export function PermissionDialog({ isOpen, onClose, onCreated }: PermissionDialogProps) {
  const [name, setName] = useState('');
  const [description, setDescription] = useState('');
  const [errorMessage, setErrorMessage] = useState<string | null>(null);
  const [isSubmitting, setIsSubmitting] = useState(false);

  useEffect(() => {
    if (isOpen) {
      setName('');
      setDescription('');
      setErrorMessage(null);
    }
  }, [isOpen]);

  /**
   * Crea el permiso.
   *
   * @param event evento de envío del formulario
   */
  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setErrorMessage(null);
    setIsSubmitting(true);

    try {
      await adminApi.createPermission({ name: name.trim(), description: description.trim() });
      onCreated();
    } catch (error) {
      setErrorMessage(getErrorMessage(error));
    } finally {
      setIsSubmitting(false);
    }
  }

  return (
    <Modal isOpen={isOpen} title="Nuevo permiso" onClose={onClose}>
      <form onSubmit={handleSubmit} className="space-y-4" noValidate>
        {errorMessage && <Notice>{errorMessage}</Notice>}

        <Input
          label="Nombre"
          required
          placeholder="REPORT_VIEW"
          autoComplete="off"
          value={name}
          onChange={(event) => setName(event.target.value.toUpperCase())}
        />

        <Input
          label="Descripción"
          placeholder="Ver la sección de reportes"
          autoComplete="off"
          value={description}
          onChange={(event) => setDescription(event.target.value)}
        />

        <ModalFooter>
          <Button type="button" variant="secondary" onClick={onClose}>
            Cancelar
          </Button>
          <Button type="submit" isLoading={isSubmitting}>
            Crear permiso
          </Button>
        </ModalFooter>
      </form>
    </Modal>
  );
}
