/*
 * UserCredentialController.kt
 *
 * $Since: 2026-05-30T00:00:00Z
 */
package com.fj.omnimemo.api.endpoint.user

import com.fj.omnimemo.api.endpoint.ApiPathsV1
import com.fj.omnimemo.api.endpoint.user.dto.request.UpdateEmailRequest
import com.fj.omnimemo.api.endpoint.user.dto.request.UpdatePasswordRequest
import com.fj.omnimemo.api.endpoint.user.dto.response.UserResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.parameters.RequestBody
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.*

/**
 * REST API contract for user credential management: e-mail and password updates.
 *
 * All operations require an authenticated session.
 * Account lifecycle (create, delete, lookup) is handled by [UserController].
 * Implementations live in the `impl` sub-package.
 *
 * @author Francesco Jo
 * @since 0.2.0
 * @version 0.2.0
 */
@RequestMapping(produces = [MediaType.APPLICATION_JSON_VALUE])
@Tag(name = "User", description = "User account management")
interface UserCredentialController {

    @Operation(
        summary = "Update user email",
        requestBody = RequestBody(
            content = [Content(
                mediaType = MediaType.APPLICATION_JSON_VALUE,
                schema = Schema(ref = "#/components/schemas/v1.user.UpdateEmailRequest"),
            )],
        ),
        responses = [
            ApiResponse(
                responseCode = "200", description = "Email updated",
                content = [Content(schema = Schema(ref = "#/components/schemas/v1.user.UserResponse"))],
            ),
            ApiResponse(
                responseCode = "400", description = "Malformed user ID",
                content = [Content(schema = Schema(ref = "#/components/schemas/common.ErrorResponseEnvelope"))],
            ),
            ApiResponse(
                responseCode = "404", description = "User not found",
                content = [Content(schema = Schema(ref = "#/components/schemas/common.ErrorResponseEnvelope"))],
            ),
        ],
    )
    @PutMapping("${ApiPathsV1.USERS}/{id}/email", consumes = [MediaType.APPLICATION_JSON_VALUE])
    fun updateEmail(
        @PathVariable id: String,
        @org.springframework.web.bind.annotation.RequestBody request: UpdateEmailRequest,
    ): UserResponse

    @Operation(
        summary = "Update user password",
        requestBody = RequestBody(
            content = [Content(
                mediaType = MediaType.APPLICATION_JSON_VALUE,
                schema = Schema(ref = "#/components/schemas/v1.user.UpdatePasswordRequest"),
            )],
        ),
        responses = [
            ApiResponse(
                responseCode = "200", description = "Password updated",
                content = [Content(schema = Schema(ref = "#/components/schemas/v1.user.UserResponse"))],
            ),
            ApiResponse(
                responseCode = "400", description = "Malformed user ID",
                content = [Content(schema = Schema(ref = "#/components/schemas/common.ErrorResponseEnvelope"))],
            ),
            ApiResponse(
                responseCode = "404", description = "User not found",
                content = [Content(schema = Schema(ref = "#/components/schemas/common.ErrorResponseEnvelope"))],
            ),
        ],
    )
    @PutMapping("${ApiPathsV1.USERS}/{id}/password", consumes = [MediaType.APPLICATION_JSON_VALUE])
    fun updatePassword(
        @PathVariable id: String,
        @org.springframework.web.bind.annotation.RequestBody request: UpdatePasswordRequest,
    ): UserResponse
}
