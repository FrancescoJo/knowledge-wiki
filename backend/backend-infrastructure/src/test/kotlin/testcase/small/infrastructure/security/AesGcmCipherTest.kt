/*
 * AesGcmCipherTest.kt
 *
 * $Since: 2026-05-23T00:00:00Z
 */
package testcase.small.infrastructure.security

import com.fj.omnimemo.infrastructure.security.AesGcmCipher
import io.kotest.matchers.ints.shouldBeGreaterThan
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import test.com.fj.omnimemo.core.annotation.SmallTest

@SmallTest
class AesGcmCipherTest {
    private lateinit var sut: AesGcmCipher

    @BeforeEach
    fun setUp() {
        sut = AesGcmCipher(ByteArray(32) { it.toByte() })
    }

    @Nested
    inner class Encrypt {
        @Test
        fun `should produce ciphertext that differs from plaintext bytes`() {
            val iv = sut.generateIv()
            val ciphertext = sut.encrypt("alice@example.com", iv)

            ciphertext.contentEquals("alice@example.com".toByteArray(Charsets.UTF_8)) shouldBe false
        }

        @Test
        fun `should produce different ciphertext for different IVs given the same plaintext`() {
            val iv1 = sut.generateIv()
            val iv2 = sut.generateIv()
            val ciphertext1 = sut.encrypt("alice@example.com", iv1)
            val ciphertext2 = sut.encrypt("alice@example.com", iv2)

            ciphertext1.contentEquals(ciphertext2) shouldBe false
        }
    }

    @Nested
    inner class Decrypt {
        @Test
        fun `should recover original plaintext after encrypt-decrypt roundtrip`() {
            val plaintext = "alice@example.com"
            val iv = sut.generateIv()
            val ciphertext = sut.encrypt(plaintext, iv)

            sut.decrypt(ciphertext, iv) shouldBe plaintext
        }

        @Test
        fun `should recover original plaintext for non-ASCII input`() {
            val plaintext = "사용자@예시.한국"
            val iv = sut.generateIv()
            val ciphertext = sut.encrypt(plaintext, iv)

            sut.decrypt(ciphertext, iv) shouldBe plaintext
        }
    }

    @Nested
    inner class GenerateIv {
        @Test
        fun `should produce a 12-byte IV`() {
            val iv = sut.generateIv()

            iv.size shouldBe 12
        }

        @Test
        fun `should produce unique IVs on successive calls`() {
            val ivs = List(20) { sut.generateIv() }
            val unique = ivs.map { it.toList() }.toSet()

            unique.size shouldBeGreaterThan 1
        }
    }
}
