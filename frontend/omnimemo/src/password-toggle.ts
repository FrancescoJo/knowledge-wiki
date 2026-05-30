/*
 * password-toggle.ts
 *
 * $Since: 2026-05-26T00:00:00Z
 */

/**
 * Toggles a password input between 'password' and 'text' types and updates
 * the accompanying toggle button label.
 *
 * @param inputId The id of the password input element.
 * @param button  The toggle button that triggered the action.
 * @since 0.1.0
 * @version 0.1.0
 */
export function togglePasswordVisibility(inputId: string, button: HTMLButtonElement): void {
  const input = document.getElementById(inputId) as HTMLInputElement | null
  if (!input) return
  const isPassword = input.type === 'password'
  input.type = isPassword ? 'text' : 'password'
  button.textContent = isPassword ? 'Hide' : 'Show'
  button.setAttribute('aria-label', isPassword ? 'Hide password' : 'Show password')
}

/**
 * Attaches a delegated click handler to root that invokes
 * togglePasswordVisibility for any element matching .login-password-toggle.
 *
 * @param root The element (or document) that receives the delegated listener.
 * @since 0.1.0
 * @version 0.1.0
 */
export function initPasswordToggles(root: EventTarget = document): void {
  root.addEventListener('click', (e: Event) => {
    const btn = (e.target as Element).closest<HTMLButtonElement>('.login-password-toggle')
    if (!btn) return
    const targetId = btn.dataset['target']
    if (!targetId) return
    togglePasswordVisibility(targetId, btn)
  })
}
