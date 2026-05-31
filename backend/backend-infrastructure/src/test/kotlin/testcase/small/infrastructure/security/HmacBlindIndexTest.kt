/*
 * HmacBlindIndexTest.kt
 *
 * $Since: 2026-05-23T00:00:00Z
 */
package testcase.small.infrastructure.security

import com.fj.omnimemo.infrastructure.security.HmacBlindIndex
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import test.com.fj.omnimemo.core.annotation.SmallTest

@SmallTest
class HmacBlindIndexTest {
    private lateinit var sut: HmacBlindIndex

    @BeforeEach
    fun setUp() {
        sut = HmacBlindIndex(ByteArray(32) { it.toByte() })
    }

    @Nested
    inner class Compute {
        @Test
        fun `should produce a 32-byte digest`() {
            val digest = sut.compute("alice@example.com")

            digest.size shouldBe 32
        }

        @Test
        fun `should produce the same digest for the same plaintext`() {
            val first = sut.compute("alice@example.com")
            val second = sut.compute("alice@example.com")

            first.contentEquals(second) shouldBe true
        }

        @Test
        fun `should produce different digests for different plaintexts`() {
            val digestAlice = sut.compute("alice@example.com")
            val digestBob = sut.compute("bob@example.com")

            digestAlice.contentEquals(digestBob) shouldBe false
        }

        @Test
        fun `should produce different digests for different keys`() {
            val otherHmac = HmacBlindIndex(ByteArray(32) { (it + 1).toByte() })

            val digest = sut.compute("alice@example.com")
            val otherDigest = otherHmac.compute("alice@example.com")

            digest.contentEquals(otherDigest) shouldBe false
        }
    }
}
