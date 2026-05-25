/*
 * UserService.kt
 *
 * $Since: 2026-05-25T00:00:00Z
 */
package com.fj.omnimemo.core.model.user

/**
 * Application service for User CRUD operations.
 *
 * Raw passwords are accepted only at creation and password-update boundaries;
 * all other operations work with the already-hashed [User.passwordHash].
 *
 * @author Francesco Jo
 * @since 0.1.1
 * @version 0.1.1
 */
class UserService(
    private val repository: UserRepository,
    private val hasher: PasswordHasher,
) {

    fun create(email: String, rawPassword: String): User =
        repository.save(User.create(email, hasher.hash(rawPassword)))

    fun findById(id: UserId): User? = repository.findById(id)

    fun findByEmail(email: String): User? = repository.findByEmail(email)

    fun updateEmail(id: UserId, newEmail: String): User? {
        val user = repository.findById(id) ?: return null
        val mutator = user.mutate()
        mutator.email = newEmail
        return repository.save(mutator)
    }

    fun updatePassword(id: UserId, newRawPassword: String): User? {
        val user = repository.findById(id) ?: return null
        val mutator = user.mutate()
        mutator.passwordHash = hasher.hash(newRawPassword)
        return repository.save(mutator)
    }

    fun delete(id: UserId) = repository.delete(id)
}
