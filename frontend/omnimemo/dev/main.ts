/**
 * main.ts
 *
 * Development entry point. Initialises password toggle and login form
 * behaviours, then auto-opens the login dialog for visual development.
 *
 * $Since: 2026-05-27
 */

import {initPasswordToggles} from '@src/password-toggle'
import {initLoginForm} from '@src/login-form'

document.addEventListener('DOMContentLoaded', () => {
  initPasswordToggles()
  initLoginForm()

  const dialog = document.getElementById('login-popup') as HTMLDialogElement | null
  dialog?.showModal()
})
