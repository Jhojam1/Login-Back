import { BrowserRouter } from 'react-router-dom';
import { AuthProvider } from './context/AuthContext';
import { AppRoutes } from './routes/AppRoutes';

/**
 * Raíz de la aplicación.
 *
 * El orden importa: el enrutador envuelve al proveedor de sesión porque los guards
 * de ruta necesitan poder navegar, y el proveedor envuelve a las rutas porque todas
 * las pantallas consultan la sesión.
 */
export default function App() {
  return (
    <BrowserRouter>
      <AuthProvider>
        <AppRoutes />
      </AuthProvider>
    </BrowserRouter>
  );
}
