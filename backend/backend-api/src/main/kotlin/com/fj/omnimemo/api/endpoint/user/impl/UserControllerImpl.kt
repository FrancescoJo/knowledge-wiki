/*
 * UserControllerImpl.kt
 *
 * $Since: 2026-05-26T00:00:00Z
 */
package com.fj.omnimemo.api.endpoint.user.impl

import com.fj.omnimemo.api.endpoint.user.UserController
import com.fj.omnimemo.api.endpoint.user.dto.request.CreateUserRequest
import com.fj.omnimemo.api.endpoint.user.dto.request.UpdateEmailRequest
import com.fj.omnimemo.api.endpoint.user.dto.request.UpdatePasswordRequest
import com.fj.omnimemo.api.endpoint.user.dto.response.UserResponse
import com.fj.omnimemo.api.endpoint.user.dto.response.toResponse
import com.fj.omnimemo.core.user.model.UserId
import com.fj.omnimemo.core.user.usecase.CreateUserUseCase
import com.fj.omnimemo.core.util.parseUuidOrNull
import com.fj.omnimemo.core.user.usecase.DeleteUserUseCase
import com.fj.omnimemo.core.user.usecase.FindUserUseCase
import com.fj.omnimemo.core.user.usecase.UpdateUserEmailUseCase
import com.fj.omnimemo.core.user.usecase.UpdateUserPasswordUseCase
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ResponseStatusException

/**
 * @author Francesco Jo
 * @since 0.1.1
 * @version 0.1.1
 */
@RestController
internal class UserControllerImpl(
    private val createUserUseCase: CreateUserUseCase,
    private val findUserUseCase: FindUserUseCase,
    private val updateUserEmailUseCase: UpdateUserEmailUseCase,
    private val updateUserPasswordUseCase: UpdateUserPasswordUseCase,
    private val deleteUserUseCase: DeleteUserUseCase,
) : UserController {

    override fun findById(id: String): UserResponse =
        findUserUseCase.findById(parseUserId(id))?.toResponse()
            ?: throw ResponseStatusException(HttpStatus.NOT_FOUND)

    override fun create(request: CreateUserRequest): UserResponse =
        createUserUseCase.create(request.email, request.password).toResponse()

    override fun updateEmail(id: String, request: UpdateEmailRequest): UserResponse =
        updateUserEmailUseCase.updateEmail(parseUserId(id), request.email).toResponse()

    override fun updatePassword(id: String, request: UpdatePasswordRequest): UserResponse =
        updateUserPasswordUseCase.updatePassword(parseUserId(id), request.password).toResponse()

    override fun delete(id: String) =
        deleteUserUseCase.delete(parseUserId(id))

    private fun parseUserId(raw: String): UserId =
        parseUuidOrNull(raw)?.let { UserId(it) }
            ?: throw ResponseStatusException(HttpStatus.BAD_REQUEST)
}
