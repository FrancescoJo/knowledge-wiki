/*
 * csrf.ts
 *
 * $Since: 2026-05-26T00:00:00Z
 */

/**
 * Reads the XSRF-TOKEN value from a cookie string.
 *
 * Accepts an optional cookie string so the function can be tested without
 * relying on a live document.cookie.
 *
 * @param cookieString The raw cookie header string (defaults to document.cookie).
 * @return The decoded token value, or null if the cookie is absent.
 * @since 0.1.0
 * @version 0.1.0
 */
export function getCsrfToken(cookieString: string = document.cookie): string | null {
    const match = cookieString.match(/(?:^|;\s*)XSRF-TOKEN=([^;]+)/)
    return match ? decodeURIComponent(match[1]) : null
}
