/*
 * CreateUserUseCaseTest.kt
 *
 * $Since: 2026-05-25T00:00:00Z
 */
package testcase.small.core.user.usecase

import com.fj.omnimemo.core.user.usecase.CreateUserUseCase
import io.kotest.assertions.assertSoftly
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import test.com.fj.omnimemo.core.annotation.SmallTest
import test.com.fj.omnimemo.core.user.repository.MockUserRepository
import test.com.fj.omnimemo.core.user.security.MockPasswordHasher

@SmallTest
class CreateUserUseCaseTest {
    private lateinit var repo: MockUserRepository
    private lateinit var hasher: MockPasswordHasher
    private lateinit var sut: CreateUserUseCase

    @BeforeEach
    fun setUp() {
        repo = MockUserRepository()
        hasher = MockPasswordHasher()
        sut = CreateUserUseCase(repo, hasher)
    }

    @AfterEach
    fun tearDown() {
        repo.clear()
    }

    @Test
    fun `should persist a new user with a hashed password`() {
        val user = sut.create("alice@example.com", "secret")

        assertSoftly {
            repo.findById(user.id) shouldNotBe null
            user.passwordHash shouldBe "hashed:secret"
        }
    }
}
