/*
 * JwtAuthenticationFilterTest.kt
 *
 * $Since: 2026-05-25T00:00:00Z
 */
package testcase.small.api.security

import com.fj.omnimemo.api.security.JwtAuthenticationFilter
import com.fj.omnimemo.infrastructure.security.JwtTokenService
import io.kotest.assertions.assertSoftly
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import jakarta.servlet.FilterChain
import jakarta.servlet.http.Cookie
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.mock.web.MockHttpServletResponse
import org.springframework.security.core.context.SecurityContextHolder
import test.com.fj.omnimemo.core.annotation.SmallTest
import java.time.Instant
import javax.crypto.spec.SecretKeySpec

@SmallTest
class JwtAuthenticationFilterTest {
    private val signingKey = SecretKeySpec(ByteArray(32) { it.toByte() }, "HmacSHA256")
    private val noOpChain = FilterChain { _, _ -> }

    private lateinit var tokenService: JwtTokenService
    private lateinit var sut: JwtAuthenticationFilter

    @BeforeEach
    fun setUp() {
        tokenService = JwtTokenService(signingKey)
        sut = JwtAuthenticationFilter(tokenService)
        SecurityContextHolder.clearContext()
    }

    @AfterEach
    fun tearDown() {
        SecurityContextHolder.clearContext()
    }

    @DisplayName("JwtAuthenticationFilter should set authentication:")
    @Nested
    inner class WithAuthentication {
        @Test
        fun `when a valid access token cookie is present`() {
            val token = tokenService.issue("user-42", Instant.now().plusSeconds(60))
            val request = MockHttpServletRequest().apply {
                setCookies(Cookie(JwtAuthenticationFilter.ACCESS_TOKEN_COOKIE, token))
            }

            sut.doFilter(request, MockHttpServletResponse(), noOpChain)

            val auth = SecurityContextHolder.getContext().authentication
            assertSoftly {
                auth shouldNotBe null
                auth?.principal shouldBe "user-42"
            }
        }

        @Test
        fun `when access token cookie follows other cookies`() {
            val token = tokenService.issue("user-42", Instant.now().plusSeconds(60))
            val request = MockHttpServletRequest().apply {
                setCookies(
                    Cookie("session_id", "some-value"),
                    Cookie(JwtAuthenticationFilter.ACCESS_TOKEN_COOKIE, token),
                )
            }

            sut.doFilter(request, MockHttpServletResponse(), noOpChain)

            SecurityContextHolder.getContext().authentication?.principal shouldBe "user-42"
        }
    }

    @DisplayName("JwtAuthenticationFilter should not set authentication:")
    @Nested
    inner class WithoutAuthentication {
        @Test
        fun `when no cookie is present`() {
            sut.doFilter(MockHttpServletRequest(), MockHttpServletResponse(), noOpChain)

            SecurityContextHolder.getContext().authentication shouldBe null
        }

        @Test
        fun `when access token cookie value is not a valid JWT`() {
            val request = MockHttpServletRequest().apply {
                setCookies(Cookie(JwtAuthenticationFilter.ACCESS_TOKEN_COOKIE, "not-a-jwt"))
            }

            sut.doFilter(request, MockHttpServletResponse(), noOpChain)

            SecurityContextHolder.getContext().authentication shouldBe null
        }

        @Test
        fun `when access token is expired`() {
            val expiredToken = tokenService.issue("user-42", Instant.now().minusSeconds(1))
            val request = MockHttpServletRequest().apply {
                setCookies(Cookie(JwtAuthenticationFilter.ACCESS_TOKEN_COOKIE, expiredToken))
            }

            sut.doFilter(request, MockHttpServletResponse(), noOpChain)

            SecurityContextHolder.getContext().authentication shouldBe null
        }

        @Test
        fun `when a different cookie is present but not the access token cookie`() {
            val request = MockHttpServletRequest().apply {
                setCookies(Cookie("session_id", "some-value"))
            }

            sut.doFilter(request, MockHttpServletResponse(), noOpChain)

            SecurityContextHolder.getContext().authentication shouldBe null
        }
    }
}
