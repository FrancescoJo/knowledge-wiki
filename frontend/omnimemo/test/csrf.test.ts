/**
 * csrf.test.ts
 *
 * $Since: 2026-05-26
 */

import { describe, it, expect } from 'vitest'
import { getCsrfToken } from '@src/csrf'

describe('getCsrfToken:', () => {
    describe('when XSRF-TOKEN cookie is absent:', () => {
        it('should return null for an empty cookie string', () => {
            expect(getCsrfToken('')).toBeNull()
        })

        it('should return null when only unrelated cookies are present', () => {
            expect(getCsrfToken('session=abc; other=xyz')).toBeNull()
        })
    })

    describe('when XSRF-TOKEN cookie is present:', () => {
        it('should return the token as the only cookie', () => {
            expect(getCsrfToken('XSRF-TOKEN=mytoken')).toBe('mytoken')
        })

        it('should return the token when it appears among multiple cookies', () => {
            expect(getCsrfToken('session=abc; XSRF-TOKEN=tok42; other=xyz')).toBe('tok42')
        })

        it('should return the URL-decoded token value', () => {
            expect(getCsrfToken('XSRF-TOKEN=abc%2F123%3D%3D')).toBe('abc/123==')
        })
    })
})
