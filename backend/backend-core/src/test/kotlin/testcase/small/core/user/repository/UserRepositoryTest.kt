/*
 * UserRepositoryTest.kt
 *
 * $Since: 2026-05-23T00:00:00Z
 */
package testcase.small.core.user.repository

import com.fj.omnimemo.core.user.model.User
import com.fj.omnimemo.core.user.model.UserId
import io.kotest.assertions.assertSoftly
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import test.com.fj.omnimemo.core.annotation.SmallTest
import test.com.fj.omnimemo.core.user.repository.MockUserRepository

/**
 * Small tests for [UserRepository] contract using [MockUserRepository].
 *
 * @author Francesco Jo
 * @since 0.1.1
 * @version 0.1.1
 */
@SmallTest
class UserRepositoryTest {
    private lateinit var repo: MockUserRepository

    @BeforeEach
    fun setUp() {
        repo = MockUserRepository()
    }

    @AfterEach
    fun tearDown() {
        repo.clear()
    }

    @Nested
    inner class FindById {
        @Test
        fun `should return user when user exists`() {
            val user = User.create("alice@example.com", "hash")
            repo.save(user)

            repo.findById(user.id) shouldBe user
        }

        @Test
        fun `should return null when user does not exist`() {
            repo.findById(UserId.generate()) shouldBe null
        }
    }

    @Nested
    inner class FindByEmail {
        @Test
        fun `should return user when email matches`() {
            val user = User.create("alice@example.com", "hash")
            repo.save(user)

            repo.findByEmail("alice@example.com") shouldNotBe null
        }

        @Test
        fun `should return null when email does not match`() {
            repo.findByEmail("nobody@example.com") shouldBe null
        }
    }

    @Test
    fun `delete should remove user when user exists`() {
        val user = User.create("alice@example.com", "hash")
        repo.save(user)
        repo.delete(user.id)

        repo.findById(user.id) shouldBe null
    }

    @Test
    fun `save should return the saved user`() {
        val user = User.create("alice@example.com", "hash")
        val result = repo.save(user)

        assertSoftly {
            result.id shouldBe user.id
            result.email shouldBe user.email
        }
    }
}
