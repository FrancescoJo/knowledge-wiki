/*
 * UserCredentialControllerImplTest.kt
 *
 * $Since: 2026-05-30T00:00:00Z
 */
package testcase.small.api.endpoint.user.impl

import com.fj.omnimemo.api.endpoint.user.dto.request.UpdateEmailRequest
import com.fj.omnimemo.api.endpoint.user.dto.request.UpdatePasswordRequest
import com.fj.omnimemo.api.endpoint.user.impl.UserCredentialControllerImpl
import com.fj.omnimemo.core.user.exception.UserNotFoundException
import com.fj.omnimemo.core.user.model.User
import com.fj.omnimemo.core.user.model.UserProfile
import com.fj.omnimemo.core.user.usecase.UpdateUserEmailUseCase
import com.fj.omnimemo.core.user.usecase.UpdateUserPasswordUseCase
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus
import org.springframework.web.server.ResponseStatusException
import test.com.fj.omnimemo.core.annotation.SmallTest
import test.com.fj.omnimemo.core.user.MockUserProfileCache
import test.com.fj.omnimemo.core.user.repository.MockUserRepository
import test.com.fj.omnimemo.core.user.security.MockPasswordHasher
import java.util.*

@SmallTest
class UserCredentialControllerImplTest {
    private lateinit var repo: MockUserRepository
    private lateinit var hasher: MockPasswordHasher
    private lateinit var profileCache: MockUserProfileCache
    private lateinit var sut: UserCredentialControllerImpl

    private lateinit var existingUser: User

    @BeforeEach
    fun setUp() {
        repo = MockUserRepository()
        hasher = MockPasswordHasher()
        profileCache = MockUserProfileCache()
        sut = UserCredentialControllerImpl(
            updateUserEmailUseCase = UpdateUserEmailUseCase(repo),
            updateUserPasswordUseCase = UpdateUserPasswordUseCase(repo, hasher),
            userProfileCache = profileCache,
        )
        existingUser = repo.save(User.create("alice@example.com", hasher.hash("secret")))
    }

    @AfterEach
    fun tearDown() {
        repo.clear()
        profileCache.clear()
    }

    @Nested
    inner class UpdateEmail {

        @Test
        fun `should update email and return updated UserResponse`() {
            val response = sut.updateEmail(
                existingUser.id.value.toString(),
                UpdateEmailRequest("new@example.com"),
            )

            response.email shouldBe "new@example.com"
        }

        @Test
        fun `should invalidate cache entry after successful email update`() {
            profileCache.put(UserProfile(existingUser.id, "alice@example.com"))

            sut.updateEmail(existingUser.id.value.toString(), UpdateEmailRequest("new@example.com"))

            profileCache.invalidatedIds shouldContain existingUser.id
            profileCache.get(existingUser.id) shouldBe null
        }

        @Test
        fun `should not invalidate cache when user is not found`() {
            val unknownId = UUID.randomUUID().toString()

            shouldThrow<UserNotFoundException> {
                sut.updateEmail(unknownId, UpdateEmailRequest("x@example.com"))
            }

            profileCache.invalidatedIds shouldBe emptyList()
        }

        @Test
        fun `should propagate UserNotFoundException for unknown id`() {
            shouldThrow<UserNotFoundException> {
                sut.updateEmail(UUID.randomUUID().toString(), UpdateEmailRequest("x@example.com"))
            }
        }

        @Test
        fun `should throw 400 for malformed id`() {
            shouldThrow<ResponseStatusException> {
                sut.updateEmail("bad-id", UpdateEmailRequest("x@example.com"))
            }.statusCode shouldBe HttpStatus.BAD_REQUEST
        }
    }

    @Nested
    inner class UpdatePassword {

        @Test
        fun `should update password and return updated UserResponse`() {
            val response = sut.updatePassword(
                existingUser.id.value.toString(),
                UpdatePasswordRequest("newpass"),
            )

            response.id shouldBe existingUser.id.value.toString()
        }

        @Test
        fun `should propagate UserNotFoundException for unknown id`() {
            shouldThrow<UserNotFoundException> {
                sut.updatePassword(UUID.randomUUID().toString(), UpdatePasswordRequest("x"))
            }
        }
    }
}
