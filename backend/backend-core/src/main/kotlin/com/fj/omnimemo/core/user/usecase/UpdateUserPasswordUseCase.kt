/*
 * UpdateUserPasswordUseCase.kt
 *
 * $Since: 2026-05-25T00:00:00Z
 */
package com.fj.omnimemo.core.user.usecase

import com.fj.omnimemo.core.user.model.User
import com.fj.omnimemo.core.user.model.UserId
import com.fj.omnimemo.core.user.mutate
import com.fj.omnimemo.core.user.repository.UserRepository
import com.fj.omnimemo.core.user.security.PasswordHasher

/**
 * Updates the password of an existing user.
 *
 * The raw password is accepted here; hashing is applied before persistence.
 * Returns [null] if no user exists for the given [UserId].
 *
 * @author Francesco Jo
 * @since 0.1.1
 * @version 0.1.1
 */
class UpdateUserPasswordUseCase(
    private val repository: UserRepository,
    private val hasher: PasswordHasher,
) {
    fun updatePassword(id: UserId, newRawPassword: String): User? {
        val user = repository.findById(id) ?: return null
        val mutator = user.mutate()
        mutator.passwordHash = hasher.hash(newRawPassword)
        return repository.save(mutator)
    }
}
