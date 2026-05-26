/*
 * AuthControllerImplTest.kt
 *
 * $Since: 2026-05-26T00:00:00Z
 */
package com.fj.omnimemo.api.endpoint.auth.impl

import com.fj.omnimemo.api.endpoint.auth.dto.request.LoginRequest
import com.fj.omnimemo.api.security.JwtAuthenticationFilter
import com.fj.omnimemo.core.security.MockTokenIssuer
import com.fj.omnimemo.core.test.annotation.SmallTest
import com.fj.omnimemo.core.user.model.User
import com.fj.omnimemo.core.user.repository.MockRefreshTokenRepository
import com.fj.omnimemo.core.user.repository.MockUserRepository
import com.fj.omnimemo.core.user.security.MockPasswordHasher
import com.fj.omnimemo.core.user.usecase.LoginUseCase
import io.kotest.assertions.assertSoftly
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import jakarta.servlet.http.Cookie
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.mock.web.MockHttpServletResponse
import org.springframework.web.server.ResponseStatusException
import java.time.Duration

@SmallTest
class AuthControllerImplTest {

    private val repo = MockUserRepository()
    private val hasher = MockPasswordHasher()
    private val tokenIssuer = MockTokenIssuer()
    private val refreshTokenRepo = MockRefreshTokenRepository()
    private val useCase = LoginUseCase(repo, hasher, tokenIssuer, refreshTokenRepo, Duration.ofDays(14))
    private val controller = AuthControllerImpl(useCase, tokenTtlSeconds = 259200, refreshTtlSeconds = 1209600)

    @BeforeEach
    fun setUp() {
        repo.clear()
        refreshTokenRepo.clear()
        val user = User.create("alice@example.com", hasher.hash("secret"))
        repo.save(user)
    }

    @Nested
    inner class Login {

        @Test
        fun `should set access and refresh token cookies on valid credentials`() {
            val response = MockHttpServletResponse()

            controller.login(LoginRequest("alice@example.com", "secret"), response)

            val accessCookie = response.getCookie(JwtAuthenticationFilter.ACCESS_TOKEN_COOKIE)
            val refreshCookie = response.getCookie(AuthControllerImpl.REFRESH_TOKEN_COOKIE)
            assertSoftly {
                accessCookie shouldNotBe null
                refreshCookie shouldNotBe null
                accessCookie?.isHttpOnly shouldBe true
                refreshCookie?.isHttpOnly shouldBe true
                accessCookie?.maxAge shouldBe 259200
                refreshCookie?.maxAge shouldBe 1209600
            }
        }

        @Test
        fun `should throw 401 for invalid password`() {
            shouldThrow<ResponseStatusException> {
                controller.login(LoginRequest("alice@example.com", "wrong"), MockHttpServletResponse())
            }.statusCode shouldBe HttpStatus.UNAUTHORIZED
        }

        @Test
        fun `should throw 401 for unknown email`() {
            shouldThrow<ResponseStatusException> {
                controller.login(LoginRequest("nobody@example.com", "secret"), MockHttpServletResponse())
            }.statusCode shouldBe HttpStatus.UNAUTHORIZED
        }
    }

    @Nested
    inner class Logout {

        @Test
        fun `should clear both cookies on logout`() {
            val loginResult = useCase.login("alice@example.com", "secret")!!
            val request = MockHttpServletRequest().apply {
                setCookies(Cookie(AuthControllerImpl.REFRESH_TOKEN_COOKIE, loginResult.refreshToken))
            }
            val response = MockHttpServletResponse()

            controller.logout(request, response)

            val accessCookie = response.getCookie(JwtAuthenticationFilter.ACCESS_TOKEN_COOKIE)
            val refreshCookie = response.getCookie(AuthControllerImpl.REFRESH_TOKEN_COOKIE)
            assertSoftly {
                accessCookie shouldNotBe null
                refreshCookie shouldNotBe null
                accessCookie?.maxAge shouldBe 0
                refreshCookie?.maxAge shouldBe 0
            }
        }

        @Test
        fun `should delete refresh token from store on logout`() {
            val loginResult = useCase.login("alice@example.com", "secret")!!
            val request = MockHttpServletRequest().apply {
                setCookies(Cookie(AuthControllerImpl.REFRESH_TOKEN_COOKIE, loginResult.refreshToken))
            }

            controller.logout(request, MockHttpServletResponse())

            refreshTokenRepo.findByToken(loginResult.refreshToken) shouldBe null
        }

        @Test
        fun `should clear cookies even when no refresh token cookie is present`() {
            val response = MockHttpServletResponse()

            controller.logout(MockHttpServletRequest(), response)

            assertSoftly {
                response.getCookie(JwtAuthenticationFilter.ACCESS_TOKEN_COOKIE) shouldNotBe null
                response.getCookie(AuthControllerImpl.REFRESH_TOKEN_COOKIE) shouldNotBe null
            }
        }
    }

    @Nested
    inner class Refresh {

        @Test
        fun `should rotate cookies for a valid refresh token`() {
            val loginResult = useCase.login("alice@example.com", "secret")!!
            val request = MockHttpServletRequest().apply {
                setCookies(Cookie(AuthControllerImpl.REFRESH_TOKEN_COOKIE, loginResult.refreshToken))
            }
            val response = MockHttpServletResponse()

            controller.refresh(request, response)

            assertSoftly {
                response.getCookie(JwtAuthenticationFilter.ACCESS_TOKEN_COOKIE) shouldNotBe null
                response.getCookie(AuthControllerImpl.REFRESH_TOKEN_COOKIE) shouldNotBe null
            }
        }

        @Test
        fun `should throw 401 when no refresh token cookie is present`() {
            shouldThrow<ResponseStatusException> {
                controller.refresh(MockHttpServletRequest(), MockHttpServletResponse())
            }.statusCode shouldBe HttpStatus.UNAUTHORIZED
        }

        @Test
        fun `should throw 401 for an invalid refresh token value`() {
            val request = MockHttpServletRequest().apply {
                setCookies(Cookie(AuthControllerImpl.REFRESH_TOKEN_COOKIE, "invalid-token"))
            }
            shouldThrow<ResponseStatusException> {
                controller.refresh(request, MockHttpServletResponse())
            }.statusCode shouldBe HttpStatus.UNAUTHORIZED
        }
    }
}
