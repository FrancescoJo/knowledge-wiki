/*
 * RefreshTokenData.kt
 *
 * $Since: 2026-05-25T00:00:00Z
 */
package com.fj.omnimemo.core.user.model

import java.time.Instant

/**
 * Immutable snapshot of a [RefreshToken].
 *
 * @author Francesco Jo
 * @since 0.1.1
 * @version 0.1.1
 */
internal data class RefreshTokenData(
    override val token: String,
    override val userId: UserId,
    override val expiresAt: Instant,
    override val createdAt: Instant,
) : RefreshToken
