/*
 * CreateUserUseCaseTest.kt
 *
 * $Since: 2026-05-25T00:00:00Z
 */
package com.fj.omnimemo.core.user.usecase

import com.fj.omnimemo.core.user.model.MockPasswordHasher
import com.fj.omnimemo.core.user.model.MockUserRepository
import com.fj.omnimemo.core.test.annotation.SmallTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

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

        assertNotNull(repo.findById(user.id))
        assertEquals("hashed:secret", user.passwordHash)
    }
}
