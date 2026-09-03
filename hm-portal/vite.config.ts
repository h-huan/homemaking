import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

export default defineConfig({
  plugins: [vue()],
  server: { proxy: { '/app-api': { target: 'http://127.0.0.1:48080', changeOrigin: false } } },
  build: { outDir: 'dist', sourcemap: false }
})
