import { useEffect, useRef, useState, type FormEvent } from 'react';
import { Link, useLocation, useNavigate } from 'react-router-dom';
import { authApi } from '../api/authApi';
import { getErrorMessage } from '../api/client';
import { AuthCard } from '../components/layout/AuthCard';
import { Button } from '../components/ui/Button';
import { Notice } from '../components/ui/Notice';
import { useAuth } from '../hooks/useAuth';

/** Lo que el login deja en el estado de la ruta al redirigir aquí. */
interface VerifyState {
  challengeId: string;
  maskedEmail?: string;
  from?: { pathname: string };
}

/**
 * Segundo paso del inicio de sesión: el código que llegó por correo.
 *
 * Solo se llega aquí desde el login, que deja el `challengeId` en el estado de la
 * ruta. Si alguien abre la dirección a pelo, no hay identificador y se le devuelve
 * al login en lugar de mostrarle un formulario que no podría funcionar.
 */
export function VerifyCodePage() {
  const navigate = useNavigate();
  const location = useLocation();
  const { verifyCode } = useAuth();

  const state = location.state as VerifyState | null;

  const [code, setCode] = useState('');
  const [errorMessage, setErrorMessage] = useState<string | null>(null);
  const [noticeMessage, setNoticeMessage] = useState<string | null>(null);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [isResending, setIsResending] = useState(false);
  const [cooldown, setCooldown] = useState(0);

  const inputRef = useRef<HTMLInputElement>(null);

  /** Sin identificador no se puede verificar nada: de vuelta al login. */
  useEffect(() => {
    if (!state?.challengeId) {
      navigate('/login', { replace: true });
    }
  }, [state, navigate]);

  /** El foco empieza en el campo, para poder escribir o pegar el código directamente. */
  useEffect(() => {
    inputRef.current?.focus();
  }, []);

  /** Cuenta atrás del botón de reenviar. */
  useEffect(() => {
    if (cooldown <= 0) {
      return;
    }
    const temporizador = setTimeout(() => setCooldown((segundos) => segundos - 1), 1000);
    return () => clearTimeout(temporizador);
  }, [cooldown]);

  if (!state?.challengeId) {
    return null;
  }

  /**
   * Envía el código y entra en la aplicación si es correcto.
   *
   * @param event evento de envío del formulario
   */
  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setErrorMessage(null);
    setNoticeMessage(null);
    setIsSubmitting(true);

    try {
      await verifyCode(state!.challengeId, code);
      navigate(state!.from?.pathname ?? '/panel', { replace: true });
    } catch (error) {
      setErrorMessage(getErrorMessage(error));
      setCode('');
      inputRef.current?.focus();
    } finally {
      setIsSubmitting(false);
    }
  }

  /**
   * Pide que se envíe un código nuevo.
   */
  async function handleResend() {
    setErrorMessage(null);
    setNoticeMessage(null);
    setIsResending(true);

    try {
      await authApi.resendCode(state!.challengeId);
      setNoticeMessage('Te hemos enviado un código nuevo.');
      setCooldown(60);
      setCode('');
      inputRef.current?.focus();
    } catch (error) {
      setErrorMessage(getErrorMessage(error));
    } finally {
      setIsResending(false);
    }
  }

  return (
    <AuthCard
      title="Revisa tu correo"
      subtitle={
        state.maskedEmail
          ? `Enviamos un código a ${state.maskedEmail}`
          : 'Enviamos un código a tu correo'
      }
      footer={
        <Link to="/login" className="font-medium text-indigo-600 hover:text-indigo-500">
          Volver al inicio de sesión
        </Link>
      }
    >
      <form onSubmit={handleSubmit} className="space-y-4" noValidate>
        {noticeMessage && <Notice variant="success">{noticeMessage}</Notice>}
        {errorMessage && <Notice>{errorMessage}</Notice>}

        <div>
          <label htmlFor="codigo" className="mb-1.5 block text-sm font-medium text-slate-700">
            Código de verificación
          </label>
          <input
            id="codigo"
            ref={inputRef}
            // inputMode numérico abre el teclado de cifras en el móvil, y
            // one-time-code deja que iOS y Android lo rellenen desde el correo.
            inputMode="numeric"
            autoComplete="one-time-code"
            maxLength={6}
            required
            placeholder="000000"
            value={code}
            onChange={(event) => setCode(event.target.value.replace(/[^0-9]/g, ''))}
            className="h-14 w-full rounded-lg border border-slate-200 bg-white text-center font-mono text-2xl tracking-[0.4em] text-slate-900 shadow-sm transition placeholder:text-slate-300 focus:border-indigo-400 focus:outline-none focus:ring-4 focus:ring-indigo-100"
          />
        </div>

        <Button type="submit" fullWidth isLoading={isSubmitting} disabled={code.length < 4}>
          Verificar
        </Button>
      </form>

      <div className="mt-5 text-center">
        <button
          type="button"
          onClick={handleResend}
          disabled={isResending || cooldown > 0}
          className="text-sm text-slate-500 transition-colors hover:text-slate-800 disabled:cursor-not-allowed disabled:opacity-50"
        >
          {cooldown > 0 ? `Reenviar en ${cooldown}s` : 'No me llegó, reenviar código'}
        </button>
      </div>
    </AuthCard>
  );
}
