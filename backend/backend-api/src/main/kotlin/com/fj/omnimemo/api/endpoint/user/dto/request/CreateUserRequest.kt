/*
 * CreateUserRequest.kt
 *
 * $Since: 2026-05-26T00:00:00Z
 */
package com.fj.omnimemo.api.endpoint.user.dto.request

/**
 * Request body for POST [com.fj.omnimemo.api.endpoint.ApiPathsV1.USERS].
 *
 * @author Francesco Jo
 * @since 0.1.1
 * @version 0.1.1
 */
data class CreateUserRequest(val email: String, val password: String)
