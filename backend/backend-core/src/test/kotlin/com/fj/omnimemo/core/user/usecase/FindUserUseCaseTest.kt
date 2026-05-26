/*
 * FindUserUseCaseTest.kt
 *
 * $Since: 2026-05-25T00:00:00Z
 */
package com.fj.omnimemo.core.user.usecase

import com.fj.omnimemo.core.test.annotation.SmallTest
import com.fj.omnimemo.core.user.model.UserId
import com.fj.omnimemo.core.user.repository.MockUserRepository
import com.fj.omnimemo.core.user.security.MockPasswordHasher
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

@SmallTest
class FindUserUseCaseTest {

    private val repo = MockUserRepository()
    private val createUseCase = CreateUserUseCase(repo, MockPasswordHasher())
    private val useCase = FindUserUseCase(repo)

    @BeforeEach
    fun setUp() = repo.clear()

    @Nested
    inner class FindById {
        @Test
        fun `should return user when user exists`() {
            val user = createUseCase.create("alice@example.com", "secret")

            useCase.findById(user.id) shouldBe user
        }

        @Test
        fun `should return null when user does not exist`() {
            useCase.findById(UserId.generate()) shouldBe null
        }
    }

    @Nested
    inner class FindByEmail {
        @Test
        fun `should return user when email matches`() {
            createUseCase.create("alice@example.com", "secret")

            useCase.findByEmail("alice@example.com") shouldNotBe null
        }

        @Test
        fun `should return null when email does not match`() {
            useCase.findByEmail("nobody@example.com") shouldBe null
        }
    }
}
