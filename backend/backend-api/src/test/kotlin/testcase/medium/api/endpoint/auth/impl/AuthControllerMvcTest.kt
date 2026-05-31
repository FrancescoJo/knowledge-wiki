/*
 * AuthControllerMvcTest.kt
 *
 * $Since: 2026-05-26T00:00:00Z
 */
package testcase.medium.api.endpoint.auth.impl

import com.fj.omnimemo.api.endpoint.ApiPathsV1
import com.fj.omnimemo.api.endpoint.auth.impl.AuthControllerImpl
import com.fj.omnimemo.core.user.UserProfileCache
import com.fj.omnimemo.core.user.exception.PasswordMismatchException
import com.fj.omnimemo.core.user.exception.RefreshTokenNotFoundException
import com.fj.omnimemo.core.user.exception.TokenExpiredException
import com.fj.omnimemo.core.user.model.LoginResult
import com.fj.omnimemo.core.user.usecase.LoginUseCase
import com.fj.omnimemo.infrastructure.security.JwtTokenService
import jakarta.servlet.http.Cookie
import org.junit.jupiter.api.Test
import org.mockito.BDDMockito.given
import org.mockito.Mockito
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*
import test.com.fj.omnimemo.core.annotation.MediumTest

/**
 * Medium Tests for [AuthControllerImpl]: verifies HTTP path routing, cookie handling,
 * and status codes via the Spring MVC stack.
 *
 * Error paths that originate from domain exceptions are translated to HTTP status codes
 * by [com.fj.omnimemo.api.advice.DomainExceptionAdvice], which is auto-loaded by
 * [WebMvcTest].
 *
 * @author Francesco Jo
 * @since 0.1.1
 * @version 0.1.1
 */
@MediumTest
@WebMvcTest(controllers = [AuthControllerImpl::class])
@AutoConfigureMockMvc(addFilters = false)
class AuthControllerMvcTest {
    @Autowired
    private lateinit var mockMvc: MockMvc

    @MockBean
    private lateinit var loginUseCase: LoginUseCase

    // Required by SecurityConfiguration; not referenced in test methods directly.
    @Suppress("UnusedPrivateProperty")
    @MockBean
    private lateinit var jwtTokenService: JwtTokenService

    // Required by GlobalModelAdvice; not referenced in test methods directly.
    @Suppress("UnusedPrivateProperty")
    @MockBean
    private lateinit var userProfileCache: UserProfileCache

    @Test
    fun `POST auth login returns 200 and sets cookies on valid credentials`() {
        val loginResult = LoginResult.create("access-token", "refresh-token")
        given(loginUseCase.login(anyArg(), anyArg())).willReturn(loginResult)

        mockMvc.perform(
            post("${ApiPathsV1.AUTH}/login")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .param("email", "alice@example.com")
                .param("password", "secret")
        )
            .andExpect(status().isOk)
            .andExpect(cookie().exists("access_token"))
            .andExpect(cookie().exists(AuthControllerImpl.REFRESH_TOKEN_COOKIE))
            .andExpect(cookie().httpOnly("access_token", true))
            .andExpect(jsonPath("$.body.accessToken").value("access-token"))
            .andExpect(jsonPath("$.body.refreshToken").value("refresh-token"))
    }

    @Test
    fun `POST auth login returns 401 for invalid credentials`() {
        given(loginUseCase.login(anyArg(), anyArg())).willThrow(PasswordMismatchException())

        mockMvc.perform(
            post("${ApiPathsV1.AUTH}/login")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .param("email", "alice@example.com")
                .param("password", "wrong")
        )
            .andExpect(status().isUnauthorized)
    }

    @Test
    fun `POST auth logout returns 200 and clears cookies`() {
        mockMvc.perform(post("${ApiPathsV1.AUTH}/logout"))
            .andExpect(status().isOk)
            .andExpect(cookie().maxAge("access_token", 0))
            .andExpect(cookie().maxAge(AuthControllerImpl.REFRESH_TOKEN_COOKIE, 0))
    }

    @Test
    fun `POST auth logout returns HX-Redirect to root`() {
        mockMvc.perform(post("${ApiPathsV1.AUTH}/logout"))
            .andExpect(status().isOk)
            .andExpect(header().string("HX-Redirect", "/"))
    }

    @Test
    fun `POST auth refresh returns 200 and rotates cookies for a valid refresh token`() {
        val loginResult = LoginResult.create("new-access", "new-refresh")
        given(loginUseCase.refresh(anyArg())).willReturn(loginResult)

        mockMvc.perform(
            post("${ApiPathsV1.AUTH}/refresh")
                .cookie(Cookie(AuthControllerImpl.REFRESH_TOKEN_COOKIE, "valid-token"))
        )
            .andExpect(status().isOk)
            .andExpect(cookie().exists("access_token"))
            .andExpect(cookie().exists(AuthControllerImpl.REFRESH_TOKEN_COOKIE))
            .andExpect(jsonPath("$.body.accessToken").value("new-access"))
            .andExpect(jsonPath("$.body.refreshToken").value("new-refresh"))
    }

    @Test
    fun `POST auth refresh returns 401 when no refresh token cookie is present`() {
        mockMvc.perform(post("${ApiPathsV1.AUTH}/refresh"))
            .andExpect(status().isUnauthorized)
    }

    @Test
    fun `POST auth refresh returns 401 when cookies are present but no refresh token cookie`() {
        mockMvc.perform(
            post("${ApiPathsV1.AUTH}/refresh")
                .cookie(Cookie("session_id", "some-value"))
        )
            .andExpect(status().isUnauthorized)
    }

    @Test
    fun `POST auth refresh returns 401 for an invalid refresh token`() {
        given(loginUseCase.refresh(anyArg())).willThrow(RefreshTokenNotFoundException())

        mockMvc.perform(
            post("${ApiPathsV1.AUTH}/refresh")
                .cookie(Cookie(AuthControllerImpl.REFRESH_TOKEN_COOKIE, "bad-token"))
        )
            .andExpect(status().isUnauthorized)
    }

    @Test
    fun `POST auth refresh returns 401 for an expired refresh token`() {
        given(loginUseCase.refresh(anyArg())).willThrow(TokenExpiredException())

        mockMvc.perform(
            post("${ApiPathsV1.AUTH}/refresh")
                .cookie(Cookie(AuthControllerImpl.REFRESH_TOKEN_COOKIE, "expired-token"))
        )
            .andExpect(status().isUnauthorized)
    }

    companion object {
        // Mockito.any() returns null; T : Any would cause Kotlin to emit a runtime null-check
        // on the cast inside this helper, throwing NPE before Mockito can intercept. Keeping T
        // unconstrained makes the cast erasure-safe so Mockito records the matcher as intended.
        @Suppress("UNCHECKED_CAST")
        private fun <T> anyArg(): T = Mockito.any<Any>() as T
    }
}
