/*
 * MockPasswordHasher.kt
 *
 * $Since: 2026-05-25T00:00:00Z
 */
package com.fj.omnimemo.core.user.model

/**
 * In-memory fake of [PasswordHasher] for use in Small tests.
 *
 * Prefixes the raw password with "hashed:" so tests can assert on the
 * stored value without running a real bcrypt operation.
 *
 * @author Francesco Jo
 * @since 0.1.1
 * @version 0.1.1
 */
class MockPasswordHasher : PasswordHasher {
    override fun hash(rawPassword: String): String = "hashed:$rawPassword"
    override fun matches(rawPassword: String, hash: String): Boolean = hash == "hashed:$rawPassword"
}
