import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';
import tailwindcss from '@tailwindcss/vite';

/**
 * Configuracion de Vite (el servidor de desarrollo y el empaquetador).
 */
export default defineConfig({
  plugins: [react(), tailwindcss()],
  server: {
    port: 5173,
    // Redirige /api del servidor de desarrollo al backend de Spring.
    // Asi en desarrollo no hay problemas de CORS y el codigo usa rutas relativas,
    // exactamente igual que en produccion detras de Nginx.
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true,
      },
    },
  },
  build: {
    outDir: 'dist',
    sourcemap: false,
  },
});
