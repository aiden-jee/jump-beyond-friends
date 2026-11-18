import { defineConfig, loadEnv } from 'vite';

export default defineConfig(({ mode }) => {
  const env = loadEnv(mode, '.', '');
  // Keep the frontend Host so Spring builds its OAuth callback on this origin.
  const backend = { target: env.BACKEND_URL || 'http://localhost:8080', changeOrigin: false };
  const proxy = {
    '/api': backend,
    '/oauth2/authorization': backend,
    '/login/oauth2/code': backend,
    '/logout': backend,
  };
  return {
    server: { port: 5173, strictPort: true, proxy },
    preview: { proxy },
  };
});
