import { defineConfig, loadEnv } from 'vite'
import path from 'path'
import createVitePlugins from './vite/plugins'

export default defineConfig(({ mode, command }) => {
  const env = loadEnv(mode, process.cwd())
  return {
    base: '/jd-customers-app/',
    plugins: createVitePlugins(env, command === 'build'),
    resolve: { alias: [
      { find: '@/store/modules/user', replacement: path.resolve('src/freight-user.js') },
      { find: '@', replacement: path.resolve('src') },
      { find: '~', replacement: path.resolve('.') }
    ] },
    build: {
      outDir: '../.codex-deploy/jd-customers-ui',
      emptyOutDir: true,
      rollupOptions: { input: path.resolve('jd-customers.html') },
      chunkSizeWarningLimit: 2000
    }
  }
})
