import { Navigate, Route, Routes } from 'react-router-dom';
import { AppLayout } from '../components/layout/AppLayout';
import { DashboardPage } from '../pages/DashboardPage';
import { EmptySectionPage } from '../pages/EmptySectionPage';
import { ForbiddenPage } from '../pages/ForbiddenPage';
import { LoginPage } from '../pages/LoginPage';
import { MenuPage } from '../pages/MenuPage';
import { NotFoundPage } from '../pages/NotFoundPage';
import { ProfilePage } from '../pages/ProfilePage';
import { RegisterPage } from '../pages/RegisterPage';
import { VerifyCodePage } from '../pages/VerifyCodePage';
import { RolesPage } from '../pages/RolesPage';
import { UsersPage } from '../pages/UsersPage';
import { ProtectedRoute, PublicOnlyRoute } from './ProtectedRoute';
import { PERMISSIONS } from '../types/auth';

/**
 * Mapa de rutas de la aplicación.
 *
 * Tres bloques que se leen de un vistazo:
 *
 * 1. Rutas solo para quien NO ha iniciado sesión (login y registro).
 * 2. Rutas protegidas bajo el layout del panel, cada una con sus permisos.
 * 3. Rutas sueltas (404).
 *
 * La última ruta dentro del panel es un comodín: recoge las entradas de menú creadas
 * desde el administrador cuya pantalla todavía no existe y muestra un aviso en lugar de
 * un 404. Sin ella, crear una entrada de menú produciría un enlace roto.
 */
export function AppRoutes() {
  return (
    <Routes>
      {/* La raíz lleva al panel; si no hay sesión, el guard desvía al login */}
      <Route path="/" element={<Navigate to="/panel" replace />} />

      {/* ---------- Solo para visitantes sin sesión ---------- */}
      <Route element={<PublicOnlyRoute />}>
        <Route path="/login" element={<LoginPage />} />
        <Route path="/registro" element={<RegisterPage />} />
        {/* Segundo paso del login; solo se llega desde /login */}
        <Route path="/verificar" element={<VerifyCodePage />} />
      </Route>

      {/* ---------- Requieren sesión iniciada ---------- */}
      <Route element={<ProtectedRoute />}>
        <Route path="/panel" element={<AppLayout />}>
          <Route index element={<DashboardPage />} />
          <Route path="perfil" element={<ProfilePage />} />

          {/* Estas secciones exigen además un permiso concreto */}
          <Route element={<ProtectedRoute requiredPermissions={[PERMISSIONS.USER_READ]} />}>
            <Route path="usuarios" element={<UsersPage />} />
          </Route>

          <Route element={<ProtectedRoute requiredPermissions={[PERMISSIONS.ROLE_READ]} />}>
            <Route path="roles" element={<RolesPage />} />
          </Route>

          <Route element={<ProtectedRoute requiredPermissions={[PERMISSIONS.MENU_WRITE]} />}>
            <Route path="menu" element={<MenuPage />} />
          </Route>

          {/* Entradas de menú creadas desde el panel que aún no tienen pantalla */}
          <Route path="*" element={<EmptySectionPage />} />
        </Route>

        <Route path="/sin-acceso" element={<ForbiddenPage />} />
      </Route>

      {/* ---------- Cualquier otra ruta ---------- */}
      <Route path="*" element={<NotFoundPage />} />
    </Routes>
  );
}
