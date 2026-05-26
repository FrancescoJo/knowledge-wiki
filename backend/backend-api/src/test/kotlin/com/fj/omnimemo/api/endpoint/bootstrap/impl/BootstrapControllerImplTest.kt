/*
 * BootstrapControllerImplTest.kt
 *
 * $Since: 2026-05-26T00:00:00Z
 */
package com.fj.omnimemo.api.endpoint.bootstrap.impl

import com.fj.omnimemo.api.endpoint.bootstrap.dto.request.BootstrapUserRequest
import com.fj.omnimemo.core.user.exception.RedundantBootstrapProhibitedException
import com.fj.omnimemo.core.test.annotation.SmallTest
import com.fj.omnimemo.core.user.repository.MockUserRepository
import com.fj.omnimemo.core.user.security.MockPasswordHasher
import com.fj.omnimemo.core.user.usecase.BootstrapUserUseCase
import io.kotest.assertions.assertSoftly
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

@SmallTest
class BootstrapControllerImplTest {

    private val repo = MockUserRepository()
    private val hasher = MockPasswordHasher()
    private val controller = BootstrapControllerImpl(BootstrapUserUseCase(repo, hasher))

    @BeforeEach
    fun setUp() = repo.clear()

    @Nested
    inner class BootstrapUser {

        @Test
        fun `should return user response when repository is empty`() {
            val response = controller.bootstrapUser(BootstrapUserRequest("alice@example.com", "secret"))

            assertSoftly {
                response.id shouldNotBe null
                response.email shouldBe "alice@example.com"
            }
        }

        @Test
        fun `should propagate RedundantBootstrapProhibitedException when users already exist`() {
            controller.bootstrapUser(BootstrapUserRequest("alice@example.com", "secret"))

            shouldThrow<RedundantBootstrapProhibitedException> {
                controller.bootstrapUser(BootstrapUserRequest("bob@example.com", "pass"))
            }
        }
    }
}
