/*
 * ResponseEnvelope.kt
 *
 * $Since: 2026-05-28T00:00:00Z
 */
package com.fj.omnimemo.api.response

/**
 * Top-level wrapper applied to every REST API response.
 *
 * @param T the payload type; null for void responses and error bodies use [ErrorResponse]
 */
data class ResponseEnvelope<T>(
    val type: Type,
    val body: T?,
    val timestamp: String,
) {
    enum class Type { OK, ERR }
}
