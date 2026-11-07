import { fileURLToPath, URL } from 'node:url'

import { defineConfig, loadEnv } from 'vite'
import vue from '@vitejs/plugin-vue'
import vueDevTools from 'vite-plugin-vue-devtools'

const rootDir = fileURLToPath(new URL('.', import.meta.url))

// https://vite.dev/config/
export default defineConfig(({ mode }) => {
  const env = loadEnv(mode, rootDir, '')
  // The Vite dev server proxies /api to the backend. Set VITE_BACKEND_URL in
  // frontend/.env to point at another host; localhost:8080 otherwise.
  const backendTarget = env.VITE_BACKEND_URL || 'http://localhost:8080'

  return {
    plugins: [
      vue(),
      vueDevTools(),
    ],
    resolve: {
      alias: {
        '@': fileURLToPath(new URL('./src', import.meta.url)),
      },
    },
    server: {
      proxy: {
        '/api': {
          target: backendTarget,
          changeOrigin: false,
          // Append the browser's address to X-Forwarded-For so the backend can
          // apply its per-IP rate limits, admin network checks and audit logging
          // to the real client instead of the dev server.
          xfwd: true,
        },
      },
    },
  }
})
