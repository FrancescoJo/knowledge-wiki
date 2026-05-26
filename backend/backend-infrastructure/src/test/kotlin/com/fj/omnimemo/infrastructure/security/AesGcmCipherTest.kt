/*
 * AesGcmCipherTest.kt
 *
 * $Since: 2026-05-23T00:00:00Z
 */
package com.fj.omnimemo.infrastructure.security

import com.fj.omnimemo.core.test.annotation.SmallTest
import io.kotest.matchers.ints.shouldBeGreaterThan
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

@SmallTest
class AesGcmCipherTest {

    private val cipher = AesGcmCipher(ByteArray(32) { it.toByte() })

    @Nested
    inner class Encrypt {
        @Test
        fun `should produce ciphertext that differs from plaintext bytes`() {
            val iv = cipher.generateIv()
            val ciphertext = cipher.encrypt("alice@example.com", iv)

            ciphertext.contentEquals("alice@example.com".toByteArray(Charsets.UTF_8)) shouldBe false
        }

        @Test
        fun `should produce different ciphertext for different IVs given the same plaintext`() {
            val iv1 = cipher.generateIv()
            val iv2 = cipher.generateIv()
            val ciphertext1 = cipher.encrypt("alice@example.com", iv1)
            val ciphertext2 = cipher.encrypt("alice@example.com", iv2)

            ciphertext1.contentEquals(ciphertext2) shouldBe false
        }
    }

    @Nested
    inner class Decrypt {
        @Test
        fun `should recover original plaintext after encrypt-decrypt roundtrip`() {
            val plaintext = "alice@example.com"
            val iv = cipher.generateIv()
            val ciphertext = cipher.encrypt(plaintext, iv)

            cipher.decrypt(ciphertext, iv) shouldBe plaintext
        }

        @Test
        fun `should recover original plaintext for non-ASCII input`() {
            val plaintext = "사용자@예시.한국"
            val iv = cipher.generateIv()
            val ciphertext = cipher.encrypt(plaintext, iv)

            cipher.decrypt(ciphertext, iv) shouldBe plaintext
        }
    }

    @Nested
    inner class GenerateIv {
        @Test
        fun `should produce a 12-byte IV`() {
            val iv = cipher.generateIv()

            iv.size shouldBe 12
        }

        @Test
        fun `should produce unique IVs on successive calls`() {
            val ivs = List(20) { cipher.generateIv() }
            val unique = ivs.map { it.toList() }.toSet()

            unique.size shouldBeGreaterThan 1
        }
    }
}
