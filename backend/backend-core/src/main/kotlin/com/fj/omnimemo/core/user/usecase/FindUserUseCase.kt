/*
 * FindUserUseCase.kt
 *
 * $Since: 2026-05-25T00:00:00Z
 */
package com.fj.omnimemo.core.user.usecase

import com.fj.omnimemo.core.user.model.User
import com.fj.omnimemo.core.user.model.UserId
import com.fj.omnimemo.core.user.repository.UserRepository

/**
 * Retrieves a user by identity or email address.
 *
 * @author Francesco Jo
 * @since 0.1.1
 * @version 0.1.1
 */
class FindUserUseCase(
    private val repository: UserRepository,
) {
    fun findById(id: UserId): User? = repository.findById(id)
    fun findByEmail(email: String): User? = repository.findByEmail(email)
}
