import { useState, type ChangeEvent, type FormEvent } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { AtSign, Lock, User as UserIcon } from 'lucide-react';
import { AuthCard } from '../components/layout/AuthCard';
import { Button } from '../components/ui/Button';
import { Input } from '../components/ui/Input';
import { Notice } from '../components/ui/Notice';
import { getErrorMessage } from '../api/client';
import { useAuth } from '../hooks/useAuth';

/** Estado inicial del formulario. */
const EMPTY_FORM = {
  firstName: '',
  lastName: '',
  username: '',
  email: '',
  password: '',
  confirmPassword: '',
};

/**
 * Pantalla de registro.
 *
 * La validación aquí sirve para responder al instante; el backend vuelve a validarlo
 * todo, porque cualquiera puede saltarse el formulario y llamar a la API directamente.
 */
export function RegisterPage() {
  const navigate = useNavigate();
  const { register } = useAuth();

  const [form, setForm] = useState(EMPTY_FORM);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);
  const [isSubmitting, setIsSubmitting] = useState(false);

  /**
   * Devuelve el manejador de cambio de un campo.
   *
   * @param field nombre del campo del formulario
   * @returns manejador del evento `change`
   */
  function handleChange(field: keyof typeof EMPTY_FORM) {
    return (event: ChangeEvent<HTMLInputElement>) => {
      setForm((current) => ({ ...current, [field]: event.target.value }));
    };
  }

  /**
   * Crea la cuenta y lleva al login con un mensaje de confirmación.
   *
   * @param event evento de envío del formulario
   */
  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setErrorMessage(null);

    if (form.password !== form.confirmPassword) {
      setErrorMessage('Las contraseñas no coinciden.');
      return;
    }

    setIsSubmitting(true);

    try {
      await register({
        username: form.username.trim(),
        email: form.email.trim(),
        password: form.password,
        firstName: form.firstName.trim() || undefined,
        lastName: form.lastName.trim() || undefined,
      });

      navigate('/login', {
        replace: true,
        state: { notice: 'Cuenta creada. Ya puedes iniciar sesión.' },
      });
    } catch (error) {
      setErrorMessage(getErrorMessage(error));
    } finally {
      setIsSubmitting(false);
    }
  }

  return (
    <AuthCard
      title="Crear cuenta"
      subtitle="Completa tus datos para empezar"
      footer={
        <>
          ¿Ya tienes cuenta?{' '}
          <Link to="/login" className="font-medium text-indigo-600 hover:text-indigo-500">
            Inicia sesión
          </Link>
        </>
      }
    >
      <form onSubmit={handleSubmit} className="space-y-4" noValidate>
        {errorMessage && <Notice>{errorMessage}</Notice>}

        <div className="grid grid-cols-2 gap-3">
          <Input
            label="Nombre"
            autoComplete="given-name"
            value={form.firstName}
            onChange={handleChange('firstName')}
          />
          <Input
            label="Apellido"
            autoComplete="family-name"
            value={form.lastName}
            onChange={handleChange('lastName')}
          />
        </div>

        <Input
          label="Usuario"
          autoComplete="username"
          required
          icon={<UserIcon className="size-4" />}
          value={form.username}
          onChange={handleChange('username')}
        />

        <Input
          label="Correo"
          type="email"
          autoComplete="email"
          required
          icon={<AtSign className="size-4" />}
          value={form.email}
          onChange={handleChange('email')}
        />

        <Input
          label="Contraseña"
          type="password"
          autoComplete="new-password"
          required
          icon={<Lock className="size-4" />}
          value={form.password}
          onChange={handleChange('password')}
          error={
            form.password && form.password.length < 8
              ? 'Mínimo 8 caracteres, con mayúscula, minúscula y número'
              : undefined
          }
        />

        <Input
          label="Repetir contraseña"
          type="password"
          autoComplete="new-password"
          required
          icon={<Lock className="size-4" />}
          value={form.confirmPassword}
          onChange={handleChange('confirmPassword')}
          error={
            form.confirmPassword && form.confirmPassword !== form.password
              ? 'No coincide'
              : undefined
          }
        />

        <Button type="submit" fullWidth isLoading={isSubmitting} className="mt-2">
          Crear cuenta
        </Button>
      </form>
    </AuthCard>
  );
}
