/*
 * LoginRequest.kt
 *
 * $Since: 2026-05-26T00:00:00Z
 */
package com.fj.omnimemo.api.endpoint.auth.dto.request

/**
 * Request body for POST [com.fj.omnimemo.api.endpoint.ApiPathsV1.AUTH]/login.
 *
 * @author Francesco Jo
 * @since 0.1.1
 * @version 0.1.1
 */
data class LoginRequest(val email: String, val password: String)
