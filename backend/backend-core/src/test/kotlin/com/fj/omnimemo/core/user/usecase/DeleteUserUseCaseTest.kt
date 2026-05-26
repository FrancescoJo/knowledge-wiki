/*
 * DeleteUserUseCaseTest.kt
 *
 * $Since: 2026-05-25T00:00:00Z
 */
package com.fj.omnimemo.core.user.usecase

import com.fj.omnimemo.core.test.annotation.SmallTest
import com.fj.omnimemo.core.user.repository.MockUserRepository
import com.fj.omnimemo.core.user.security.MockPasswordHasher
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@SmallTest
class DeleteUserUseCaseTest {

    private val repo = MockUserRepository()
    private val createUseCase = CreateUserUseCase(repo, MockPasswordHasher())
    private val useCase = DeleteUserUseCase(repo)

    @BeforeEach
    fun setUp() = repo.clear()

    @Test
    fun `should remove user from repository`() {
        val user = createUseCase.create("alice@example.com", "secret")
        useCase.delete(user.id)

        repo.findById(user.id) shouldBe null
    }
}
