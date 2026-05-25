/*
 * UserApiController.kt
 *
 * $Since: 2026-05-25T00:00:00Z
 */
package com.fj.omnimemo.api.endpoint.user

import com.fj.omnimemo.core.user.model.UserId
import com.fj.omnimemo.core.user.usecase.CreateUserUseCase
import com.fj.omnimemo.core.user.usecase.DeleteUserUseCase
import com.fj.omnimemo.core.user.usecase.FindUserUseCase
import com.fj.omnimemo.core.user.usecase.UpdateUserEmailUseCase
import com.fj.omnimemo.core.user.usecase.UpdateUserPasswordUseCase
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ResponseStatusException
import java.util.UUID

/**
 * REST endpoints for user account management.
 *
 * All endpoints require authentication; access is governed by the
 * [com.fj.omnimemo.api.config.SecurityConfiguration] filter chain.
 *
 * @author Francesco Jo
 * @since 0.1.1
 * @version 0.1.1
 */
@RestController
@RequestMapping("/api/users")
class UserApiController(
    private val createUserUseCase: CreateUserUseCase,
    private val findUserUseCase: FindUserUseCase,
    private val updateUserEmailUseCase: UpdateUserEmailUseCase,
    private val updateUserPasswordUseCase: UpdateUserPasswordUseCase,
    private val deleteUserUseCase: DeleteUserUseCase,
) {

    @GetMapping("/{id}")
    fun findById(@PathVariable id: String): UserResponse =
        findUserUseCase.findById(parseUserId(id))?.toResponse()
            ?: throw ResponseStatusException(HttpStatus.NOT_FOUND)

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun create(@RequestBody request: CreateUserRequest): UserResponse =
        createUserUseCase.create(request.email, request.password).toResponse()

    @PutMapping("/{id}/email")
    fun updateEmail(@PathVariable id: String, @RequestBody request: UpdateEmailRequest): UserResponse =
        updateUserEmailUseCase.updateEmail(parseUserId(id), request.email)?.toResponse()
            ?: throw ResponseStatusException(HttpStatus.NOT_FOUND)

    @PutMapping("/{id}/password")
    fun updatePassword(@PathVariable id: String, @RequestBody request: UpdatePasswordRequest): UserResponse =
        updateUserPasswordUseCase.updatePassword(parseUserId(id), request.password)?.toResponse()
            ?: throw ResponseStatusException(HttpStatus.NOT_FOUND)

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun delete(@PathVariable id: String) =
        deleteUserUseCase.delete(parseUserId(id))

    private fun parseUserId(raw: String): UserId =
        try { UserId(UUID.fromString(raw)) }
        catch (e: IllegalArgumentException) { throw ResponseStatusException(HttpStatus.BAD_REQUEST) }
}
