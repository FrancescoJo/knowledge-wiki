/*
 * PasswordHasher.kt
 *
 * $Since: 2026-05-25T00:00:00Z
 */
package com.fj.omnimemo.core.user.security

/**
 * Contract for hashing and verifying passwords.
 *
 * The domain never sees raw passwords after initial creation; all comparisons
 * operate through this interface so the hashing algorithm remains an
 * infrastructure concern.
 *
 * @author Francesco Jo
 * @since 0.1.1
 * @version 0.1.1
 */
interface PasswordHasher {
    fun hash(rawPassword: String): String
    fun matches(rawPassword: String, hash: String): Boolean
}
