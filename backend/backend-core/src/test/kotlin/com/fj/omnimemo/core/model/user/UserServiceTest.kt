/*
 * UserServiceTest.kt
 *
 * $Since: 2026-05-25T00:00:00Z
 */
package com.fj.omnimemo.core.model.user

import com.fj.omnimemo.core.test.annotation.SmallTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

@SmallTest
class UserServiceTest {

    private val repo = MockUserRepository()
    private val hasher = MockPasswordHasher()
    private val service = UserService(repo, hasher)

    @BeforeEach
    fun setUp() = repo.clear()

    @Nested
    inner class Create {
        @Test
        fun `should persist a new user with a hashed password`() {
            val user = service.create("alice@example.com", "secret")

            assertNotNull(repo.findById(user.id))
            assertEquals("hashed:secret", user.passwordHash)
        }
    }

    @Nested
    inner class FindById {
        @Test
        fun `should return user when user exists`() {
            val user = service.create("alice@example.com", "secret")

            assertEquals(user, service.findById(user.id))
        }

        @Test
        fun `should return null when user does not exist`() {
            assertNull(service.findById(UserId.generate()))
        }
    }

    @Nested
    inner class FindByEmail {
        @Test
        fun `should return user when email matches`() {
            service.create("alice@example.com", "secret")

            assertNotNull(service.findByEmail("alice@example.com"))
        }

        @Test
        fun `should return null when email does not match`() {
            assertNull(service.findByEmail("nobody@example.com"))
        }
    }

    @Nested
    inner class UpdateEmail {
        @Test
        fun `should return user with updated email`() {
            val user = service.create("alice@example.com", "secret")

            val updated = service.updateEmail(user.id, "new@example.com")

            assertEquals("new@example.com", updated?.email)
        }

        @Test
        fun `should return null when user does not exist`() {
            assertNull(service.updateEmail(UserId.generate(), "new@example.com"))
        }
    }

    @Nested
    inner class UpdatePassword {
        @Test
        fun `should return user with updated password hash`() {
            val user = service.create("alice@example.com", "old-secret")

            val updated = service.updatePassword(user.id, "new-secret")

            assertEquals("hashed:new-secret", updated?.passwordHash)
        }

        @Test
        fun `should return null when user does not exist`() {
            assertNull(service.updatePassword(UserId.generate(), "new-secret"))
        }
    }

    @Nested
    inner class Delete {
        @Test
        fun `should remove user from repository`() {
            val user = service.create("alice@example.com", "secret")
            service.delete(user.id)

            assertNull(service.findById(user.id))
        }
    }
}
