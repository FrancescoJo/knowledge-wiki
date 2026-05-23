/*
 * AesGcmCipherTest.kt
 *
 * $Since: 2026-05-23T00:00:00Z
 */
package com.fj.omnimemo.infrastructure.security

import com.fj.omnimemo.core.test.annotation.SmallTest
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@SmallTest
class AesGcmCipherTest {

    private val cipher = AesGcmCipher(ByteArray(32) { it.toByte() })

    @Nested
    inner class Encrypt {
        @Test
        fun `should produce ciphertext that differs from plaintext bytes`() {
            val iv = cipher.generateIv()
            val ciphertext = cipher.encrypt("alice@example.com", iv)

            assertFalse(ciphertext.contentEquals("alice@example.com".toByteArray(Charsets.UTF_8)))
        }

        @Test
        fun `should produce different ciphertext for different IVs given the same plaintext`() {
            val iv1 = cipher.generateIv()
            val iv2 = cipher.generateIv()
            val ciphertext1 = cipher.encrypt("alice@example.com", iv1)
            val ciphertext2 = cipher.encrypt("alice@example.com", iv2)

            assertFalse(ciphertext1.contentEquals(ciphertext2))
        }
    }

    @Nested
    inner class Decrypt {
        @Test
        fun `should recover original plaintext after encrypt-decrypt roundtrip`() {
            val plaintext = "alice@example.com"
            val iv = cipher.generateIv()
            val ciphertext = cipher.encrypt(plaintext, iv)

            assertEquals(plaintext, cipher.decrypt(ciphertext, iv))
        }

        @Test
        fun `should recover original plaintext for non-ASCII input`() {
            val plaintext = "사용자@예시.한국"
            val iv = cipher.generateIv()
            val ciphertext = cipher.encrypt(plaintext, iv)

            assertEquals(plaintext, cipher.decrypt(ciphertext, iv))
        }
    }

    @Nested
    inner class GenerateIv {
        @Test
        fun `should produce a 12-byte IV`() {
            val iv = cipher.generateIv()

            assertEquals(12, iv.size)
        }

        @Test
        fun `should produce unique IVs on successive calls`() {
            val ivs = List(20) { cipher.generateIv() }
            val unique = ivs.map { it.toList() }.toSet()

            assertTrue(unique.size > 1, "expected unique IVs across 20 calls")
        }
    }
}
