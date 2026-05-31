/*
 * BootstrapControllerImplTest.kt
 *
 * $Since: 2026-05-26T00:00:00Z
 */
package testcase.small.api.endpoint.bootstrap.impl

import com.fj.omnimemo.api.endpoint.bootstrap.dto.request.BootstrapUserRequest
import com.fj.omnimemo.api.endpoint.bootstrap.impl.BootstrapControllerImpl
import com.fj.omnimemo.core.user.exception.RedundantBootstrapProhibitedException
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
class BootstrapControllerImplTest {
    private lateinit var repo: MockUserRepository
    private lateinit var hasher: MockPasswordHasher
    private lateinit var sut: BootstrapControllerImpl

    @BeforeEach
    fun setUp() {
        repo = MockUserRepository()
        hasher = MockPasswordHasher()
        sut = BootstrapControllerImpl(BootstrapUserUseCase(repo, hasher))
    }

    @AfterEach
    fun tearDown() {
        repo.clear()
    }

    @Nested
    inner class BootstrapUser {

        @Test
        fun `should return user response when repository is empty`() {
            val response = sut.bootstrapUser(BootstrapUserRequest("alice@example.com", "secret"))

            assertSoftly {
                response.id shouldNotBe null
                response.email shouldBe "alice@example.com"
            }
        }

        @Test
        fun `should propagate RedundantBootstrapProhibitedException when users already exist`() {
            sut.bootstrapUser(BootstrapUserRequest("alice@example.com", "secret"))

            shouldThrow<RedundantBootstrapProhibitedException> {
                sut.bootstrapUser(BootstrapUserRequest("bob@example.com", "pass"))
            }
        }
    }
}
