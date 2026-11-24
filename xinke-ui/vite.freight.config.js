import { defineConfig, loadEnv } from 'vite'
import path from 'path'
import createVitePlugins from './vite/plugins'

export default defineConfig(({ mode, command }) => {
  const env = loadEnv(mode, process.cwd())
  return {
    base: '/freight-app/',
    plugins: createVitePlugins(env, command === 'build'),
    resolve: { alias: [
      { find: '@/store/modules/user', replacement: path.resolve('src/freight-user.js') },
      { find: '@', replacement: path.resolve('src') },
      { find: '~', replacement: path.resolve('.') }
    ] },
    build: {
      outDir: '../.codex-deploy/freight-ui',
      emptyOutDir: true,
      rollupOptions: { input: path.resolve('freight.html') },
      chunkSizeWarningLimit: 2000
    }
  }
})
