/*
 * JwtAuthenticationFilter.kt
 *
 * $Since: 2026-05-25T00:00:00Z
 */
package com.fj.omnimemo.api.security

import com.fj.omnimemo.infrastructure.security.JwtTokenService
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.filter.OncePerRequestFilter

/**
 * Extracts and verifies the JWT access token from the incoming request cookie,
 * populating the [SecurityContextHolder] with the authenticated principal on success.
 *
 * @author Francesco Jo
 * @since 0.1.1
 * @version 0.1.1
 */
class JwtAuthenticationFilter(
    private val tokenService: JwtTokenService,
) : OncePerRequestFilter() {
    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain,
    ) {
        val subject = request.cookies
            ?.find { it.name == ACCESS_TOKEN_COOKIE }
            ?.let { tokenService.verify(it.value) }

        if (subject != null) {
            SecurityContextHolder.getContext().authentication =
                UsernamePasswordAuthenticationToken(subject, null, emptyList())
        }

        filterChain.doFilter(request, response)
    }

    companion object {
        const val ACCESS_TOKEN_COOKIE = "access_token"
    }
}
