/*
 * UserData.kt
 *
 * $Since: 2026-05-23T00:00:00Z
 */
package com.fj.omnimemo.core.user.model.snapshot

import com.fj.omnimemo.core.user.model.User
import com.fj.omnimemo.core.user.model.UserId
import java.time.Instant

/**
 * Immutable snapshot of a [User]. Used for all read paths.
 *
 * @author Francesco Jo
 * @since 0.1.1
 * @version 0.1.1
 */
internal data class UserData(
    override val id: UserId,
    override val isNew: Boolean,
    override val email: String,
    override val passwordHash: String,
    override val createdAt: Instant,
    override val updatedAt: Instant,
) : User
