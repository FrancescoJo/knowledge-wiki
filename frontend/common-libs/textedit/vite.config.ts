/**
 * vite.config.ts
 *
 * $Since: 2026-05-07
 */

import {defineConfig} from 'vite'
import {resolve} from 'path'

const EXTERNAL_PREFIXES = ['@tiptap/', 'lowlight', 'highlight.js']

export default defineConfig(({command}) => ({
  root: command === 'serve' ? 'dev' : '.',
  build: {
    lib: {
      entry: resolve(__dirname, 'src/index.ts'),
      name: 'TextEdit',
      fileName: 'textedit',
      formats: ['es'],
    },
    outDir: resolve(__dirname, 'dist'),
    emptyOutDir: true,
    minify: 'esbuild',
    rollupOptions: {
      external: (id: string) => EXTERNAL_PREFIXES.some(prefix => id.startsWith(prefix)),
    },
  },
}))
