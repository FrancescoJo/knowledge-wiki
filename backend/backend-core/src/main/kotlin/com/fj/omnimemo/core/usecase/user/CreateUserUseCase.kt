/*
 * CreateUserUseCase.kt
 *
 * $Since: 2026-05-25T00:00:00Z
 */
package com.fj.omnimemo.core.usecase.user

import com.fj.omnimemo.core.model.user.PasswordHasher
import com.fj.omnimemo.core.model.user.User
import com.fj.omnimemo.core.model.user.UserRepository

/**
 * Creates a new user account.
 *
 * The raw password is accepted only here; [User.passwordHash] stores the
 * hashed result. Encryption of stored fields is an infrastructure concern.
 *
 * @author Francesco Jo
 * @since 0.1.1
 * @version 0.1.1
 */
class CreateUserUseCase(
    private val repository: UserRepository,
    private val hasher: PasswordHasher,
) {
    fun create(email: String, rawPassword: String): User =
        repository.save(User.create(email, hasher.hash(rawPassword)))
}
