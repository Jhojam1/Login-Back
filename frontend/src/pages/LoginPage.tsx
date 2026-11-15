import { useState, type FormEvent } from 'react';
import { Link, useLocation, useNavigate } from 'react-router-dom';
import { Lock, User as UserIcon } from 'lucide-react';
import { AuthCard } from '../components/layout/AuthCard';
import { Button } from '../components/ui/Button';
import { Input } from '../components/ui/Input';
import { Notice } from '../components/ui/Notice';
import { getErrorMessage } from '../api/client';
import { useAuth } from '../hooks/useAuth';

/**
 * Pantalla de inicio de sesión.
 *
 * Tras entrar devuelve a la página que se intentaba abrir antes de ser redirigido
 * (la guarda `ProtectedRoute` en el estado de la ruta); si se entró directamente, al panel.
 */
export function LoginPage() {
  const navigate = useNavigate();
  const location = useLocation();
  const { login } = useAuth();

  const [identifier, setIdentifier] = useState('');
  const [password, setPassword] = useState('');
  const [errorMessage, setErrorMessage] = useState<string | null>(null);
  const [isSubmitting, setIsSubmitting] = useState(false);

  /** Ruta a la que volver después de entrar. */
  const redirectTo =
    (location.state as { from?: { pathname: string } } | null)?.from?.pathname ?? '/panel';

  /** Mensaje que llega desde el registro o el cambio de contraseña. */
  const noticeMessage = (location.state as { notice?: string } | null)?.notice;

  /**
   * Envía las credenciales y navega al panel si son correctas.
   *
   * @param event evento de envío del formulario
   */
  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setErrorMessage(null);
    setIsSubmitting(true);

    try {
      await login({ identifier: identifier.trim(), password });
      navigate(redirectTo, { replace: true });
    } catch (error) {
      setErrorMessage(getErrorMessage(error));
    } finally {
      setIsSubmitting(false);
    }
  }

  return (
    <AuthCard
      title="Bienvenido"
      subtitle="Inicia sesión para continuar"
      footer={
        <>
          ¿No tienes cuenta?{' '}
          <Link to="/registro" className="font-medium text-indigo-600 hover:text-indigo-500">
            Regístrate
          </Link>
        </>
      }
    >
      <form onSubmit={handleSubmit} className="space-y-4" noValidate>
        {noticeMessage && <Notice variant="success">{noticeMessage}</Notice>}
        {errorMessage && <Notice>{errorMessage}</Notice>}

        <Input
          label="Usuario o correo"
          name="identifier"
          autoComplete="username"
          autoFocus
          required
          icon={<UserIcon className="size-4" />}
          value={identifier}
          onChange={(event) => setIdentifier(event.target.value)}
        />

        <Input
          label="Contraseña"
          name="password"
          type="password"
          autoComplete="current-password"
          required
          icon={<Lock className="size-4" />}
          value={password}
          onChange={(event) => setPassword(event.target.value)}
        />

        <Button type="submit" fullWidth isLoading={isSubmitting} className="mt-2">
          Iniciar sesión
        </Button>
      </form>
    </AuthCard>
  );
}
