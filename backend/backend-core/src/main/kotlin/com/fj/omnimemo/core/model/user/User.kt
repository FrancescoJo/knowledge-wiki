/*
 * User.kt
 *
 * $Since: 2026-05-23T00:00:00Z
 */
package com.fj.omnimemo.core.model.user

import java.time.Instant

/**
 * Represents an authenticated user of the system.
 *
 * [email] is the plaintext address. Encryption is the responsibility of
 * the infrastructure layer; the domain always operates on plaintext.
 *
 * [passwordHash] contains the bcrypt-hashed password. The raw password
 * is never stored or passed through this type.
 *
 * [id] is null for a user that has not yet been persisted.
 *
 * @author Francesco Jo
 * @since 0.1.1
 * @version 0.1.1
 */
data class User(
    val id: Long?,
    val email: String,
    val passwordHash: String,
    val createdAt: Instant,
    val updatedAt: Instant
)
