/*
 * UserController.kt
 *
 * $Since: 2026-05-26T00:00:00Z
 */
package com.fj.omnimemo.api.endpoint.user

import com.fj.omnimemo.api.endpoint.ApiPathsV1
import com.fj.omnimemo.api.endpoint.user.dto.request.CreateUserRequest
import com.fj.omnimemo.api.endpoint.user.dto.request.UpdateEmailRequest
import com.fj.omnimemo.api.endpoint.user.dto.request.UpdatePasswordRequest
import com.fj.omnimemo.api.endpoint.user.dto.response.UserResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus

/**
 * REST API contract for user account management.
 *
 * All endpoints require authentication unless stated otherwise.
 * Implementations live in the `impl` sub-package.
 *
 * @author Francesco Jo
 * @since 0.1.1
 * @version 0.1.1
 */
@RequestMapping(produces = [MediaType.APPLICATION_JSON_VALUE])
@Tag(name = "User", description = "User account management")
interface UserController {

    @Operation(
        summary = "Find user by ID",
        responses = [
            ApiResponse(responseCode = "200", description = "User found"),
            ApiResponse(responseCode = "400", description = "Malformed user ID"),
            ApiResponse(responseCode = "404", description = "User not found"),
        ]
    )
    @GetMapping("${ApiPathsV1.USERS}/{id}")
    fun findById(@PathVariable id: String): UserResponse

    @Operation(
        summary = "Create user",
        responses = [
            ApiResponse(responseCode = "201", description = "User created"),
            ApiResponse(responseCode = "409", description = "Email already in use"),
        ]
    )
    @PostMapping(ApiPathsV1.USERS, consumes = [MediaType.APPLICATION_JSON_VALUE])
    @ResponseStatus(HttpStatus.CREATED)
    fun create(@RequestBody request: CreateUserRequest): UserResponse

    @Operation(
        summary = "Update user email",
        responses = [
            ApiResponse(responseCode = "200", description = "Email updated"),
            ApiResponse(responseCode = "400", description = "Malformed user ID"),
            ApiResponse(responseCode = "404", description = "User not found"),
        ]
    )
    @PutMapping("${ApiPathsV1.USERS}/{id}/email", consumes = [MediaType.APPLICATION_JSON_VALUE])
    fun updateEmail(@PathVariable id: String, @RequestBody request: UpdateEmailRequest): UserResponse

    @Operation(
        summary = "Update user password",
        responses = [
            ApiResponse(responseCode = "200", description = "Password updated"),
            ApiResponse(responseCode = "400", description = "Malformed user ID"),
            ApiResponse(responseCode = "404", description = "User not found"),
        ]
    )
    @PutMapping("${ApiPathsV1.USERS}/{id}/password", consumes = [MediaType.APPLICATION_JSON_VALUE])
    fun updatePassword(@PathVariable id: String, @RequestBody request: UpdatePasswordRequest): UserResponse

    @Operation(
        summary = "Delete user",
        responses = [
            ApiResponse(responseCode = "204", description = "User deleted"),
            ApiResponse(responseCode = "400", description = "Malformed user ID"),
        ]
    )
    @DeleteMapping("${ApiPathsV1.USERS}/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun delete(@PathVariable id: String)
}
