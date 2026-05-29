/**
 * login-form.test.ts
 *
 * $Since: 2026-05-26
 */

import { describe, it, expect, beforeEach, afterEach, vi } from 'vitest'
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

    describe('keydown Enter on email field:', () => {
        let emailInput: HTMLInputElement
        let passwordInput: HTMLInputElement

        beforeEach(() => {
            emailInput = document.createElement('input')
            emailInput.id = 'login-email'
            emailInput.type = 'email'
            passwordInput = document.createElement('input')
            passwordInput.id = 'login-password'
            passwordInput.type = 'password'
            root.appendChild(emailInput)
            root.appendChild(passwordInput)
            initLoginForm(root, () => null)
        })

        it('should move focus to the password field', () => {
            emailInput.dispatchEvent(new KeyboardEvent('keydown', { key: 'Enter', bubbles: true }))
            expect(document.activeElement).toBe(passwordInput)
        })

        it('should suppress the default action', () => {
            const event = new KeyboardEvent('keydown', { key: 'Enter', bubbles: true, cancelable: true })
            emailInput.dispatchEvent(event)
            expect(event.defaultPrevented).toBe(true)
        })

        it('should not intercept Enter on other elements', () => {
            const otherInput = document.createElement('input')
            otherInput.id = 'other-input'
            root.appendChild(otherInput)
            const event = new KeyboardEvent('keydown', { key: 'Enter', bubbles: true, cancelable: true })
            otherInput.dispatchEvent(event)
            expect(event.defaultPrevented).toBe(false)
            root.removeChild(otherInput)
        })
    })

    describe('keydown Enter on password field:', () => {
        let loginForm: HTMLFormElement
        let passwordInput: HTMLInputElement
        let submitBtn: HTMLButtonElement

        beforeEach(() => {
            loginForm = document.createElement('form')
            loginForm.id = 'login-form'
            passwordInput = document.createElement('input')
            passwordInput.id = 'login-password'
            passwordInput.type = 'password'
            submitBtn = document.createElement('button')
            submitBtn.type = 'submit'
            loginForm.appendChild(passwordInput)
            loginForm.appendChild(submitBtn)
            root.appendChild(loginForm)
            initLoginForm(root, () => null)
        })

        it('should click the submit button', () => {
            const clickSpy = vi.spyOn(submitBtn, 'click').mockImplementation(() => {})
            passwordInput.dispatchEvent(new KeyboardEvent('keydown', { key: 'Enter', bubbles: true }))
            expect(clickSpy).toHaveBeenCalledOnce()
        })

        it('should suppress the default action', () => {
            vi.spyOn(submitBtn, 'click').mockImplementation(() => {})
            const event = new KeyboardEvent('keydown', { key: 'Enter', bubbles: true, cancelable: true })
            passwordInput.dispatchEvent(event)
            expect(event.defaultPrevented).toBe(true)
        })

        it('should not intercept non-Enter keys', () => {
            const clickSpy = vi.spyOn(submitBtn, 'click').mockImplementation(() => {})
            const event = new KeyboardEvent('keydown', { key: 'Tab', bubbles: true, cancelable: true })
            passwordInput.dispatchEvent(event)
            expect(clickSpy).not.toHaveBeenCalled()
            expect(event.defaultPrevented).toBe(false)
        })
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

    describe('htmx:afterRequest handler:', () => {
        let loginForm: HTMLFormElement

        beforeEach(() => {
            loginForm = document.createElement('form')
            loginForm.id = 'login-form'
            const emailInput = document.createElement('input')
            emailInput.setAttribute('autocomplete', 'username')
            emailInput.value = 'alice@example.com'
            const passwordInput = document.createElement('input')
            passwordInput.setAttribute('autocomplete', 'current-password')
            passwordInput.value = 'secret'
            loginForm.appendChild(emailInput)
            loginForm.appendChild(passwordInput)
            root.appendChild(loginForm)
        })

        it('should store credentials with the form element and navigate to / on success', async () => {
            const navigate = vi.fn()
            const credentialStore = vi.fn(() => Promise.resolve())
            initLoginForm(root, () => null, navigate, credentialStore)

            root.dispatchEvent(new CustomEvent('htmx:afterRequest', {
                detail: { elt: loginForm, successful: true },
            }))

            expect(credentialStore).toHaveBeenCalledWith(loginForm)
            await Promise.resolve()
            expect(navigate).toHaveBeenCalledWith('/')
        })

        it('should not store credentials or navigate when the request fails', () => {
            const navigate = vi.fn()
            const credentialStore = vi.fn(() => Promise.resolve())
            initLoginForm(root, () => null, navigate, credentialStore)

            root.dispatchEvent(new CustomEvent('htmx:afterRequest', {
                detail: { elt: loginForm, successful: false },
            }))

            expect(credentialStore).not.toHaveBeenCalled()
            expect(navigate).not.toHaveBeenCalled()
        })

        it('should not act on successful requests from other elements', () => {
            const navigate = vi.fn()
            const credentialStore = vi.fn(() => Promise.resolve())
            const otherForm = document.createElement('form')
            otherForm.id = 'other-form'
            root.appendChild(otherForm)
            initLoginForm(root, () => null, navigate, credentialStore)

            root.dispatchEvent(new CustomEvent('htmx:afterRequest', {
                detail: { elt: otherForm, successful: true },
            }))

            expect(credentialStore).not.toHaveBeenCalled()
            expect(navigate).not.toHaveBeenCalled()
            root.removeChild(otherForm)
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
