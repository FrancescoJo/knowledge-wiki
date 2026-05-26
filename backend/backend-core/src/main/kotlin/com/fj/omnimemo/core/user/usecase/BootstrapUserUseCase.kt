/*
 * BootstrapUserUseCase.kt
 *
 * $Since: 2026-05-26T00:00:00Z
 */
package com.fj.omnimemo.core.user.usecase

import com.fj.omnimemo.core.user.model.User
import com.fj.omnimemo.core.user.repository.UserRepository
import com.fj.omnimemo.core.user.security.PasswordHasher

/**
 * Creates the first user account when the repository is empty.
 *
 * Returns null when at least one user already exists. Intended for the
 * bootstrap flow only; normal user creation goes through [CreateUserUseCase].
 *
 * @author Francesco Jo
 * @since 0.1.1
 * @version 0.1.1
 */
class BootstrapUserUseCase(
    private val repository: UserRepository,
    private val hasher: PasswordHasher,
) {
    fun bootstrap(email: String, rawPassword: String): User? {
        if (repository.hasAny()) return null
        return repository.save(User.create(email, hasher.hash(rawPassword)))
    }
}
