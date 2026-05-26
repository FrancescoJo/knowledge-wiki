/*
 * CreateUserUseCaseTest.kt
 *
 * $Since: 2026-05-25T00:00:00Z
 */
package com.fj.omnimemo.core.user.usecase

import com.fj.omnimemo.core.test.annotation.SmallTest
import com.fj.omnimemo.core.user.repository.MockUserRepository
import com.fj.omnimemo.core.user.security.MockPasswordHasher
import io.kotest.assertions.assertSoftly
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@SmallTest
class CreateUserUseCaseTest {

    private val repo = MockUserRepository()
    private val hasher = MockPasswordHasher()
    private val useCase = CreateUserUseCase(repo, hasher)

    @BeforeEach
    fun setUp() = repo.clear()

    @Test
    fun `should persist a new user with a hashed password`() {
        val user = useCase.create("alice@example.com", "secret")

        assertSoftly {
            repo.findById(user.id) shouldNotBe null
            user.passwordHash shouldBe "hashed:secret"
        }
    }
}
