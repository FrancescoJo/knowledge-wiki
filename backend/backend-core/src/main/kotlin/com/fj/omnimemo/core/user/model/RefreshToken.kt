/*
 * RefreshToken.kt
 *
 * $Since: 2026-05-25T00:00:00Z
 */
package com.fj.omnimemo.core.user.model

import com.fj.omnimemo.core.user.model.snapshot.RefreshTokenData
import java.time.Instant

/**
 * Represents a stateful refresh token associated with a [User].
 *
 * [token] is an opaque random UUID string used as the primary key.
 * Expiry is checked by the domain; persistence is the infrastructure's responsibility.
 *
 * Use [create] to instantiate.
 *
 * @author Francesco Jo
 * @since 0.1.1
 * @version 0.1.1
 */
interface RefreshToken {
    val token: String
    val userId: UserId
    val expiresAt: Instant
    val createdAt: Instant

    companion object {
        fun create(
            token: String,
            userId: UserId,
            expiresAt: Instant,
            createdAt: Instant,
        ): RefreshToken = RefreshTokenData(token, userId, expiresAt, createdAt)
    }
}
