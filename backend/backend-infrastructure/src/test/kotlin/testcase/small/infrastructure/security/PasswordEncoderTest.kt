/*
 * PasswordEncoderTest.kt
 *
 * $Since: 2026-05-25T00:00:00Z
 */
package testcase.small.infrastructure.security

import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.security.crypto.password.PasswordEncoder
import test.com.fj.omnimemo.core.annotation.SmallTest

@SmallTest
class PasswordEncoderTest {
    private val encoder: PasswordEncoder = BCryptPasswordEncoder()

    @Nested
    inner class Encode {
        @Test
        fun `should produce hash that differs from raw password`() {
            val hash = encoder.encode("secret123")

            hash shouldNotBe "secret123"
        }

        @Test
        fun `should produce different hashes for the same password on successive calls`() {
            val hash1 = encoder.encode("secret123")
            val hash2 = encoder.encode("secret123")

            hash1 shouldNotBe hash2
        }
    }

    @Nested
    inner class Matches {
        @Test
        fun `should return true when password matches its hash`() {
            val hash = encoder.encode("secret123")

            encoder.matches("secret123", hash) shouldBe true
        }

        @Test
        fun `should return false when password does not match hash`() {
            val hash = encoder.encode("secret123")

            encoder.matches("wrong-password", hash) shouldBe false
        }
    }
}
