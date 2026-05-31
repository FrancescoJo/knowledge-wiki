/*
 * MockUserRepository.kt
 *
 * $Since: 2026-05-23T00:00:00Z
 */
package test.com.fj.omnimemo.core.user.repository

import com.fj.omnimemo.core.user.model.User
import com.fj.omnimemo.core.user.model.UserId
import com.fj.omnimemo.core.user.repository.UserRepository
import test.com.fj.omnimemo.core.repository.AbstractMockRepository

/**
 * In-memory fake of [UserRepository] for use in Small tests.
 *
 * @author Francesco Jo
 * @since 0.1.1
 * @version 0.2.0
 */
class MockUserRepository : AbstractMockRepository<UserId, User>({ it.id }), UserRepository {
    override fun findById(id: UserId): User? = doFindByKey(id)

    override fun findByEmail(email: String): User? = store.values.find { it.email == email }

    override fun save(user: User): User = doSave(user)

    override fun delete(id: UserId) = doDeleteByKey(id)

    override fun hasAny(): Boolean = store.isNotEmpty()
}
