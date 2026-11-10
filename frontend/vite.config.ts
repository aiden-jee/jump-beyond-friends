import { defineConfig, loadEnv } from 'vite';

export default defineConfig(({ mode }) => {
  const env = loadEnv(mode, '.', '');
  const proxy = { '/api': { target: env.BACKEND_URL || 'http://localhost:8080', changeOrigin: true } };
  return {
    server: { port: 5173, strictPort: true, proxy },
    preview: { proxy },
  };
});
