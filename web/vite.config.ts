import { defineConfig, loadEnv } from 'vite';
import react from '@vitejs/plugin-react';

export default defineConfig(({ mode }) => {
  const env = loadEnv(mode, process.cwd(), '');

  return {
    // Relative asset paths make the built app work both on a domain root and under a subpath.
    base: env.VITE_APP_BASE_PATH || './',
    plugins: [react()],
    build: {
      outDir: 'dist',
      assetsDir: 'assets',
      sourcemap: false,
    },
    server: {
      host: '0.0.0.0',
      port: 3000,
      strictPort: true,
      proxy: {
        '/core-api': {
          target: env.VITE_CORE_PROXY_TARGET || 'http://localhost:8080',
          changeOrigin: true,
          secure: false,
          rewrite: (path) => path.replace(/^\/core-api/, ''),
        },
        '/currencies-api': {
          target: env.VITE_CURRENCIES_PROXY_TARGET || 'http://localhost:8081',
          changeOrigin: true,
          secure: false,
          rewrite: (path) => path.replace(/^\/currencies-api/, ''),
        },
      },
    },
  };
});
