import { useState, type FormEvent } from 'react';
import { useNavigate } from 'react-router-dom';
import { Lock } from 'lucide-react';
import { authApi } from '../api/authApi';
import { getErrorMessage } from '../api/client';
import { Badge } from '../components/ui/Badge';
import { Button } from '../components/ui/Button';
import { Input } from '../components/ui/Input';
import { Notice } from '../components/ui/Notice';
import { PageHeader } from '../components/ui/PageHeader';
import { useAuth } from '../hooks/useAuth';
import { ROLES } from '../types/auth';
import { formatDate, roleLabel } from '../utils/labels';

/**
 * Pantalla de la cuenta propia, con el cambio de contraseña.
 *
 * Tras cambiarla el backend revoca todas las sesiones, así que aquí se cierra la sesión
 * local y se vuelve al login.
 */
export function ProfilePage() {
  const { user, logout } = useAuth();
  const navigate = useNavigate();

  const [currentPassword, setCurrentPassword] = useState('');
  const [newPassword, setNewPassword] = useState('');
  const [confirmPassword, setConfirmPassword] = useState('');
  const [errorMessage, setErrorMessage] = useState<string | null>(null);
  const [isSubmitting, setIsSubmitting] = useState(false);

  /**
   * Envía el cambio de contraseña y cierra la sesión si todo va bien.
   *
   * @param event evento de envío del formulario
   */
  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setErrorMessage(null);

    if (newPassword !== confirmPassword) {
      setErrorMessage('Las contraseñas nuevas no coinciden.');
      return;
    }

    setIsSubmitting(true);

    try {
      await authApi.changePassword(currentPassword, newPassword);
      await logout();
      navigate('/login', {
        replace: true,
        state: { notice: 'Contraseña actualizada. Inicia sesión con la nueva.' },
      });
    } catch (error) {
      setErrorMessage(getErrorMessage(error));
      setIsSubmitting(false);
    }
  }

  if (!user) {
    return null;
  }

  const initials =
    (user.firstName?.[0] ?? user.username[0]).toUpperCase() + (user.lastName?.[0] ?? '').toUpperCase();

  return (
    <div className="animate-rise">
      <PageHeader title="Cuenta" />

      <div className="grid gap-6 lg:grid-cols-[320px_1fr]">
        <section className="card h-fit p-6">
          <div className="flex flex-col items-center text-center">
            <div className="flex size-16 items-center justify-center rounded-full bg-gradient-to-br from-indigo-500 to-violet-500 text-xl font-semibold text-white">
              {initials}
            </div>
            <h2 className="mt-4 font-semibold text-slate-900">{user.fullName}</h2>
            <p className="text-sm text-slate-500">{user.email}</p>
            <div className="mt-3 flex flex-wrap justify-center gap-1.5">
              {user.roles.map((role) => (
                <Badge key={role} tone={role === ROLES.ADMIN ? 'indigo' : 'slate'}>
                  {roleLabel(role)}
                </Badge>
              ))}
            </div>
          </div>

          <dl className="mt-6 space-y-3 border-t border-slate-100 pt-5 text-sm">
            <div className="flex justify-between gap-4">
              <dt className="text-slate-500">Usuario</dt>
              <dd className="font-medium text-slate-900">{user.username}</dd>
            </div>
            <div className="flex justify-between gap-4">
              <dt className="text-slate-500">Miembro desde</dt>
              <dd className="font-medium text-slate-900">{formatDate(user.createdAt)}</dd>
            </div>
          </dl>
        </section>

        <section className="card p-6">
          <h2 className="font-semibold text-slate-900">Cambiar contraseña</h2>
          <p className="mt-1 text-sm text-slate-500">Se cerrarán tus otras sesiones.</p>

          <form onSubmit={handleSubmit} className="mt-5 max-w-md space-y-4" noValidate>
            {errorMessage && <Notice>{errorMessage}</Notice>}

            <Input
              label="Contraseña actual"
              type="password"
              autoComplete="current-password"
              required
              icon={<Lock className="size-4" />}
              value={currentPassword}
              onChange={(event) => setCurrentPassword(event.target.value)}
            />

            <Input
              label="Contraseña nueva"
              type="password"
              autoComplete="new-password"
              required
              icon={<Lock className="size-4" />}
              value={newPassword}
              onChange={(event) => setNewPassword(event.target.value)}
              error={
                newPassword && newPassword.length < 8
                  ? 'Mínimo 8 caracteres, con mayúscula, minúscula y número'
                  : undefined
              }
            />

            <Input
              label="Repetir contraseña nueva"
              type="password"
              autoComplete="new-password"
              required
              icon={<Lock className="size-4" />}
              value={confirmPassword}
              onChange={(event) => setConfirmPassword(event.target.value)}
              error={confirmPassword && confirmPassword !== newPassword ? 'No coincide' : undefined}
            />

            <Button type="submit" isLoading={isSubmitting}>
              Actualizar contraseña
            </Button>
          </form>
        </section>
      </div>
    </div>
  );
}
