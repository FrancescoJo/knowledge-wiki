/*
 * JwtTokenServiceTest.kt
 *
 * $Since: 2026-05-25T00:00:00Z
 */
package com.fj.omnimemo.infrastructure.security

import com.fj.omnimemo.core.test.annotation.SmallTest
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.Instant
import javax.crypto.spec.SecretKeySpec

@SmallTest
class JwtTokenServiceTest {

    private val signingKey = SecretKeySpec(ByteArray(32) { it.toByte() }, "HmacSHA256")
    private val service = JwtTokenService(signingKey)

    @Nested
    inner class Issue {
        @Test
        fun `should return a serialised JWT with three dot-separated parts`() {
            val token = service.issue("user-1", Instant.now().plusSeconds(60))

            token.count { it == '.' } shouldBe 2
        }
    }

    @Nested
    inner class Verify {
        @Test
        fun `should return subject for a valid token`() {
            val token = service.issue("user-1", Instant.now().plusSeconds(60))

            service.verify(token) shouldBe "user-1"
        }

        @Test
        fun `should return null for an expired token`() {
            val token = service.issue("user-1", Instant.now().minusSeconds(1))

            service.verify(token) shouldBe null
        }

        @Test
        fun `should return null for a tampered token`() {
            val token = service.issue("user-1", Instant.now().plusSeconds(60))
            val tampered = token.dropLast(4) + "XXXX"

            service.verify(tampered) shouldBe null
        }

        @Test
        fun `should return null for malformed input`() {
            service.verify("not-a-jwt") shouldBe null
        }
    }
}
