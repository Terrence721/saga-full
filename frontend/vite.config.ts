import react from '@vitejs/plugin-react'
import tailwindcss from '@tailwindcss/vite'
// Imported from 'vitest/config', not 'vite' - it re-exports Vite's own defineConfig
// with the type extended to include the `test` field below, so a plain `vite` build
// still just ignores it.
import { defineConfig } from 'vitest/config'

// https://vite.dev/config/
export default defineConfig({
  plugins: [react(), tailwindcss()],
  server: {
    // Pinned rather than left to Vite's automatic fallback - this machine runs
    // several portfolio projects' dev servers concurrently (coolify-full's
    // Docker-based Vite container already claims the default 5173), and the
    // gateway's CORS config needs a fixed origin to match, not whatever port
    // happens to be free on a given run.
    port: 5180,
    strictPort: true,
  },
  test: {
    environment: 'jsdom',
    setupFiles: './src/test/setup.ts',
  },
})
