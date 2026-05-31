/*
 * FindUserUseCaseTest.kt
 *
 * $Since: 2026-05-25T00:00:00Z
 */
package testcase.small.core.user.usecase

import com.fj.omnimemo.core.user.model.UserId
import com.fj.omnimemo.core.user.usecase.CreateUserUseCase
import com.fj.omnimemo.core.user.usecase.FindUserUseCase
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import test.com.fj.omnimemo.core.annotation.SmallTest
import test.com.fj.omnimemo.core.user.repository.MockUserRepository
import test.com.fj.omnimemo.core.user.security.MockPasswordHasher

@SmallTest
class FindUserUseCaseTest {
    private lateinit var repo: MockUserRepository
    private lateinit var createUseCase: CreateUserUseCase
    private lateinit var sut: FindUserUseCase

    @BeforeEach
    fun setUp() {
        repo = MockUserRepository()
        createUseCase = CreateUserUseCase(repo, MockPasswordHasher())
        sut = FindUserUseCase(repo)
    }

    @AfterEach
    fun tearDown() {
        repo.clear()
    }

    @Nested
    inner class FindById {
        @Test
        fun `should return user when user exists`() {
            val user = createUseCase.create("alice@example.com", "secret")

            sut.findById(user.id) shouldBe user
        }

        @Test
        fun `should return null when user does not exist`() {
            sut.findById(UserId.generate()) shouldBe null
        }
    }

    @Nested
    inner class FindByEmail {
        @Test
        fun `should return user when email matches`() {
            createUseCase.create("alice@example.com", "secret")

            sut.findByEmail("alice@example.com") shouldNotBe null
        }

        @Test
        fun `should return null when email does not match`() {
            sut.findByEmail("nobody@example.com") shouldBe null
        }
    }
}
