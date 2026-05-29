/*
 * AuthControllerImpl.kt
 *
 * $Since: 2026-05-26T00:00:00Z
 */
package com.fj.omnimemo.api.endpoint.auth.impl

import com.fj.omnimemo.api.endpoint.auth.AuthController
import com.fj.omnimemo.api.endpoint.auth.dto.response.AuthTokenResponse
import com.fj.omnimemo.api.security.JwtAuthenticationFilter
import com.fj.omnimemo.core.user.usecase.LoginUseCase
import jakarta.servlet.http.Cookie
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ResponseStatusException

/**
 * @author Francesco Jo
 * @since 0.1.1
 * @version 0.1.1
 */
@RestController
internal class AuthControllerImpl(
    private val loginUseCase: LoginUseCase,
    @param:Value("\${app.security.token-ttl-seconds}") private val tokenTtlSeconds: Int,
    @param:Value("\${app.security.refresh-token-ttl-seconds}") private val refreshTtlSeconds: Int,
) : AuthController {

    override fun login(email: String, password: String, response: HttpServletResponse): AuthTokenResponse {
        val result = loginUseCase.login(email, password)
        response.addCookie(accessTokenCookie(result.accessToken))
        response.addCookie(refreshTokenCookie(result.refreshToken))
        return AuthTokenResponse(result.accessToken, result.refreshToken)
    }

    override fun logout(request: HttpServletRequest, response: HttpServletResponse) {
        val refreshToken = request.cookies
            ?.find { it.name == REFRESH_TOKEN_COOKIE }
            ?.value
        if (refreshToken != null) {
            loginUseCase.logout(refreshToken)
        }
        response.addCookie(clearCookie(JwtAuthenticationFilter.ACCESS_TOKEN_COOKIE))
        response.addCookie(clearCookie(REFRESH_TOKEN_COOKIE))
        response.setHeader("HX-Redirect", "/")
    }

    override fun refresh(request: HttpServletRequest, response: HttpServletResponse): AuthTokenResponse {
        val refreshToken = request.cookies
            ?.find { it.name == REFRESH_TOKEN_COOKIE }
            ?.value
            ?: throw ResponseStatusException(HttpStatus.UNAUTHORIZED)
        val result = loginUseCase.refresh(refreshToken)
        response.addCookie(accessTokenCookie(result.accessToken))
        response.addCookie(refreshTokenCookie(result.refreshToken))
        return AuthTokenResponse(result.accessToken, result.refreshToken)
    }

    private fun accessTokenCookie(value: String) = Cookie(JwtAuthenticationFilter.ACCESS_TOKEN_COOKIE, value).apply {
        isHttpOnly = true
        path = "/"
        maxAge = tokenTtlSeconds
        setAttribute("SameSite", "Strict")
    }

    private fun refreshTokenCookie(value: String) = Cookie(REFRESH_TOKEN_COOKIE, value).apply {
        isHttpOnly = true
        path = "/"
        maxAge = refreshTtlSeconds
        setAttribute("SameSite", "Strict")
    }

    private fun clearCookie(name: String) = Cookie(name, "").apply {
        isHttpOnly = true
        path = "/"
        maxAge = 0
        setAttribute("SameSite", "Strict")
    }

    companion object {
        const val REFRESH_TOKEN_COOKIE = "refresh_token"
    }
}
