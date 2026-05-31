/*
 * AuthController.kt
 *
 * $Since: 2026-05-26T00:00:00Z
 */
package com.fj.omnimemo.api.endpoint.auth

import com.fj.omnimemo.api.endpoint.ApiPathsV1
import com.fj.omnimemo.api.endpoint.auth.dto.response.AuthTokenResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.parameters.RequestBody
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam

/**
 * REST API contract for authentication: login, logout, and token refresh.
 *
 * On login and refresh, implementations must set httpOnly access and refresh token cookies
 * and return the tokens in the response body for native clients.
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
        requestBody = RequestBody(
            content = [Content(
                mediaType = MediaType.APPLICATION_FORM_URLENCODED_VALUE,
                schema = Schema(ref = "#/components/schemas/v1.auth.LoginRequest"),
            )],
        ),
        responses = [
            ApiResponse(
                responseCode = "200", description = "Login successful; tokens in response body and cookies",
                content = [Content(schema = Schema(ref = "#/components/schemas/v1.auth.AuthTokenResponse"))],
            ),
            ApiResponse(
                responseCode = "401", description = "Invalid credentials",
                content = [Content(schema = Schema(ref = "#/components/schemas/common.ErrorResponseEnvelope"))],
            ),
        ],
    )
    @PostMapping("${ApiPathsV1.AUTH}/login", consumes = [MediaType.APPLICATION_FORM_URLENCODED_VALUE])
    fun login(
        @RequestParam email: String,
        @RequestParam password: String,
        response: HttpServletResponse,
    ): AuthTokenResponse

    @Operation(
        summary = "Logout",
        responses = [
            ApiResponse(responseCode = "200", description = "Logout successful; cookies cleared"),
        ],
    )
    @PostMapping("${ApiPathsV1.AUTH}/logout")
    fun logout(request: HttpServletRequest, response: HttpServletResponse)

    @Operation(
        summary = "Refresh access token",
        responses = [
            ApiResponse(
                responseCode = "200", description = "Tokens rotated; new tokens in response body and cookies",
                content = [Content(schema = Schema(ref = "#/components/schemas/v1.auth.AuthTokenResponse"))],
            ),
            ApiResponse(
                responseCode = "401", description = "Missing or invalid refresh token",
                content = [Content(schema = Schema(ref = "#/components/schemas/common.ErrorResponseEnvelope"))],
            ),
        ],
    )
    @PostMapping("${ApiPathsV1.AUTH}/refresh")
    fun refresh(request: HttpServletRequest, response: HttpServletResponse): AuthTokenResponse
}
