/*
 * CsrfSupport.kt
 *
 * $Since: 2026-05-26T00:00:00Z
 */
package com.fj.omnimemo.api.endpoint.test

import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import java.util.*

/**
 * Builds HTTP headers that satisfy the double-submit cookie CSRF pattern used by
 * [org.springframework.security.web.csrf.CookieCsrfTokenRepository].
 *
 * The server stores no CSRF state — it checks only that the `X-XSRF-TOKEN` header
 * matches the `XSRF-TOKEN` cookie. A freshly generated UUID sent in both places is
 * therefore always a valid CSRF token pair.
 *
 * @author Francesco Jo
 * @since 0.1.1
 * @version 0.1.1
 */
internal object CsrfSupport {

    fun buildHeaders(withJson: Boolean, cookies: Map<String, String> = emptyMap()): HttpHeaders {
        val token = UUID.randomUUID().toString()
        return HttpHeaders().apply {
            if (withJson) contentType = MediaType.APPLICATION_JSON
            add("X-XSRF-TOKEN", token)
            add(
                HttpHeaders.COOKIE,
                (mapOf("XSRF-TOKEN" to token) + cookies)
                    .entries.joinToString("; ") { "${it.key}=${it.value}" },
            )
        }
    }
}
