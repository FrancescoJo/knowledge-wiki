/*
 * UpdateUserPasswordUseCaseTest.kt
 *
 * $Since: 2026-05-25T00:00:00Z
 */
package com.fj.omnimemo.core.user.usecase

import com.fj.omnimemo.core.test.annotation.SmallTest
import com.fj.omnimemo.core.user.exception.UserNotFoundException
import com.fj.omnimemo.core.user.model.UserId
import com.fj.omnimemo.core.user.repository.MockUserRepository
import com.fj.omnimemo.core.user.security.MockPasswordHasher
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

@SmallTest
class UpdateUserPasswordUseCaseTest {

    private val repo = MockUserRepository()
    private val hasher = MockPasswordHasher()
    private val createUseCase = CreateUserUseCase(repo, hasher)
    private val useCase = UpdateUserPasswordUseCase(repo, hasher)

    @BeforeEach
    fun setUp() = repo.clear()

    @Nested
    inner class UpdatePassword {

        @Test
        fun `should return user with updated password hash`() {
            val user = createUseCase.create("alice@example.com", "old-secret")

            val updated = useCase.updatePassword(user.id, "new-secret")

            updated.passwordHash shouldBe "hashed:new-secret"
        }

        @Test
        fun `should throw UserNotFoundException when user does not exist`() {
            shouldThrow<UserNotFoundException> {
                useCase.updatePassword(UserId.generate(), "new-secret")
            }
        }
    }
}
