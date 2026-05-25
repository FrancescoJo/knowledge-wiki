/*
 * UserRepositoryTest.kt
 *
 * $Since: 2026-05-23T00:00:00Z
 */
package com.fj.omnimemo.core.user.model

import com.fj.omnimemo.core.test.annotation.SmallTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

/**
 * Small tests for [UserRepository] contract using [MockUserRepository].
 *
 * @author Francesco Jo
 * @since 0.1.1
 * @version 0.1.1
 */
@SmallTest
class UserRepositoryTest {

    private val repo = MockUserRepository()

    @BeforeEach
    fun setUp() = repo.clear()

    @Nested
    inner class FindById {
        @Test
        fun `should return user when user exists`() {
            val user = User.create("alice@example.com", "hash")
            repo.save(user)

            assertEquals(user, repo.findById(user.id))
        }

        @Test
        fun `should return null when user does not exist`() {
            assertNull(repo.findById(UserId.generate()))
        }
    }

    @Nested
    inner class FindByEmail {
        @Test
        fun `should return user when email matches`() {
            val user = User.create("alice@example.com", "hash")
            repo.save(user)

            assertNotNull(repo.findByEmail("alice@example.com"))
        }

        @Test
        fun `should return null when email does not match`() {
            assertNull(repo.findByEmail("nobody@example.com"))
        }
    }

    @Test
    fun `delete should remove user when user exists`() {
        val user = User.create("alice@example.com", "hash")
        repo.save(user)
        repo.delete(user.id)

        assertNull(repo.findById(user.id))
    }

    @Test
    fun `save should return the saved user`() {
        val user = User.create("alice@example.com", "hash")
        val result = repo.save(user)

        assertEquals(user.id, result.id)
        assertEquals(user.email, result.email)
    }
}
