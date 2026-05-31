/*
 * JwtTokenServiceTest.kt
 *
 * $Since: 2026-05-25T00:00:00Z
 */
package testcase.small.infrastructure.security

import com.fj.omnimemo.infrastructure.security.JwtTokenService
import com.nimbusds.jose.JWSAlgorithm
import com.nimbusds.jose.JWSHeader
import com.nimbusds.jose.crypto.MACSigner
import com.nimbusds.jwt.JWTClaimsSet
import com.nimbusds.jwt.SignedJWT
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import test.com.fj.omnimemo.core.annotation.SmallTest
import java.time.Instant
import javax.crypto.spec.SecretKeySpec

@SmallTest
class JwtTokenServiceTest {
    private val signingKey = SecretKeySpec(ByteArray(32) { it.toByte() }, "HmacSHA256")
    private lateinit var sut: JwtTokenService

    @BeforeEach
    fun setUp() {
        sut = JwtTokenService(signingKey)
    }

    @Nested
    inner class Issue {
        @Test
        fun `should return a serialised JWT with three dot-separated parts`() {
            val token = sut.issue("user-1", Instant.now().plusSeconds(60))

            token.count { it == '.' } shouldBe 2
        }
    }

    @Nested
    inner class Verify {
        @Test
        fun `should return subject for a valid token`() {
            val token = sut.issue("user-1", Instant.now().plusSeconds(60))

            sut.verify(token) shouldBe "user-1"
        }

        @Test
        fun `should return null for an expired token`() {
            val token = sut.issue("user-1", Instant.now().minusSeconds(1))

            sut.verify(token) shouldBe null
        }

        @Test
        fun `should return null for a tampered token`() {
            val token = sut.issue("user-1", Instant.now().plusSeconds(60))
            val tampered = token.dropLast(4) + "XXXX"

            sut.verify(tampered) shouldBe null
        }

        @Test
        fun `should return null for malformed input`() {
            sut.verify("not-a-jwt") shouldBe null
        }

        @Test
        fun `should return null for a token without an expiry claim`() {
            val jwt = SignedJWT(
                JWSHeader(JWSAlgorithm.HS256),
                JWTClaimsSet.Builder().subject("user-1").build(),
            )
            jwt.sign(MACSigner(signingKey))

            sut.verify(jwt.serialize()) shouldBe null
        }
    }
}
