import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';

// The browser always calls /api on its own origin; in development Vite forwards it to the backend.
const apiProxyTarget = process.env.API_PROXY_TARGET ?? 'http://localhost:8080';

export default defineConfig({
  plugins: [react()],
  server: {
    port: 5173,
    proxy: {
      '/api': apiProxyTarget,
    },
  },
});
