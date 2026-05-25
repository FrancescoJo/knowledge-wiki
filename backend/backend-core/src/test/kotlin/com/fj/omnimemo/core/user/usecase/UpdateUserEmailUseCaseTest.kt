/*
 * UpdateUserEmailUseCaseTest.kt
 *
 * $Since: 2026-05-25T00:00:00Z
 */
package com.fj.omnimemo.core.user.usecase

import com.fj.omnimemo.core.user.model.MockPasswordHasher
import com.fj.omnimemo.core.user.model.MockUserRepository
import com.fj.omnimemo.core.user.model.UserId
import com.fj.omnimemo.core.test.annotation.SmallTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

@SmallTest
class UpdateUserEmailUseCaseTest {

    private val repo = MockUserRepository()
    private val createUseCase = CreateUserUseCase(repo, MockPasswordHasher())
    private val useCase = UpdateUserEmailUseCase(repo)

    @BeforeEach
    fun setUp() = repo.clear()

    @Test
    fun `should return user with updated email`() {
        val user = createUseCase.create("alice@example.com", "secret")

        val updated = useCase.updateEmail(user.id, "new@example.com")

        assertEquals("new@example.com", updated?.email)
    }

    @Test
    fun `should return null when user does not exist`() {
        assertNull(useCase.updateEmail(UserId.generate(), "new@example.com"))
    }
}
