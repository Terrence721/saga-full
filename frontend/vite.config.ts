import react from '@vitejs/plugin-react'
import { defineConfig } from 'vite'

// https://vite.dev/config/
export default defineConfig({
  plugins: [react()],
  server: {
    // Pinned rather than left to Vite's automatic fallback - this machine runs
    // several portfolio projects' dev servers concurrently (coolify-full's
    // Docker-based Vite container already claims the default 5173), and the
    // gateway's CORS config needs a fixed origin to match, not whatever port
    // happens to be free on a given run.
    port: 5180,
    strictPort: true,
  },
})
