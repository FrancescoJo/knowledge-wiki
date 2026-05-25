/*
 * UpdatePasswordRequest.kt
 *
 * $Since: 2026-05-25T00:00:00Z
 */
package com.fj.omnimemo.api.endpoint.user

/**
 * Request body for PUT /api/users/{id}/password.
 *
 * @author Francesco Jo
 * @since 0.1.1
 * @version 0.1.1
 */
data class UpdatePasswordRequest(val password: String)
