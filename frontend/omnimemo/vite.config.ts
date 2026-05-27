/**
 * vite.config.ts
 *
 * $Since: 2026-05-26
 */

import { defineConfig } from 'vite'
import { resolve } from 'path'

export default defineConfig({
  root: 'dev',
  resolve: {
    alias: {
      '@src': resolve(__dirname, 'src'),
    },
  },
})
