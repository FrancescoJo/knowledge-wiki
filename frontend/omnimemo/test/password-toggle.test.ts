/**
 * password-toggle.test.ts
 *
 * $Since: 2026-05-26
 */

import { describe, it, expect, beforeEach, afterEach } from 'vitest'
import { togglePasswordVisibility, initPasswordToggles } from '@src/password-toggle'

describe('togglePasswordVisibility:', () => {
    let input: HTMLInputElement
    let btn: HTMLButtonElement

    beforeEach(() => {
        input = document.createElement('input')
        input.id = 'test-pass'
        input.type = 'password'
        document.body.appendChild(input)

        btn = document.createElement('button')
        btn.textContent = 'Show'
        btn.setAttribute('aria-label', 'Show password')
        document.body.appendChild(btn)
    })

    afterEach(() => {
        document.body.removeChild(input)
        document.body.removeChild(btn)
    })

    describe('on first call (password → text):', () => {
        it('should change the input type to text', () => {
            togglePasswordVisibility('test-pass', btn)
            expect(input.type).toBe('text')
        })

        it('should set the button label to Hide', () => {
            togglePasswordVisibility('test-pass', btn)
            expect(btn.textContent).toBe('Hide')
        })

        it('should update aria-label to Hide password', () => {
            togglePasswordVisibility('test-pass', btn)
            expect(btn.getAttribute('aria-label')).toBe('Hide password')
        })
    })

    describe('on second call (text → password):', () => {
        it('should restore the input type to password', () => {
            togglePasswordVisibility('test-pass', btn)
            togglePasswordVisibility('test-pass', btn)
            expect(input.type).toBe('password')
        })

        it('should restore the button label to Show', () => {
            togglePasswordVisibility('test-pass', btn)
            togglePasswordVisibility('test-pass', btn)
            expect(btn.textContent).toBe('Show')
        })

        it('should restore aria-label to Show password', () => {
            togglePasswordVisibility('test-pass', btn)
            togglePasswordVisibility('test-pass', btn)
            expect(btn.getAttribute('aria-label')).toBe('Show password')
        })
    })

    it('should do nothing when the target input id does not exist', () => {
        togglePasswordVisibility('nonexistent-id', btn)
        expect(btn.textContent).toBe('Show')
    })
})

describe('initPasswordToggles:', () => {
    let container: HTMLDivElement
    let input: HTMLInputElement
    let toggleBtn: HTMLButtonElement

    beforeEach(() => {
        container = document.createElement('div')

        input = document.createElement('input')
        input.id = 'pw-field'
        input.type = 'password'
        container.appendChild(input)

        toggleBtn = document.createElement('button')
        toggleBtn.className = 'login-password-toggle'
        toggleBtn.dataset['target'] = 'pw-field'
        toggleBtn.textContent = 'Show'
        toggleBtn.setAttribute('aria-label', 'Show password')
        container.appendChild(toggleBtn)

        document.body.appendChild(container)
        initPasswordToggles(container)
    })

    afterEach(() => {
        document.body.removeChild(container)
    })

    it('should toggle the password input type when the toggle button is clicked', () => {
        toggleBtn.click()
        expect(input.type).toBe('text')
    })

    it('should not change unrelated inputs when a non-toggle element is clicked', () => {
        const other = document.createElement('button')
        container.appendChild(other)
        other.click()
        expect(input.type).toBe('password')
        container.removeChild(other)
    })
})
