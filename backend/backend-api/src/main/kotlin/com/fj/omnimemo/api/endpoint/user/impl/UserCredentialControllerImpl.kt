/*
 * UserCredentialControllerImpl.kt
 *
 * $Since: 2026-05-30T00:00:00Z
 */
package com.fj.omnimemo.api.endpoint.user.impl

import com.fj.omnimemo.api.endpoint.user.UserCredentialController
import com.fj.omnimemo.api.endpoint.user.dto.request.UpdateEmailRequest
import com.fj.omnimemo.api.endpoint.user.dto.request.UpdatePasswordRequest
import com.fj.omnimemo.api.endpoint.user.dto.response.UserResponse
import com.fj.omnimemo.api.endpoint.user.dto.response.toResponse
import com.fj.omnimemo.core.user.UserProfileCache
import com.fj.omnimemo.core.user.model.UserId
import com.fj.omnimemo.core.user.usecase.UpdateUserEmailUseCase
import com.fj.omnimemo.core.user.usecase.UpdateUserPasswordUseCase
import com.fj.omnimemo.core.util.parseUuidOrNull
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ResponseStatusException

/**
 * @author Francesco Jo
 * @since 0.2.0
 * @version 0.2.0
 */
@RestController
internal class UserCredentialControllerImpl(
    private val updateUserEmailUseCase: UpdateUserEmailUseCase,
    private val updateUserPasswordUseCase: UpdateUserPasswordUseCase,
    private val userProfileCache: UserProfileCache,
) : UserCredentialController {
    override fun updateEmail(id: String, request: UpdateEmailRequest): UserResponse {
        val userId = parseUserId(id)
        val updated = updateUserEmailUseCase.updateEmail(userId, request.email)
        userProfileCache.invalidate(userId)
        return updated.toResponse()
    }

    override fun updatePassword(id: String, request: UpdatePasswordRequest): UserResponse =
        updateUserPasswordUseCase.updatePassword(parseUserId(id), request.password).toResponse()

    private fun parseUserId(raw: String): UserId =
        parseUuidOrNull(raw)?.let { UserId(it) }
            ?: throw ResponseStatusException(HttpStatus.BAD_REQUEST)
}
