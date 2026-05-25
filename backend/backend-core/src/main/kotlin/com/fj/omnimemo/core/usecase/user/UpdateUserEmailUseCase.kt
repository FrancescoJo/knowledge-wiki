/*
 * UpdateUserEmailUseCase.kt
 *
 * $Since: 2026-05-25T00:00:00Z
 */
package com.fj.omnimemo.core.usecase.user

import com.fj.omnimemo.core.model.user.User
import com.fj.omnimemo.core.model.user.UserId
import com.fj.omnimemo.core.model.user.UserRepository
import com.fj.omnimemo.core.model.user.mutate

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
