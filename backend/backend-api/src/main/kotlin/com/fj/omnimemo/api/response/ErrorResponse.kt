/*
 * ErrorResponse.kt
 *
 * $Since: 2026-05-28T00:00:00Z
 */
package com.fj.omnimemo.api.response

/**
 * Payload placed in [ResponseEnvelope.body] when [ResponseEnvelope.type] is [ResponseType.ERR].
 *
 * @param code     Hex error code independent of the HTTP status code (e.g. "0x1A2B3C4D").
 *                 For domain exceptions this is derived from [com.fj.omnimemo.core.exception.OmniMemoErrorCode].
 * @param message  Human-readable description of the error.
 * @param details  Optional structured elaboration; null when no additional context is needed.
 */
data class ErrorResponse(
    val code: String,
    val message: String,
    val details: Any?,
)
