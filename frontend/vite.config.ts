import { fileURLToPath, URL } from 'node:url'
import { cwd } from 'node:process'

import { defineConfig, loadEnv } from 'vite'
import vue from '@vitejs/plugin-vue'
import vueDevTools from 'vite-plugin-vue-devtools'

// https://vite.dev/config/
export default defineConfig(({ mode }) => {
  const env = loadEnv(mode, cwd(), '')
  const backendUrl = env.VITE_BACKEND_URL || `http://localhost:${env.VITE_BACKEND_PORT || '8080'}`

  return {
    plugins: [
      vue(),
      vueDevTools(),
    ],
    resolve: {
      alias: {
        '@': fileURLToPath(new URL('./src', import.meta.url))
      },
    },
    server: {
      proxy: {
        '/api': {
          target: backendUrl,
          changeOrigin: true
        }
      }
    }
  }
})
