/*
 * login-form.ts
 *
 * $Since: 2026-05-26T00:00:00Z
 */

import {getCsrfToken} from '@src/csrf'

type CsrfProvider = () => string | null
type NavigateFn = (url: string) => void
type CredentialStoreFn = (form: HTMLFormElement) => Promise<void>

function defaultCredentialStore(form: HTMLFormElement): Promise<void> {
  if (!('credentials' in navigator)) return Promise.resolve()
  const Ctor = (window as unknown as Record<string, unknown>)['PasswordCredential'] as
    (new(form: HTMLFormElement) => Credential) | undefined
  if (!Ctor) return Promise.resolve()
  try {
    return navigator.credentials.store(new Ctor(form)).then(() => {
    }, () => {
    })
  } catch (_) {
    return Promise.resolve()
  }
}

/**
 * Attaches event listeners to root that handle:
 * - Enter in the email field: advances focus to the password field
 * - Enter in the password field: explicitly clicks the submit button so the
 *   keyboard path is identical to the mouse path through HTMX
 * - CSRF token injection for all HTMX requests
 * - Login success: saves credentials to the browser password manager then navigates
 * - Login error: displays a user-friendly message in #login-error
 *
 * @param root            The element that receives the delegated listeners.
 * @param csrfProvider    Returns the current CSRF token (injectable for testing).
 * @param navigate        Performs a full page navigation (injectable for testing).
 * @param credentialStore Saves credentials to the browser password manager (injectable for testing).
 * @since 0.1.0
 * @version 0.1.4
 */
export function initLoginForm(
  root: EventTarget = document.body,
  csrfProvider: CsrfProvider = getCsrfToken,
  navigate: NavigateFn = (url) => {
    window.location.href = url
  },
  credentialStore: CredentialStoreFn = defaultCredentialStore,
): void {
  root.addEventListener('keydown', (evt: Event) => {
    const event = evt as KeyboardEvent
    if (event.key !== 'Enter') return
    const target = event.target as HTMLElement

    if (target.id === 'login-email') {
      event.preventDefault()
      ;(document.getElementById('login-password') as HTMLInputElement | null)?.focus()
      return
    }

    if (target.id === 'login-password') {
      event.preventDefault()
      document.querySelector<HTMLButtonElement>('#login-form button[type="submit"]')?.click()
    }
  })

  root.addEventListener('htmx:configRequest', (evt: Event) => {
    const token = csrfProvider()
    if (token) (evt as CustomEvent).detail.headers['X-XSRF-TOKEN'] = token
  })

  root.addEventListener('htmx:afterRequest', (evt: Event) => {
    const event = evt as CustomEvent
    if ((event.detail.elt as HTMLElement).id !== 'login-form') return
    if (!event.detail.successful) return
    const form = event.detail.elt as HTMLFormElement
    credentialStore(form).then(() => navigate('/'))
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
