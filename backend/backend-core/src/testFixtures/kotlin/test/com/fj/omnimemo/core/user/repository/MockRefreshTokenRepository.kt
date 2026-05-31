/*
 * MockRefreshTokenRepository.kt
 *
 * $Since: 2026-05-25T00:00:00Z
 */
package test.com.fj.omnimemo.core.user.repository

import com.fj.omnimemo.core.user.model.RefreshToken
import com.fj.omnimemo.core.user.repository.RefreshTokenRepository
import test.com.fj.omnimemo.core.repository.AbstractMockRepository

/**
 * In-memory fake of [RefreshTokenRepository] for use in Small tests.
 *
 * @author Francesco Jo
 * @since 0.1.1
 * @version 0.2.0
 */
class MockRefreshTokenRepository : AbstractMockRepository<String, RefreshToken>({ it.token }), RefreshTokenRepository {
    override fun save(refreshToken: RefreshToken): RefreshToken = doSave(refreshToken)

    override fun findByToken(token: String): RefreshToken? = doFindByKey(token)

    override fun delete(token: String) = doDeleteByKey(token)
}
