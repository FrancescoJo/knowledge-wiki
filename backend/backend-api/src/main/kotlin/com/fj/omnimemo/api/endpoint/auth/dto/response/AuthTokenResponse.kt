/*
 * AuthTokenResponse.kt
 *
 * $Since: 2026-05-29T00:00:00Z
 */
package com.fj.omnimemo.api.endpoint.auth.dto.response

/**
 * Response body returned by login and token-refresh endpoints.
 *
 * Tokens are also set as httpOnly cookies for browser clients. Native clients
 * that cannot rely on automatic cookie management should read them from this body.
 *
 * @author Francesco Jo
 * @since 0.1.1
 * @version 0.1.1
 */
data class AuthTokenResponse(
    val accessToken: String,
    val refreshToken: String,
)
