import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import path from 'node:path'
export default defineConfig(({mode})=>({
  base: mode==='desktop'?'./':'/anchor-console/',
  plugins:[vue()],resolve:{alias:{'@':path.resolve('src')}},
  build:{outDir:mode==='desktop'?'../clients/anchor-metrics-probe/ui':'../.codex-deploy/anchor-console',emptyOutDir:true,chunkSizeWarningLimit:2000,
    rollupOptions:{input:path.resolve(mode==='desktop'?'anchor-desktop.html':'anchor-releases.html')}}
}))
