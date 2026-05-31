/*
 * BootstrapUserUseCaseTest.kt
 *
 * $Since: 2026-05-26T00:00:00Z
 */
package testcase.small.core.user.usecase

import com.fj.omnimemo.core.user.exception.RedundantBootstrapProhibitedException
import com.fj.omnimemo.core.user.model.User
import com.fj.omnimemo.core.user.usecase.BootstrapUserUseCase
import io.kotest.assertions.assertSoftly
import io.kotest.assertions.throwables.shouldThrow
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
class BootstrapUserUseCaseTest {
    private lateinit var repo: MockUserRepository
    private lateinit var hasher: MockPasswordHasher
    private lateinit var sut: BootstrapUserUseCase

    @BeforeEach
    fun setUp() {
        repo = MockUserRepository()
        hasher = MockPasswordHasher()
        sut = BootstrapUserUseCase(repo, hasher)
    }

    @AfterEach
    fun tearDown() {
        repo.clear()
    }

    @Nested
    inner class Bootstrap {

        @Test
        fun `should create and persist the first user when repository is empty`() {
            val user = sut.bootstrap("alice@example.com", "secret")

            assertSoftly {
                repo.findById(user.id) shouldNotBe null
                user.passwordHash shouldBe "hashed:secret"
            }
        }

        @Test
        fun `should throw RedundantBootstrapProhibitedException when at least one user already exists`() {
            repo.save(User.create("existing@example.com", hasher.hash("pass")))

            shouldThrow<RedundantBootstrapProhibitedException> {
                sut.bootstrap("alice@example.com", "secret")
            }
        }
    }
}
