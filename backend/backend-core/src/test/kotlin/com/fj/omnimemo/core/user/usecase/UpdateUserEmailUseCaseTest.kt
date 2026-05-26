/*
 * UpdateUserEmailUseCaseTest.kt
 *
 * $Since: 2026-05-25T00:00:00Z
 */
package com.fj.omnimemo.core.user.usecase

import com.fj.omnimemo.core.user.exception.UserNotFoundException
import com.fj.omnimemo.core.test.annotation.SmallTest
import com.fj.omnimemo.core.user.model.UserId
import com.fj.omnimemo.core.user.repository.MockUserRepository
import com.fj.omnimemo.core.user.security.MockPasswordHasher
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

@SmallTest
class UpdateUserEmailUseCaseTest {

    private val repo = MockUserRepository()
    private val createUseCase = CreateUserUseCase(repo, MockPasswordHasher())
    private val useCase = UpdateUserEmailUseCase(repo)

    @BeforeEach
    fun setUp() = repo.clear()

    @Nested
    inner class UpdateEmail {

        @Test
        fun `should return user with updated email`() {
            val user = createUseCase.create("alice@example.com", "secret")

            val updated = useCase.updateEmail(user.id, "new@example.com")

            updated.email shouldBe "new@example.com"
        }

        @Test
        fun `should throw UserNotFoundException when user does not exist`() {
            shouldThrow<UserNotFoundException> {
                useCase.updateEmail(UserId.generate(), "new@example.com")
            }
        }
    }
}
