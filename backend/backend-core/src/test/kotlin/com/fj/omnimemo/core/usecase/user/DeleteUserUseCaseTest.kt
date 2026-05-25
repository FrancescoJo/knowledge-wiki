/*
 * DeleteUserUseCaseTest.kt
 *
 * $Since: 2026-05-25T00:00:00Z
 */
package com.fj.omnimemo.core.usecase.user

import com.fj.omnimemo.core.model.user.MockPasswordHasher
import com.fj.omnimemo.core.model.user.MockUserRepository
import com.fj.omnimemo.core.test.annotation.SmallTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.test.assertNull

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

        assertNull(repo.findById(user.id))
    }
}
