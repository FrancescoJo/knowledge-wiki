/*
 * UserRepository.kt
 *
 * $Since: 2026-05-23T00:00:00Z
 */
package com.fj.omnimemo.core.user.repository

import com.fj.omnimemo.core.user.model.User
import com.fj.omnimemo.core.user.model.UserId

/**
 * Persistence contract for [User] entities.
 *
 * All methods operate on plaintext [User.email]. Encryption of the stored
 * email is the sole responsibility of the implementing infrastructure class.
 *
 * @author Francesco Jo
 * @since 0.1.1
 * @version 0.1.1
 */
interface UserRepository {
    fun findById(id: UserId): User?
    fun findByEmail(email: String): User?
    fun save(user: User): User
    fun delete(id: UserId)
}
