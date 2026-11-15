import { useEffect, useState, type FormEvent } from 'react';
import clsx from 'clsx';
import { menuApi } from '../../api/authApi';
import { getErrorMessage } from '../../api/client';
import { Button } from '../ui/Button';
import { Input } from '../ui/Input';
import { Modal, ModalFooter } from '../ui/Modal';
import { Notice } from '../ui/Notice';
import type { MenuItem, Permission } from '../../types/auth';
import { MENU_ICONS } from '../../utils/icons';
import { permissionLabel } from '../../utils/labels';

interface MenuItemDialogProps {
  /** Si el diálogo está visible. */
  isOpen: boolean;
  /** Entrada a editar, o `null` para crear una nueva. */
  item: MenuItem | null;
  /** Catálogo de permisos para el desplegable. */
  permissions: Permission[];
  /** Posición sugerida al crear una entrada nueva. */
  nextOrder: number;
  /** Se ejecuta al cerrar sin guardar. */
  onClose: () => void;
  /** Se ejecuta cuando la entrada se guardó correctamente. */
  onSaved: () => void;
}

/**
 * Formulario de crear y editar entradas del menú, con selector de icono.
 *
 * @param props estado, entrada a editar, catálogo de permisos y acciones de cierre
 */
export function MenuItemDialog({
  isOpen,
  item,
  permissions,
  nextOrder,
  onClose,
  onSaved,
}: MenuItemDialogProps) {
  const [label, setLabel] = useState('');
  const [path, setPath] = useState('');
  const [icon, setIcon] = useState('folder');
  const [requiredPermission, setRequiredPermission] = useState('');
  const [displayOrder, setDisplayOrder] = useState(0);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);
  const [isSubmitting, setIsSubmitting] = useState(false);

  const isEditing = item !== null;

  /** Precarga el formulario al abrirlo. */
  useEffect(() => {
    if (!isOpen) {
      return;
    }
    setLabel(item?.label ?? '');
    setPath(item?.path ?? '/panel/');
    setIcon(item?.icon ?? 'folder');
    setRequiredPermission(item?.requiredPermission ?? '');
    setDisplayOrder(item?.displayOrder ?? nextOrder);
    setErrorMessage(null);
  }, [isOpen, item, nextOrder]);

  /**
   * Guarda la entrada, creándola o actualizándola según el caso.
   *
   * @param event evento de envío del formulario
   */
  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setErrorMessage(null);
    setIsSubmitting(true);

    const payload = {
      label: label.trim(),
      path: path.trim(),
      icon,
      requiredPermission,
      displayOrder,
      visible: item?.visible ?? true,
    };

    try {
      if (isEditing) {
        await menuApi.update(item.id, payload);
      } else {
        await menuApi.create(payload);
      }
      onSaved();
    } catch (error) {
      setErrorMessage(getErrorMessage(error));
    } finally {
      setIsSubmitting(false);
    }
  }

  return (
    <Modal isOpen={isOpen} title={isEditing ? 'Editar entrada' : 'Nueva entrada'} onClose={onClose}>
      <form onSubmit={handleSubmit} className="space-y-4" noValidate>
        {errorMessage && <Notice>{errorMessage}</Notice>}

        <div className="grid grid-cols-[1fr_96px] gap-3">
          <Input
            label="Texto"
            required
            placeholder="Reportes"
            autoComplete="off"
            value={label}
            onChange={(event) => setLabel(event.target.value)}
          />
          <Input
            label="Posición"
            type="number"
            min={0}
            value={displayOrder}
            onChange={(event) => setDisplayOrder(Number(event.target.value))}
          />
        </div>

        <Input
          label="Ruta"
          required
          placeholder="/panel/reportes"
          autoComplete="off"
          value={path}
          onChange={(event) => setPath(event.target.value)}
          error={path && !path.startsWith('/') ? 'Debe empezar por /' : undefined}
        />

        <fieldset>
          <legend className="mb-2 text-sm font-medium text-slate-700">Icono</legend>
          <div className="grid grid-cols-6 gap-2 sm:grid-cols-9">
            {Object.entries(MENU_ICONS).map(([name, Icon]) => (
              <button
                key={name}
                type="button"
                onClick={() => setIcon(name)}
                aria-label={name}
                aria-pressed={icon === name}
                className={clsx(
                  'flex aspect-square items-center justify-center rounded-lg border transition-colors',
                  icon === name
                    ? 'border-indigo-400 bg-indigo-50 text-indigo-600'
                    : 'border-slate-200 text-slate-500 hover:bg-slate-50 hover:text-slate-700',
                )}
              >
                <Icon className="size-4.5" />
              </button>
            ))}
          </div>
        </fieldset>

        <div>
          <label htmlFor="menu-permission" className="mb-1.5 block text-sm font-medium text-slate-700">
            Quién la ve
          </label>
          <select
            id="menu-permission"
            value={requiredPermission}
            onChange={(event) => setRequiredPermission(event.target.value)}
            className="h-10 w-full rounded-lg border border-slate-200 bg-white px-3 text-sm text-slate-900 shadow-sm focus:border-indigo-400 focus:outline-none focus:ring-4 focus:ring-indigo-100"
          >
            <option value="">Todos los usuarios</option>
            {permissions.map((permission) => (
              <option key={permission.id} value={permission.name}>
                Quien tenga: {permissionLabel(permission.name)}
              </option>
            ))}
          </select>
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
