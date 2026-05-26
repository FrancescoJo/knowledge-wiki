/*
 * BootstrapControllerImpl.kt
 *
 * $Since: 2026-05-26T00:00:00Z
 */
package com.fj.omnimemo.api.endpoint.bootstrap.impl

import com.fj.omnimemo.api.endpoint.bootstrap.BootstrapController
import com.fj.omnimemo.api.endpoint.bootstrap.dto.request.BootstrapUserRequest
import com.fj.omnimemo.api.endpoint.user.dto.response.UserResponse
import com.fj.omnimemo.api.endpoint.user.dto.response.toResponse
import com.fj.omnimemo.core.user.usecase.BootstrapUserUseCase
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ResponseStatusException

/**
 * @author Francesco Jo
 * @since 0.1.1
 * @version 0.1.1
 */
@RestController
internal class BootstrapControllerImpl(
    private val bootstrapUserUseCase: BootstrapUserUseCase,
) : BootstrapController {

    override fun bootstrapUser(request: BootstrapUserRequest): UserResponse =
        bootstrapUserUseCase.bootstrap(request.email, request.password)?.toResponse()
            ?: throw ResponseStatusException(HttpStatus.CONFLICT)
}
