/*
 * UserControllerImpl.kt
 *
 * $Since: 2026-05-26T00:00:00Z
 */
package com.fj.omnimemo.api.endpoint.user.impl

import com.fj.omnimemo.api.endpoint.user.UserController
import com.fj.omnimemo.api.endpoint.user.dto.request.CreateUserRequest
import com.fj.omnimemo.api.endpoint.user.dto.response.UserResponse
import com.fj.omnimemo.api.endpoint.user.dto.response.toResponse
import com.fj.omnimemo.core.user.UserProfileCache
import com.fj.omnimemo.core.user.model.UserId
import com.fj.omnimemo.core.user.usecase.CreateUserUseCase
import com.fj.omnimemo.core.user.usecase.DeleteUserUseCase
import com.fj.omnimemo.core.user.usecase.FindUserUseCase
import com.fj.omnimemo.core.util.parseUuidOrNull
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
    private val deleteUserUseCase: DeleteUserUseCase,
    private val userProfileCache: UserProfileCache,
) : UserController {
    override fun findById(id: String): UserResponse {
        val user = findUserUseCase.findById(parseUserId(id))
            ?: throw ResponseStatusException(HttpStatus.NOT_FOUND)
        return user.toResponse()
    }

    override fun create(request: CreateUserRequest): UserResponse =
        createUserUseCase.create(request.email, request.password).toResponse()

    override fun delete(id: String) {
        val userId = parseUserId(id)
        deleteUserUseCase.delete(userId)
        userProfileCache.invalidate(userId)
    }

    private fun parseUserId(raw: String): UserId =
        parseUuidOrNull(raw)?.let { UserId(it) }
            ?: throw ResponseStatusException(HttpStatus.BAD_REQUEST)
}
