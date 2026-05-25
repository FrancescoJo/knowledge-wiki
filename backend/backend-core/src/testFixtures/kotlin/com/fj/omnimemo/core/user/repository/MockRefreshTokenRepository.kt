/*
 * MockRefreshTokenRepository.kt
 *
 * $Since: 2026-05-25T00:00:00Z
 */
package com.fj.omnimemo.core.user.repository

import com.fj.omnimemo.core.user.model.RefreshToken

/**
 * In-memory fake of [RefreshTokenRepository] for use in Small tests.
 *
 * @author Francesco Jo
 * @since 0.1.1
 * @version 0.1.1
 */
class MockRefreshTokenRepository : RefreshTokenRepository {

    private val store = mutableMapOf<String, RefreshToken>()

    override fun save(refreshToken: RefreshToken): RefreshToken =
        refreshToken.also { store[refreshToken.token] = refreshToken }

    override fun findByToken(token: String): RefreshToken? = store[token]

    override fun delete(token: String) { store.remove(token) }

    fun clear() = store.clear()
}
