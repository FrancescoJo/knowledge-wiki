/*
 * AuthController.kt
 *
 * $Since: 2026-05-26T00:00:00Z
 */
package com.fj.omnimemo.api.endpoint.auth

import com.fj.omnimemo.api.endpoint.ApiPathsV1
import com.fj.omnimemo.api.endpoint.auth.dto.request.LoginRequest
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping

/**
 * REST API contract for authentication: login, logout, and token refresh.
 *
 * On success, implementations must set httpOnly access and refresh token cookies.
 * On logout, both cookies must be cleared.
 * Implementations live in the `impl` sub-package.
 *
 * @author Francesco Jo
 * @since 0.1.1
 * @version 0.1.1
 */
@RequestMapping(produces = [MediaType.APPLICATION_JSON_VALUE])
@Tag(name = "Auth", description = "Authentication management")
interface AuthController {

    @Operation(
        summary = "Login",
        responses = [
            ApiResponse(responseCode = "200", description = "Login successful; cookies set"),
            ApiResponse(responseCode = "401", description = "Invalid credentials"),
        ]
    )
    @PostMapping("${ApiPathsV1.AUTH}/login", consumes = [MediaType.APPLICATION_JSON_VALUE])
    fun login(@RequestBody request: LoginRequest, response: HttpServletResponse)

    @Operation(
        summary = "Logout",
        responses = [
            ApiResponse(responseCode = "200", description = "Logout successful; cookies cleared"),
        ]
    )
    @PostMapping("${ApiPathsV1.AUTH}/logout")
    fun logout(request: HttpServletRequest, response: HttpServletResponse)

    @Operation(
        summary = "Refresh access token",
        responses = [
            ApiResponse(responseCode = "200", description = "Tokens rotated; new cookies set"),
            ApiResponse(responseCode = "401", description = "Missing or invalid refresh token"),
        ]
    )
    @PostMapping("${ApiPathsV1.AUTH}/refresh")
    fun refresh(request: HttpServletRequest, response: HttpServletResponse)
}
