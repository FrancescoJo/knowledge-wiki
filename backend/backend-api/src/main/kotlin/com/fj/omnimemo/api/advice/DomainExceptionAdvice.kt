/*
 * DomainExceptionAdvice.kt
 *
 * $Since: 2026-05-26T00:00:00Z
 */
package com.fj.omnimemo.api.advice

import com.fj.omnimemo.api.response.ErrorResponse
import com.fj.omnimemo.core.exception.OmniMemoErrorCode
import com.fj.omnimemo.core.exception.OmniMemoException
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.server.ResponseStatusException
import kotlin.reflect.jvm.jvmName

/**
 * Translates domain exceptions into HTTP responses.
 *
 * The HTTP status and error code for every [OmniMemoException] are derived from
 * [OmniMemoErrorCode], keeping the mapping in a single, auditable place.
 * [ResponseStatusException] thrown at the controller layer (e.g. missing auth
 * cookie, malformed ID) is handled separately with a generic error code.
 *
 * All responses are subsequently wrapped in a [com.fj.omnimemo.api.response.ResponseEnvelope]
 * by [ResponseEnvelopeAdvice].
 *
 * @author Francesco Jo
 * @since 0.1.1
 * @version 0.1.1
 */
@RestControllerAdvice
class DomainExceptionAdvice {

    @ExceptionHandler(OmniMemoException::class)
    fun handleDomainException(exception: OmniMemoException): ResponseEntity<ErrorResponse> {
        val errorCode = OmniMemoErrorCode.of(exception)
        return ResponseEntity
            .status(errorCode.toHttpStatus())
            .body(
                ErrorResponse(
                    code = errorCode.code,
                    message = exception.message ?: errorCode.name,
                    details = null,
                )
            )
    }

    @ExceptionHandler(ResponseStatusException::class)
    fun handleResponseStatus(exception: ResponseStatusException): ResponseEntity<ErrorResponse> =
        ResponseEntity
            .status(exception.statusCode)
            .body(
                ErrorResponse(
                    code = exception::class.simpleName ?: exception::class.jvmName,
                    message = exception.reason ?: exception.message,
                    details = null,
                )
            )

    private fun OmniMemoErrorCode.toHttpStatus(): HttpStatus = when (this) {
        OmniMemoErrorCode.PASSWORD_MISMATCH              -> HttpStatus.UNAUTHORIZED
        OmniMemoErrorCode.REFRESH_TOKEN_NOT_FOUND        -> HttpStatus.UNAUTHORIZED
        OmniMemoErrorCode.TOKEN_EXPIRED                  -> HttpStatus.UNAUTHORIZED
        OmniMemoErrorCode.USER_NOT_FOUND                 -> HttpStatus.NOT_FOUND
        OmniMemoErrorCode.REDUNDANT_BOOTSTRAP_PROHIBITED -> HttpStatus.CONFLICT
        OmniMemoErrorCode.NOTE_NOT_FOUND                 -> HttpStatus.NOT_FOUND
        OmniMemoErrorCode.DUPLICATE_NOTE_TITLE           -> HttpStatus.CONFLICT
        OmniMemoErrorCode.NOTE_ACCESS_DENIED             -> HttpStatus.FORBIDDEN
        OmniMemoErrorCode.STALE_NOTE_VERSION             -> HttpStatus.CONFLICT
        OmniMemoErrorCode.NOTE_ALREADY_DELETED           -> HttpStatus.CONFLICT
        OmniMemoErrorCode.NOTE_ROLLBACK_NOT_PERMITTED    -> HttpStatus.FORBIDDEN
    }
}
