/*
 * RefreshTokenRepository.kt
 *
 * $Since: 2026-05-25T00:00:00Z
 */
package com.fj.omnimemo.core.model.user

/**
 * Persistence contract for [RefreshToken] entities.
 *
 * @author Francesco Jo
 * @since 0.1.1
 * @version 0.1.1
 */
interface RefreshTokenRepository {
    fun save(refreshToken: RefreshToken): RefreshToken
    fun findByToken(token: String): RefreshToken?
    fun delete(token: String)
}
