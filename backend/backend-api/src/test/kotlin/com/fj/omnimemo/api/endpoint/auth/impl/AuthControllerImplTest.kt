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
import jakarta.servlet.http.Cookie
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.mock.web.MockHttpServletResponse
import org.springframework.web.server.ResponseStatusException
import java.time.Duration
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertNull

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
            assertNotNull(accessCookie)
            assertNotNull(refreshCookie)
            assertEquals(true, accessCookie.isHttpOnly)
            assertEquals(true, refreshCookie.isHttpOnly)
            assertEquals(259200, accessCookie.maxAge)
            assertEquals(1209600, refreshCookie.maxAge)
        }

        @Test
        fun `should throw 401 for invalid password`() {
            val ex = assertFailsWith<ResponseStatusException> {
                controller.login(LoginRequest("alice@example.com", "wrong"), MockHttpServletResponse())
            }
            assertEquals(HttpStatus.UNAUTHORIZED, ex.statusCode)
        }

        @Test
        fun `should throw 401 for unknown email`() {
            val ex = assertFailsWith<ResponseStatusException> {
                controller.login(LoginRequest("nobody@example.com", "secret"), MockHttpServletResponse())
            }
            assertEquals(HttpStatus.UNAUTHORIZED, ex.statusCode)
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
            assertNotNull(accessCookie)
            assertNotNull(refreshCookie)
            assertEquals(0, accessCookie.maxAge)
            assertEquals(0, refreshCookie.maxAge)
        }

        @Test
        fun `should delete refresh token from store on logout`() {
            val loginResult = useCase.login("alice@example.com", "secret")!!
            val request = MockHttpServletRequest().apply {
                setCookies(Cookie(AuthControllerImpl.REFRESH_TOKEN_COOKIE, loginResult.refreshToken))
            }

            controller.logout(request, MockHttpServletResponse())

            assertNull(refreshTokenRepo.findByToken(loginResult.refreshToken))
        }

        @Test
        fun `should clear cookies even when no refresh token cookie is present`() {
            val response = MockHttpServletResponse()

            controller.logout(MockHttpServletRequest(), response)

            assertNotNull(response.getCookie(JwtAuthenticationFilter.ACCESS_TOKEN_COOKIE))
            assertNotNull(response.getCookie(AuthControllerImpl.REFRESH_TOKEN_COOKIE))
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

            assertNotNull(response.getCookie(JwtAuthenticationFilter.ACCESS_TOKEN_COOKIE))
            assertNotNull(response.getCookie(AuthControllerImpl.REFRESH_TOKEN_COOKIE))
        }

        @Test
        fun `should throw 401 when no refresh token cookie is present`() {
            val ex = assertFailsWith<ResponseStatusException> {
                controller.refresh(MockHttpServletRequest(), MockHttpServletResponse())
            }
            assertEquals(HttpStatus.UNAUTHORIZED, ex.statusCode)
        }

        @Test
        fun `should throw 401 for an invalid refresh token value`() {
            val request = MockHttpServletRequest().apply {
                setCookies(Cookie(AuthControllerImpl.REFRESH_TOKEN_COOKIE, "invalid-token"))
            }
            val ex = assertFailsWith<ResponseStatusException> {
                controller.refresh(request, MockHttpServletResponse())
            }
            assertEquals(HttpStatus.UNAUTHORIZED, ex.statusCode)
        }
    }
}
