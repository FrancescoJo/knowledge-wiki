/*
 * FindUserUseCaseTest.kt
 *
 * $Since: 2026-05-25T00:00:00Z
 */
package com.fj.omnimemo.core.usecase.user

import com.fj.omnimemo.core.model.user.MockPasswordHasher
import com.fj.omnimemo.core.model.user.MockUserRepository
import com.fj.omnimemo.core.model.user.UserId
import com.fj.omnimemo.core.test.annotation.SmallTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

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

            assertEquals(user, useCase.findById(user.id))
        }

        @Test
        fun `should return null when user does not exist`() {
            assertNull(useCase.findById(UserId.generate()))
        }
    }

    @Nested
    inner class FindByEmail {
        @Test
        fun `should return user when email matches`() {
            createUseCase.create("alice@example.com", "secret")

            assertNotNull(useCase.findByEmail("alice@example.com"))
        }

        @Test
        fun `should return null when email does not match`() {
            assertNull(useCase.findByEmail("nobody@example.com"))
        }
    }
}
