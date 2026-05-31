/*
 * JwtTokenIssuer.kt
 *
 * $Since: 2026-05-25T00:00:00Z
 */
package com.fj.omnimemo.infrastructure.security

import com.fj.omnimemo.core.security.TokenIssuer
import java.time.Duration
import java.time.Instant

/**
 * [TokenIssuer] implementation that delegates to [JwtTokenService].
 *
 * @author Francesco Jo
 * @since 0.1.1
 * @version 0.1.1
 */
class JwtTokenIssuer(
    private val tokenService: JwtTokenService,
    private val tokenTtl: Duration,
) : TokenIssuer {
    override fun issue(subject: String): String =
        tokenService.issue(subject, Instant.now().plus(tokenTtl))
}
