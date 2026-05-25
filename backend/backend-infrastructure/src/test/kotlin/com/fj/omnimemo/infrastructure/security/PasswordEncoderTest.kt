/*
 * PasswordEncoderTest.kt
 *
 * $Since: 2026-05-25T00:00:00Z
 */
package com.fj.omnimemo.infrastructure.security

import com.fj.omnimemo.core.test.annotation.SmallTest
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.security.crypto.password.PasswordEncoder
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@SmallTest
class PasswordEncoderTest {

    private val encoder: PasswordEncoder = BCryptPasswordEncoder()

    @Nested
    inner class Encode {
        @Test
        fun `should produce hash that differs from raw password`() {
            val hash = encoder.encode("secret123")

            assertFalse(hash == "secret123")
        }

        @Test
        fun `should produce different hashes for the same password on successive calls`() {
            val hash1 = encoder.encode("secret123")
            val hash2 = encoder.encode("secret123")

            assertFalse(hash1 == hash2)
        }
    }

    @Nested
    inner class Matches {
        @Test
        fun `should return true when password matches its hash`() {
            val hash = encoder.encode("secret123")

            assertTrue(encoder.matches("secret123", hash))
        }

        @Test
        fun `should return false when password does not match hash`() {
            val hash = encoder.encode("secret123")

            assertFalse(encoder.matches("wrong-password", hash))
        }
    }
}
