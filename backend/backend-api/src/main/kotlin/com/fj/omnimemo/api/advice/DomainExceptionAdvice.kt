/*
 * DomainExceptionAdvice.kt
 *
 * $Since: 2026-05-26T00:00:00Z
 */
package com.fj.omnimemo.api.advice

import com.fj.omnimemo.core.user.exception.PasswordMismatchException
import com.fj.omnimemo.core.user.exception.RedundantBootstrapProhibitedException
import com.fj.omnimemo.core.user.exception.RefreshTokenNotFoundException
import com.fj.omnimemo.core.user.exception.TokenExpiredException
import com.fj.omnimemo.core.user.exception.UserNotFoundException
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice

/**
 * Translates domain exceptions into HTTP responses.
 *
 * The core module has no knowledge of HTTP; this advice is the single place
 * where that translation happens. Every [com.fj.omnimemo.core.exception.OmniMemoException]
 * subtype must have a handler here so that domain failures always produce a
 * well-defined HTTP status code.
 *
 * @author Francesco Jo
 * @since 0.1.1
 * @version 0.1.1
 */
@RestControllerAdvice
class DomainExceptionAdvice {

    @ExceptionHandler(UserNotFoundException::class)
    fun handleUserNotFound(): ResponseEntity<Unit> =
        ResponseEntity.status(HttpStatus.NOT_FOUND).build()

    @ExceptionHandler(
        PasswordMismatchException::class,
        RefreshTokenNotFoundException::class,
        TokenExpiredException::class,
    )
    fun handleUnauthorised(): ResponseEntity<Unit> =
        ResponseEntity.status(HttpStatus.UNAUTHORIZED).build()

    @ExceptionHandler(RedundantBootstrapProhibitedException::class)
    fun handleRedundantBootstrap(): ResponseEntity<Unit> =
        ResponseEntity.status(HttpStatus.CONFLICT).build()
}
