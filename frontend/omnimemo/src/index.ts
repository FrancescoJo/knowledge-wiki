/*
 * index.ts
 *
 * $Since: 2026-05-26T00:00:00Z
 */

import { initPasswordToggles } from '@src/password-toggle'
import { initLoginForm } from '@src/login-form'

document.addEventListener('DOMContentLoaded', () => {
    initPasswordToggles()
    initLoginForm()
})
