/*
 * HmacBlindIndexTest.kt
 *
 * $Since: 2026-05-23T00:00:00Z
 */
package com.fj.omnimemo.infrastructure.security

import com.fj.omnimemo.core.test.annotation.SmallTest
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@SmallTest
class HmacBlindIndexTest {

    private val hmac = HmacBlindIndex(ByteArray(32) { it.toByte() })

    @Nested
    inner class Compute {
        @Test
        fun `should produce a 32-byte digest`() {
            val digest = hmac.compute("alice@example.com")

            assertTrue(digest.size == 32)
        }

        @Test
        fun `should produce the same digest for the same plaintext`() {
            val first = hmac.compute("alice@example.com")
            val second = hmac.compute("alice@example.com")

            assertTrue(first.contentEquals(second))
        }

        @Test
        fun `should produce different digests for different plaintexts`() {
            val digestAlice = hmac.compute("alice@example.com")
            val digestBob = hmac.compute("bob@example.com")

            assertFalse(digestAlice.contentEquals(digestBob))
        }

        @Test
        fun `should produce different digests for different keys`() {
            val otherHmac = HmacBlindIndex(ByteArray(32) { (it + 1).toByte() })

            val digest = hmac.compute("alice@example.com")
            val otherDigest = otherHmac.compute("alice@example.com")

            assertFalse(digest.contentEquals(otherDigest))
        }
    }
}
