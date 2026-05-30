/*
 * index.ts
 *
 * $Since: 2026-05-26T00:00:00Z
 */

import {initPasswordToggles} from '@src/password-toggle'
import {initLoginForm} from '@src/login-form'
import {initNoteViewer} from '@src/note-viewer'

document.addEventListener('DOMContentLoaded', () => {
  initPasswordToggles()
  initLoginForm()
  initNoteViewer()
})
