import {defineConfig} from 'vite'
import {resolve} from 'path'
import {readFileSync} from 'fs'

const pkg = JSON.parse(readFileSync(resolve(__dirname, 'package.json'), 'utf-8'))
const versionTag = `v${(pkg.version as string).replace(/\./g, '_')}`

export default defineConfig({
  build: {
    lib: {
      entry: resolve(__dirname, 'src/index.ts'),
      name: 'TextEdit',
      fileName: () => `textedit-${versionTag}.js`,
      formats: ['iife'],
    },
    outDir: resolve(__dirname, 'dist-bundle'),
    emptyOutDir: true,
    minify: 'esbuild',
  },
})
