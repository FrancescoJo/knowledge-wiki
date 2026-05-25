/*
 * MockUserRepository.kt
 *
 * $Since: 2026-05-23T00:00:00Z
 */
package com.fj.omnimemo.core.user.model

/**
 * In-memory fake of [UserRepository] for use in Small tests.
 *
 * Stores entities in a plain [MutableMap]. Not thread-safe; construct a
 * fresh instance per test class.
 *
 * @author Francesco Jo
 * @since 0.1.1
 * @version 0.1.1
 */
class MockUserRepository : UserRepository {

    private val store = mutableMapOf<UserId, User>()

    override fun findById(id: UserId): User? = store[id]

    override fun findByEmail(email: String): User? = store.values.find { it.email == email }

    override fun save(user: User): User = user.also { store[user.id] = user }

    override fun delete(id: UserId) { store.remove(id) }

    fun clear() = store.clear()
}
