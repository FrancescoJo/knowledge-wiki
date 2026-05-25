/*
 * AuthApiController.kt
 *
 * $Since: 2026-05-25T00:00:00Z
 */
package com.fj.omnimemo.api.endpoint.auth

import com.fj.omnimemo.api.security.JwtAuthenticationFilter
import com.fj.omnimemo.core.user.usecase.LoginUseCase
import jakarta.servlet.http.Cookie
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ResponseStatusException

/**
 * REST endpoints for authentication: login, logout, and token refresh.
 *
 * On success, sets [JwtAuthenticationFilter.ACCESS_TOKEN_COOKIE] and [REFRESH_TOKEN_COOKIE]
 * as httpOnly cookies. On logout, both cookies are cleared by setting Max-Age to 0.
 *
 * @author Francesco Jo
 * @since 0.1.1
 * @version 0.1.1
 */
@RestController
@RequestMapping("/api/auth")
class AuthApiController(
    private val loginUseCase: LoginUseCase,
    @param:Value("\${app.security.token-ttl-seconds}") private val tokenTtlSeconds: Int,
    @param:Value("\${app.security.refresh-token-ttl-seconds}") private val refreshTtlSeconds: Int,
) {

    companion object {
        const val REFRESH_TOKEN_COOKIE = "refresh_token"
    }

    @PostMapping("/login")
    fun login(@RequestBody request: LoginRequest, response: HttpServletResponse) {
        val result = loginUseCase.login(request.email, request.password)
            ?: throw ResponseStatusException(HttpStatus.UNAUTHORIZED)
        response.addCookie(accessTokenCookie(result.accessToken))
        response.addCookie(refreshTokenCookie(result.refreshToken))
    }

    @PostMapping("/logout")
    fun logout(request: HttpServletRequest, response: HttpServletResponse) {
        val refreshToken = request.cookies
            ?.find { it.name == REFRESH_TOKEN_COOKIE }
            ?.value
        if (refreshToken != null) {
            loginUseCase.logout(refreshToken)
        }
        response.addCookie(clearCookie(JwtAuthenticationFilter.ACCESS_TOKEN_COOKIE))
        response.addCookie(clearCookie(REFRESH_TOKEN_COOKIE))
    }

    @PostMapping("/refresh")
    fun refresh(request: HttpServletRequest, response: HttpServletResponse) {
        val refreshToken = request.cookies
            ?.find { it.name == REFRESH_TOKEN_COOKIE }
            ?.value
            ?: throw ResponseStatusException(HttpStatus.UNAUTHORIZED)
        val result = loginUseCase.refresh(refreshToken)
            ?: throw ResponseStatusException(HttpStatus.UNAUTHORIZED)
        response.addCookie(accessTokenCookie(result.accessToken))
        response.addCookie(refreshTokenCookie(result.refreshToken))
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
}
