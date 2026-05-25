/*
 * LoginRequest.kt
 *
 * $Since: 2026-05-25T00:00:00Z
 */
package com.fj.omnimemo.api.endpoint.auth

/**
 * Request body for POST /api/auth/login.
 *
 * @author Francesco Jo
 * @since 0.1.1
 * @version 0.1.1
 */
data class LoginRequest(val email: String, val password: String)
