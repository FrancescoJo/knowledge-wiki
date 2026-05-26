/*
 * BootstrapUserUseCaseTest.kt
 *
 * $Since: 2026-05-26T00:00:00Z
 */
package com.fj.omnimemo.core.user.usecase

import com.fj.omnimemo.core.test.annotation.SmallTest
import com.fj.omnimemo.core.user.model.User
import com.fj.omnimemo.core.user.repository.MockUserRepository
import com.fj.omnimemo.core.user.security.MockPasswordHasher
import io.kotest.assertions.assertSoftly
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

@SmallTest
class BootstrapUserUseCaseTest {

    private val repo = MockUserRepository()
    private val hasher = MockPasswordHasher()
    private val useCase = BootstrapUserUseCase(repo, hasher)

    @BeforeEach
    fun setUp() = repo.clear()

    @Nested
    inner class Bootstrap {

        @Test
        fun `should create and persist the first user when repository is empty`() {
            val user = useCase.bootstrap("alice@example.com", "secret")

            assertSoftly {
                user shouldNotBe null
                repo.findById(user!!.id) shouldNotBe null
                user.passwordHash shouldBe "hashed:secret"
            }
        }

        @Test
        fun `should return null when at least one user already exists`() {
            repo.save(User.create("existing@example.com", hasher.hash("pass")))

            val result = useCase.bootstrap("alice@example.com", "secret")

            result shouldBe null
        }
    }
}
