import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

// https://vite.dev/config/
export default defineConfig({
  plugins: [react()],
  // Provide a browser-friendly global for libraries that expect `global`
  define: {
    global: 'window',
  },
  // Pre-bundle these deps to avoid ESM/CJS resolution issues
  optimizeDeps: {
    include: ['@stomp/stompjs', 'sockjs-client'],
  },
  build: {
    commonjsOptions: {
      include: [/node_modules/],
    },
  },
  server: {
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true,
        secure: false,
      },
      // Proxy websocket (SockJS) endpoint to backend during development
      '/ws': {
        target: 'http://localhost:8080',
        ws: true,
        changeOrigin: true,
        secure: false,
      },
    },
  },
})
