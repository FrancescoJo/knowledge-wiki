/*
 * login-form.ts
 *
 * $Since: 2026-05-26T00:00:00Z
 */

import { getCsrfToken } from '@src/csrf'

type CsrfProvider = () => string | null

/**
 * Attaches HTMX event listeners to root that handle CSRF token injection
 * and login error display for the login popup form.
 *
 * @param root         The element that receives the delegated HTMX listeners.
 * @param csrfProvider Function that returns the current CSRF token (injectable for testing).
 * @since 0.1.0
 * @version 0.1.0
 */
export function initLoginForm(
    root: EventTarget = document.body,
    csrfProvider: CsrfProvider = getCsrfToken,
): void {
    root.addEventListener('htmx:configRequest', (evt: Event) => {
        const token = csrfProvider()
        if (token) (evt as CustomEvent).detail.headers['X-XSRF-TOKEN'] = token
    })

    root.addEventListener('htmx:responseError', (evt: Event) => {
        const event = evt as CustomEvent
        if ((event.detail.elt as HTMLElement).id !== 'login-form') return
        const errorEl = document.getElementById('login-error')
        if (!errorEl) return
        errorEl.textContent = event.detail.xhr.status === 401
            ? 'Invalid email or password.'
            : 'Login failed. Please try again.'
    })
}
