import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { ArrowRight, Clock, ShieldCheck, Users, type LucideIcon } from 'lucide-react';
import { adminApi } from '../api/authApi';
import { useAuth } from '../hooks/useAuth';
import { PERMISSIONS } from '../types/auth';
import { formatDate, roleLabel } from '../utils/labels';

interface StatCardProps {
  label: string;
  value: string | number;
  icon: LucideIcon;
  to?: string;
}

/**
 * Tarjeta de un dato con icono. Si recibe `to`, toda la tarjeta enlaza a esa sección.
 *
 * @param props etiqueta, valor, icono y enlace opcional
 */
function StatCard({ label, value, icon: Icon, to }: StatCardProps) {
  const content = (
    <>
      <div className="flex size-10 items-center justify-center rounded-lg bg-indigo-50 text-indigo-600">
        <Icon className="size-5" aria-hidden />
      </div>
      <div className="min-w-0 flex-1">
        <p className="text-sm text-slate-500">{label}</p>
        <p className="truncate text-xl font-semibold text-slate-900">{value}</p>
      </div>
      {to && (
        <ArrowRight className="size-4 text-slate-300 transition-transform group-hover:translate-x-0.5 group-hover:text-slate-500" />
      )}
    </>
  );

  const className = 'card group flex items-center gap-4 p-5';

  return to ? (
    <Link to={to} className={`${className} transition-shadow hover:shadow-md`}>
      {content}
    </Link>
  ) : (
    <div className={className}>{content}</div>
  );
}

/**
 * Pantalla de inicio del panel: saludo y unos pocos datos útiles.
 *
 * El total de usuarios solo se pide y se muestra si la persona tiene
 * permiso para ver esa sección.
 */
export function DashboardPage() {
  const { user, hasPermission } = useAuth();
  const [userCount, setUserCount] = useState<number | null>(null);

  const canSeeUsers = hasPermission(PERMISSIONS.USER_READ);

  /**
   * Carga el total de usuarios, solo si esta persona puede verlos.
   */
  useEffect(() => {
    if (!canSeeUsers) {
      return;
    }

    let cancelled = false;

    adminApi
      .listUsers(0, 1)
      .then((page) => {
        if (!cancelled) {
          setUserCount(page.totalElements);
        }
      })
      .catch(() => {
        // El total no es crítico: si falla, simplemente no se muestra.
      });

    return () => {
      cancelled = true;
    };
  }, [canSeeUsers]);

  if (!user) {
    return null;
  }

  return (
    <div className="animate-rise">
      <div className="mb-8">
        <h1 className="text-2xl font-semibold tracking-tight text-slate-900">
          Hola, {user.firstName ?? user.username} 👋
        </h1>
        <p className="mt-1 text-sm text-slate-500">Esto es lo que hay hoy en tu panel.</p>
      </div>

      <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
        <StatCard
          label="Tu acceso"
          value={user.roles.map(roleLabel).join(', ')}
          icon={ShieldCheck}
        />
        <StatCard label="Último inicio de sesión" value={formatDate(user.lastLoginAt)} icon={Clock} />

        {userCount !== null && (
          <StatCard label="Usuarios registrados" value={userCount} icon={Users} to="/panel/usuarios" />
        )}
      </div>
    </div>
  );
}
