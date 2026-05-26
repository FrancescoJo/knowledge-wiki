/**
 * login-form.test.ts
 *
 * $Since: 2026-05-26
 */

import { describe, it, expect, beforeEach, afterEach } from 'vitest'
import { initLoginForm } from '@src/login-form'

describe('initLoginForm:', () => {
    let root: HTMLDivElement

    beforeEach(() => {
        root = document.createElement('div')
        document.body.appendChild(root)
    })

    afterEach(() => {
        document.body.removeChild(root)
    })

    describe('htmx:configRequest handler:', () => {
        it('should inject X-XSRF-TOKEN header when a token is provided', () => {
            initLoginForm(root, () => 'test-csrf-token')
            const headers: Record<string, string> = {}
            root.dispatchEvent(new CustomEvent('htmx:configRequest', { detail: { headers } }))
            expect(headers['X-XSRF-TOKEN']).toBe('test-csrf-token')
        })

        it('should not inject X-XSRF-TOKEN header when token is null', () => {
            initLoginForm(root, () => null)
            const headers: Record<string, string> = {}
            root.dispatchEvent(new CustomEvent('htmx:configRequest', { detail: { headers } }))
            expect(headers['X-XSRF-TOKEN']).toBeUndefined()
        })
    })

    describe('htmx:responseError handler:', () => {
        let loginForm: HTMLFormElement
        let errorEl: HTMLParagraphElement

        beforeEach(() => {
            loginForm = document.createElement('form')
            loginForm.id = 'login-form'
            errorEl = document.createElement('p')
            errorEl.id = 'login-error'
            root.appendChild(loginForm)
            root.appendChild(errorEl)
            initLoginForm(root, () => null)
        })

        it('should display an invalid credentials message for a 401 response', () => {
            root.dispatchEvent(new CustomEvent('htmx:responseError', {
                detail: { elt: loginForm, xhr: { status: 401 } },
            }))
            expect(errorEl.textContent).toBe('Invalid email or password.')
        })

        it('should display a generic error message for non-401 errors', () => {
            root.dispatchEvent(new CustomEvent('htmx:responseError', {
                detail: { elt: loginForm, xhr: { status: 500 } },
            }))
            expect(errorEl.textContent).toBe('Login failed. Please try again.')
        })

        it('should not modify the error element for errors from other forms', () => {
            const otherForm = document.createElement('form')
            otherForm.id = 'other-form'
            root.appendChild(otherForm)
            root.dispatchEvent(new CustomEvent('htmx:responseError', {
                detail: { elt: otherForm, xhr: { status: 401 } },
            }))
            expect(errorEl.textContent).toBe('')
            root.removeChild(otherForm)
        })
    })
})
