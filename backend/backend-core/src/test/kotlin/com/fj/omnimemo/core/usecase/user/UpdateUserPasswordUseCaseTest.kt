/*
 * UpdateUserPasswordUseCaseTest.kt
 *
 * $Since: 2026-05-25T00:00:00Z
 */
package com.fj.omnimemo.core.usecase.user

import com.fj.omnimemo.core.model.user.MockPasswordHasher
import com.fj.omnimemo.core.model.user.MockUserRepository
import com.fj.omnimemo.core.model.user.UserId
import com.fj.omnimemo.core.test.annotation.SmallTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

@SmallTest
class UpdateUserPasswordUseCaseTest {

    private val repo = MockUserRepository()
    private val hasher = MockPasswordHasher()
    private val createUseCase = CreateUserUseCase(repo, hasher)
    private val useCase = UpdateUserPasswordUseCase(repo, hasher)

    @BeforeEach
    fun setUp() = repo.clear()

    @Test
    fun `should return user with updated password hash`() {
        val user = createUseCase.create("alice@example.com", "old-secret")

        val updated = useCase.updatePassword(user.id, "new-secret")

        assertEquals("hashed:new-secret", updated?.passwordHash)
    }

    @Test
    fun `should return null when user does not exist`() {
        assertNull(useCase.updatePassword(UserId.generate(), "new-secret"))
    }
}
