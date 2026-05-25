/*
 * JwtTokenServiceTest.kt
 *
 * $Since: 2026-05-25T00:00:00Z
 */
package com.fj.omnimemo.infrastructure.security

import com.fj.omnimemo.core.test.annotation.SmallTest
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.Instant
import javax.crypto.spec.SecretKeySpec
import kotlin.test.assertEquals
import kotlin.test.assertNull

@SmallTest
class JwtTokenServiceTest {

    private val signingKey = SecretKeySpec(ByteArray(32) { it.toByte() }, "HmacSHA256")
    private val service = JwtTokenService(signingKey)

    @Nested
    inner class Issue {
        @Test
        fun `should return a serialised JWT with three dot-separated parts`() {
            val token = service.issue("user-1", Instant.now().plusSeconds(60))

            assertEquals(2, token.count { it == '.' })
        }
    }

    @Nested
    inner class Verify {
        @Test
        fun `should return subject for a valid token`() {
            val token = service.issue("user-1", Instant.now().plusSeconds(60))

            assertEquals("user-1", service.verify(token))
        }

        @Test
        fun `should return null for an expired token`() {
            val token = service.issue("user-1", Instant.now().minusSeconds(1))

            assertNull(service.verify(token))
        }

        @Test
        fun `should return null for a tampered token`() {
            val token = service.issue("user-1", Instant.now().plusSeconds(60))
            val tampered = token.dropLast(4) + "XXXX"

            assertNull(service.verify(tampered))
        }

        @Test
        fun `should return null for malformed input`() {
            assertNull(service.verify("not-a-jwt"))
        }
    }
}
