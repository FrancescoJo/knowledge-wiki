/*
 * DeleteUserUseCaseTest.kt
 *
 * $Since: 2026-05-25T00:00:00Z
 */
package testcase.small.core.user.usecase

import com.fj.omnimemo.core.user.exception.UserNotFoundException
import com.fj.omnimemo.core.user.model.UserId
import com.fj.omnimemo.core.user.usecase.CreateUserUseCase
import com.fj.omnimemo.core.user.usecase.DeleteUserUseCase
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import test.com.fj.omnimemo.core.annotation.SmallTest
import test.com.fj.omnimemo.core.user.repository.MockUserRepository
import test.com.fj.omnimemo.core.user.security.MockPasswordHasher

@SmallTest
class DeleteUserUseCaseTest {
    private lateinit var repo: MockUserRepository
    private lateinit var createUseCase: CreateUserUseCase
    private lateinit var sut: DeleteUserUseCase

    @BeforeEach
    fun setUp() {
        repo = MockUserRepository()
        createUseCase = CreateUserUseCase(repo, MockPasswordHasher())
        sut = DeleteUserUseCase(repo)
    }

    @AfterEach
    fun tearDown() {
        repo.clear()
    }

    @Nested
    inner class Delete {

        @Test
        fun `should remove user from repository`() {
            val user = createUseCase.create("alice@example.com", "secret")
            sut.delete(user.id)

            repo.findById(user.id) shouldBe null
        }

        @Test
        fun `should throw UserNotFoundException when user does not exist`() {
            shouldThrow<UserNotFoundException> {
                sut.delete(UserId.generate())
            }
        }
    }
}
