/*
 * UpdateUserPasswordUseCaseTest.kt
 *
 * $Since: 2026-05-25T00:00:00Z
 */
package testcase.small.core.user.usecase

import com.fj.omnimemo.core.user.exception.UserNotFoundException
import com.fj.omnimemo.core.user.model.UserId
import com.fj.omnimemo.core.user.usecase.CreateUserUseCase
import com.fj.omnimemo.core.user.usecase.UpdateUserPasswordUseCase
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
class UpdateUserPasswordUseCaseTest {
    private lateinit var repo: MockUserRepository
    private lateinit var hasher: MockPasswordHasher
    private lateinit var createUseCase: CreateUserUseCase
    private lateinit var sut: UpdateUserPasswordUseCase

    @BeforeEach
    fun setUp() {
        repo = MockUserRepository()
        hasher = MockPasswordHasher()
        createUseCase = CreateUserUseCase(repo, hasher)
        sut = UpdateUserPasswordUseCase(repo, hasher)
    }

    @AfterEach
    fun tearDown() {
        repo.clear()
    }

    @Nested
    inner class UpdatePassword {

        @Test
        fun `should return user with updated password hash`() {
            val user = createUseCase.create("alice@example.com", "old-secret")

            val updated = sut.updatePassword(user.id, "new-secret")

            updated.passwordHash shouldBe "hashed:new-secret"
        }

        @Test
        fun `should throw UserNotFoundException when user does not exist`() {
            shouldThrow<UserNotFoundException> {
                sut.updatePassword(UserId.generate(), "new-secret")
            }
        }
    }
}
