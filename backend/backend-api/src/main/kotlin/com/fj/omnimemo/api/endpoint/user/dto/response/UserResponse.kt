/*
 * UserResponse.kt
 *
 * $Since: 2026-05-26T00:00:00Z
 */
package com.fj.omnimemo.api.endpoint.user.dto.response

import com.fj.omnimemo.core.user.model.User

/**
 * API response representation of a [User].
 *
 * [User.passwordHash] is intentionally excluded from this view.
 *
 * @author Francesco Jo
 * @since 0.1.1
 * @version 0.1.1
 */
data class UserResponse(
    val id: String,
    val email: String,
    val createdAt: String,
    val updatedAt: String,
)

internal fun User.toResponse() = UserResponse(
    id = id.value.toString(),
    email = email,
    createdAt = createdAt.toString(),
    updatedAt = updatedAt.toString(),
)
