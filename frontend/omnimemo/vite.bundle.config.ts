import { defineConfig } from 'vite'
import { resolve } from 'path'

export default defineConfig({
  resolve: {
    alias: {
      '@src': resolve(__dirname, 'src'),
    },
  },
  build: {
    lib: {
      entry: resolve(__dirname, 'src/index.ts'),
      name: 'Omnimemo',
      fileName: () => 'omnimemo.js',
      formats: ['iife'],
    },
    outDir: resolve(__dirname, 'dist-bundle'),
    emptyOutDir: true,
    minify: 'esbuild',
  },
})
