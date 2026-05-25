/*
 * LoginUseCase.kt
 *
 * $Since: 2026-05-25T00:00:00Z
 */
package com.fj.omnimemo.core.usecase.user

import com.fj.omnimemo.core.model.user.LoginResult
import com.fj.omnimemo.core.model.user.PasswordHasher
import com.fj.omnimemo.core.model.user.RefreshToken
import com.fj.omnimemo.core.model.user.RefreshTokenRepository
import com.fj.omnimemo.core.model.user.TokenIssuer
import com.fj.omnimemo.core.model.user.UserId
import com.fj.omnimemo.core.model.user.UserRepository
import java.time.Duration
import java.time.Instant
import java.util.UUID

/**
 * Authenticates users and manages session tokens.
 *
 * Both [login] and [refresh] return [null] on any failure to prevent user
 * enumeration. Each successful [refresh] rotates the refresh token: the
 * consumed token is deleted and a new one is issued.
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

    fun login(email: String, rawPassword: String): LoginResult? {
        val user = repository.findByEmail(email) ?: return null
        if (!hasher.matches(rawPassword, user.passwordHash)) return null
        return LoginResult.create(
            accessToken = tokenIssuer.issue(user.id.value.toString()),
            refreshToken = issueRefreshToken(user.id).token,
        )
    }

    fun refresh(refreshToken: String): LoginResult? {
        val existing = refreshTokenRepository.findByToken(refreshToken) ?: return null
        refreshTokenRepository.delete(existing.token)
        if (Instant.now().isAfter(existing.expiresAt)) return null
        return LoginResult.create(
            accessToken = tokenIssuer.issue(existing.userId.value.toString()),
            refreshToken = issueRefreshToken(existing.userId).token,
        )
    }

    private fun issueRefreshToken(userId: UserId): RefreshToken =
        refreshTokenRepository.save(
            RefreshToken.create(
                token = UUID.randomUUID().toString(),
                userId = userId,
                expiresAt = Instant.now().plus(refreshTokenTtl),
                createdAt = Instant.now(),
            )
        )
}
