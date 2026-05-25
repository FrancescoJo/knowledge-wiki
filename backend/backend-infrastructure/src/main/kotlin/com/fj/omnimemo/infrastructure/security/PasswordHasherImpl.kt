/*
 * PasswordHasherImpl.kt
 *
 * $Since: 2026-05-25T00:00:00Z
 */
package com.fj.omnimemo.infrastructure.security

import com.fj.omnimemo.core.model.user.PasswordHasher
import org.springframework.security.crypto.password.PasswordEncoder

/**
 * [PasswordHasher] implementation backed by Spring Security's [PasswordEncoder].
 *
 * @author Francesco Jo
 * @since 0.1.1
 * @version 0.1.1
 */
class PasswordHasherImpl(private val encoder: PasswordEncoder) : PasswordHasher {
    override fun hash(rawPassword: String): String = encoder.encode(rawPassword)
    override fun matches(rawPassword: String, hash: String): Boolean = encoder.matches(rawPassword, hash)
}
