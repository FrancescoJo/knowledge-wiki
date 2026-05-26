/*
 * CookieSupport.kt
 *
 * $Since: 2026-05-26T00:00:00Z
 */
package com.fj.omnimemo.api.endpoint.test

import org.springframework.http.HttpHeaders
import org.springframework.http.ResponseEntity

/**
 * Utility for extracting cookie attributes from HTTP responses in Large Test specs.
 *
 * Methods are annotated with [@JvmStatic] so that Groovy specifications can call them
 * as `CookieSupport.cookieValue(response, "name")` without referencing the companion object.
 *
 * @author Francesco Jo
 * @since 0.1.1
 * @version 0.1.1
 */
object CookieSupport {

    @JvmStatic
    fun cookieValue(response: ResponseEntity<*>, name: String): String? =
        response.headers[HttpHeaders.SET_COOKIE]
            ?.map { it.split(";").first().trim() }
            ?.find { it.startsWith("$name=") }
            ?.substring("$name=".length)

    @JvmStatic
    fun cookieMaxAge(response: ResponseEntity<*>, name: String): Int {
        val header = response.headers[HttpHeaders.SET_COOKIE]
            ?.find { cookie -> cookie.split(";").first().trim().startsWith("$name=") }
            ?: return -1
        val maxAgePart = header.split(";").find { it.trim().lowercase().startsWith("max-age=") }
        return maxAgePart?.trim()?.substring("max-age=".length)?.toIntOrNull() ?: -1
    }
}
