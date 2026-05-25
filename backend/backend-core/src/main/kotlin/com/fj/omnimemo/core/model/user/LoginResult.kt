/*
 * LoginResult.kt
 *
 * $Since: 2026-05-25T00:00:00Z
 */
package com.fj.omnimemo.core.model.user

/**
 * Holds the access and refresh tokens produced by a successful login or token refresh.
 *
 * Use [create] to instantiate.
 *
 * @author Francesco Jo
 * @since 0.1.1
 * @version 0.1.1
 */
interface LoginResult {
    val accessToken: String
    val refreshToken: String

    companion object {
        fun create(accessToken: String, refreshToken: String): LoginResult =
            LoginResultData(accessToken, refreshToken)
    }
}
