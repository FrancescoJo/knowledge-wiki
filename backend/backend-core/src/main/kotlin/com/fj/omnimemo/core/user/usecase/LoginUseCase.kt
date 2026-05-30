/*
 * LoginUseCase.kt
 *
 * $Since: 2026-05-25T00:00:00Z
 */
package com.fj.omnimemo.core.user.usecase

import com.fj.omnimemo.core.security.TokenIssuer
import com.fj.omnimemo.core.user.exception.PasswordMismatchException
import com.fj.omnimemo.core.user.exception.RefreshTokenNotFoundException
import com.fj.omnimemo.core.user.exception.TokenExpiredException
import com.fj.omnimemo.core.user.model.LoginResult
import com.fj.omnimemo.core.user.model.RefreshToken
import com.fj.omnimemo.core.user.model.UserId
import com.fj.omnimemo.core.user.repository.RefreshTokenRepository
import com.fj.omnimemo.core.user.repository.UserRepository
import com.fj.omnimemo.core.user.security.PasswordHasher
import com.github.f4b6a3.uuid.UuidCreator
import java.time.Duration
import java.time.Instant

/**
 * Authenticates users and manages session tokens.
 *
 * [login] throws [PasswordMismatchException] for any authentication failure —
 * both "unknown email" and "wrong password" produce the same exception to prevent
 * user enumeration. [refresh] throws [RefreshTokenNotFoundException] for an unknown
 * token and [TokenExpiredException] for an expired one; the expired token is deleted
 * before the exception is thrown to prevent replay.
 *
 * @author Francesco Jo
 * @since 0.1.1
 * @version 0.1.1
 */
class LoginUseCase(
    private val repository: UserRepository,
    private val hasher: PasswordHasher,
    private val tokenIssuer: TokenIssuer,
    private val refreshTokenRepository: RefreshTokenRepository,
    private val refreshTokenTtl: Duration,
) {

    fun login(email: String, rawPassword: String): LoginResult {
        val user = repository.findByEmail(email) ?: throw PasswordMismatchException()
        if (!hasher.matches(rawPassword, user.passwordHash)) throw PasswordMismatchException()
        return LoginResult.create(
            accessToken = tokenIssuer.issue(user.id.value.toString()),
            refreshToken = issueRefreshToken(user.id).token,
        )
    }

    fun refresh(refreshToken: String): LoginResult {
        val existing = refreshTokenRepository.findByToken(refreshToken)
            ?: throw RefreshTokenNotFoundException()
        refreshTokenRepository.delete(existing.token)
        if (Instant.now().isAfter(existing.expiresAt)) throw TokenExpiredException()
        return LoginResult.create(
            accessToken = tokenIssuer.issue(existing.userId.value.toString()),
            refreshToken = issueRefreshToken(existing.userId).token,
        )
    }

    fun logout(refreshToken: String) {
        refreshTokenRepository.delete(refreshToken)
    }

    private fun issueRefreshToken(userId: UserId): RefreshToken =
        refreshTokenRepository.save(
            RefreshToken.create(
                token = UuidCreator.getTimeOrderedEpoch().toString(),
                userId = userId,
                expiresAt = Instant.now().plus(refreshTokenTtl),
                createdAt = Instant.now(),
            )
        )
}
