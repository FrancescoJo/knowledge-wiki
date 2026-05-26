/*
 * DeleteUserUseCase.kt
 *
 * $Since: 2026-05-25T00:00:00Z
 */
package com.fj.omnimemo.core.user.usecase

import com.fj.omnimemo.core.user.exception.UserNotFoundException
import com.fj.omnimemo.core.user.model.UserId
import com.fj.omnimemo.core.user.repository.UserRepository

/**
 * Removes a user account from the system.
 *
 * Throws [UserNotFoundException] when no user exists for the given [UserId].
 *
 * @author Francesco Jo
 * @since 0.1.1
 * @version 0.1.1
 */
class DeleteUserUseCase(
    private val repository: UserRepository,
) {
    fun delete(id: UserId) {
        repository.findById(id) ?: throw UserNotFoundException(id)
        repository.delete(id)
    }
}
