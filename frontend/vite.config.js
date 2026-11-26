import react from '@vitejs/plugin-react'
import { defineConfig } from 'vite'

// The Spring Boot backend (../backend). Override with BACKEND_URL if it runs elsewhere.
const backend = process.env.BACKEND_URL ?? 'http://localhost:8080'

// https://vite.dev/config/
export default defineConfig({
  plugins: [react()],
  server: {
    // Port 5020 is already in the backend's live-feed.allowed-origins.
    port: 5020,
    strictPort: true,
    // The React app calls relative URLs (/api/..., /ws/fleet); the dev server forwards them to the
    // backend, so the browser sees one origin and no CORS setup is needed.
    proxy: {
      '/api': backend,
      '/ws': { target: backend, ws: true },
    },
  },
})
