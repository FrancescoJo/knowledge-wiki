/*
 * UserProfile.kt
 *
 * $Since: 2026-05-30T00:00:00Z
 */
package com.fj.omnimemo.core.user.model

/**
 * A read-only projection of a [User] containing only the fields needed for display.
 *
 * Decoupled from [User] intentionally: callers that only need display data should
 * not receive the full entity (which includes the password hash).
 *
 * @author Francesco Jo
 * @since 0.1.1
 * @version 0.1.1
 */
data class UserProfile(
    val id: UserId,
    val email: String,
)
