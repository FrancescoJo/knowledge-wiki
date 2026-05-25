/*
 * UpdateUserEmailUseCase.kt
 *
 * $Since: 2026-05-25T00:00:00Z
 */
package com.fj.omnimemo.core.user.usecase

import com.fj.omnimemo.core.user.model.User
import com.fj.omnimemo.core.user.model.UserId
import com.fj.omnimemo.core.user.model.UserRepository
import com.fj.omnimemo.core.user.model.mutate

/**
 * Updates the email address of an existing user.
 *
 * Returns [null] if no user exists for the given [UserId].
 *
 * @author Francesco Jo
 * @since 0.1.1
 * @version 0.1.1
 */
class UpdateUserEmailUseCase(
    private val repository: UserRepository,
) {
    fun updateEmail(id: UserId, newEmail: String): User? {
        val user = repository.findById(id) ?: return null
        val mutator = user.mutate()
        mutator.email = newEmail
        return repository.save(mutator)
    }
}
