/*
 * BootstrapController.kt
 *
 * $Since: 2026-05-26T00:00:00Z
 */
package com.fj.omnimemo.api.endpoint.bootstrap

import com.fj.omnimemo.api.endpoint.ApiPathsV1
import com.fj.omnimemo.api.endpoint.bootstrap.dto.request.BootstrapUserRequest
import com.fj.omnimemo.api.endpoint.user.dto.response.UserResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.parameters.RequestBody
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus

/**
 * REST API contract for the one-time bootstrap user registration.
 *
 * This endpoint is restricted to localhost and only succeeds when the users
 * table is empty. Use it to create the first administrator account.
 *
 * @author Francesco Jo
 * @since 0.1.1
 * @version 0.1.1
 */
@RequestMapping(produces = [MediaType.APPLICATION_JSON_VALUE])
@Tag(name = "Bootstrap", description = "One-time first-user registration (localhost only)")
interface BootstrapController {
    @Operation(
        summary = "Create the first user",
        requestBody = RequestBody(
            content = [Content(
                mediaType = MediaType.APPLICATION_JSON_VALUE,
                schema = Schema(ref = "#/components/schemas/v1.bootstrap.BootstrapUserRequest"),
            )],
        ),
        responses = [
            ApiResponse(
                responseCode = "201", description = "First user created",
                content = [Content(schema = Schema(ref = "#/components/schemas/v1.user.UserResponse"))],
            ),
            ApiResponse(
                responseCode = "403", description = "Request not from localhost",
                content = [Content(schema = Schema(ref = "#/components/schemas/common.ErrorResponseEnvelope"))],
            ),
            ApiResponse(
                responseCode = "409", description = "At least one user already exists",
                content = [Content(schema = Schema(ref = "#/components/schemas/common.ErrorResponseEnvelope"))],
            ),
        ],
    )
    @PostMapping(ApiPathsV1.BOOTSTRAP_USERS, consumes = [MediaType.APPLICATION_JSON_VALUE])
    @ResponseStatus(HttpStatus.CREATED)
    fun bootstrapUser(@org.springframework.web.bind.annotation.RequestBody request: BootstrapUserRequest): UserResponse
}
