/*
 * RefreshTokenRepositoryImplTest.kt
 *
 * $Since: 2026-05-26T00:00:00Z
 */
package com.fj.omnimemo.infrastructure.user.persistence

import com.fj.omnimemo.core.test.annotation.MediumTest
import com.fj.omnimemo.core.user.model.RefreshToken
import com.fj.omnimemo.core.user.model.User
import com.fj.omnimemo.core.user.model.UserId
import com.fj.omnimemo.infrastructure.security.AesGcmCipher
import com.fj.omnimemo.infrastructure.security.HmacBlindIndex
import com.fj.omnimemo.infrastructure.test.InfrastructureTestDatabase
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.time.Instant
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

/**
 * Medium Tests for [RefreshTokenRepositoryImpl]: verifies persistence behaviour
 * against a real PostgreSQL instance shared via [InfrastructureTestDatabase].
 *
 * Each test starts with a clean state: all users (and their tokens via FK cascade)
 * are deleted, then a single test user is inserted to satisfy the FK constraint.
 *
 * @author Francesco Jo
 * @since 0.1.1
 * @version 0.1.1
 */
@MediumTest
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class RefreshTokenRepositoryImplTest {

    private val jdbc = InfrastructureTestDatabase.jdbc
    private val userRepo = UserRepositoryImpl(jdbc, TEST_AES_CIPHER, TEST_HMAC_INDEX)
    private val repo = RefreshTokenRepositoryImpl(jdbc)

    private lateinit var testUser: User

    @BeforeEach
    fun setUp() {
        jdbc.update("DELETE FROM users") // cascades to refresh_tokens via FK ON DELETE CASCADE
        testUser = User.create("alice@example.com", "hash")
        userRepo.save(testUser)
    }

    @Test
    fun `save should persist the refresh token`() {
        val token = newToken(testUser.id)
        repo.save(token)

        val found = repo.findByToken(token.token)

        assertNotNull(found)
        assertEquals(token.token, found.token)
        assertEquals(testUser.id, found.userId)
    }

    @Nested
    inner class FindByToken {
        @Test
        fun `should return token when it exists`() {
            val token = newToken(testUser.id)
            repo.save(token)

            val found = repo.findByToken(token.token)

            assertNotNull(found)
            assertEquals(token.token, found.token)
            assertEquals(testUser.id, found.userId)
        }

        @Test
        fun `should return null when token does not exist`() {
            assertNull(repo.findByToken("no-such-token"))
        }
    }

    @Nested
    inner class Delete {
        @Test
        fun `should remove the token`() {
            val token = newToken(testUser.id)
            repo.save(token)

            repo.delete(token.token)

            assertNull(repo.findByToken(token.token))
        }

        @Test
        fun `should be removed when the owning user is deleted`() {
            val token = newToken(testUser.id)
            repo.save(token)

            userRepo.delete(testUser.id)

            assertNull(repo.findByToken(token.token))
        }
    }

    companion object {
        private val TEST_AES_CIPHER = AesGcmCipher(ByteArray(32) { it.toByte() })
        private val TEST_HMAC_INDEX = HmacBlindIndex(ByteArray(32) { (it + 32).toByte() })

        private fun newToken(userId: UserId): RefreshToken = RefreshToken.create(
            token = UUID.randomUUID().toString(),
            userId = userId,
            expiresAt = Instant.now().plusSeconds(3600),
            createdAt = Instant.now(),
        )
    }
}
