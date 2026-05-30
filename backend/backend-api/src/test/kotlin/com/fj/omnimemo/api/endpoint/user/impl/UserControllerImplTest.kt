/*
 * UserControllerImplTest.kt
 *
 * $Since: 2026-05-26T00:00:00Z
 */
package com.fj.omnimemo.api.endpoint.user.impl

import com.fj.omnimemo.api.endpoint.user.dto.request.CreateUserRequest
import com.fj.omnimemo.api.endpoint.user.dto.response.UserResponse
import com.fj.omnimemo.core.test.annotation.SmallTest
import com.fj.omnimemo.core.user.MockUserProfileCache
import com.fj.omnimemo.core.user.exception.UserNotFoundException
import com.fj.omnimemo.core.user.model.User
import com.fj.omnimemo.core.user.model.UserProfile
import com.fj.omnimemo.core.user.repository.MockUserRepository
import com.fj.omnimemo.core.user.security.MockPasswordHasher
import com.fj.omnimemo.core.user.usecase.CreateUserUseCase
import com.fj.omnimemo.core.user.usecase.DeleteUserUseCase
import com.fj.omnimemo.core.user.usecase.FindUserUseCase
import io.kotest.assertions.assertSoftly
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus
import org.springframework.web.server.ResponseStatusException
import java.util.*

@SmallTest
class UserControllerImplTest {

    private val repo = MockUserRepository()
    private val hasher = MockPasswordHasher()
    private val profileCache = MockUserProfileCache()
    private val controller = UserControllerImpl(
        createUserUseCase = CreateUserUseCase(repo, hasher),
        findUserUseCase = FindUserUseCase(repo),
        deleteUserUseCase = DeleteUserUseCase(repo),
        userProfileCache = profileCache,
    )

    private lateinit var existingUser: User

    @BeforeEach
    fun setUp() {
        repo.clear()
        profileCache.clear()
        existingUser = repo.save(User.create("alice@example.com", hasher.hash("secret")))
    }

    @Nested
    inner class FindById {

        @Test
        fun `should return UserResponse for an existing user`() {
            val response = controller.findById(existingUser.id.value.toString())

            assertSoftly {
                response.id shouldBe existingUser.id.value.toString()
                response.email shouldBe "alice@example.com"
                response.createdAt shouldNotBe null
                response.updatedAt shouldNotBe null
            }
        }

        @Test
        fun `should not expose password hash`() {
            val fields = UserResponse::class.java.declaredFields.map { it.name }
            fields shouldNotContain "passwordHash"
        }

        @Test
        fun `should throw 404 for unknown id`() {
            shouldThrow<ResponseStatusException> {
                controller.findById(UUID.randomUUID().toString())
            }.statusCode shouldBe HttpStatus.NOT_FOUND
        }

        @Test
        fun `should throw 400 for malformed id`() {
            shouldThrow<ResponseStatusException> {
                controller.findById("not-a-uuid")
            }.statusCode shouldBe HttpStatus.BAD_REQUEST
        }
    }

    @Nested
    inner class Create {

        @Test
        fun `should create and return UserResponse`() {
            val response = controller.create(CreateUserRequest("bob@example.com", "pass"))

            assertSoftly {
                response.id shouldNotBe null
                response.email shouldBe "bob@example.com"
                repo.findByEmail("bob@example.com") shouldNotBe null
            }
        }
    }

    @Nested
    inner class Delete {

        @Test
        fun `should remove user from repository`() {
            controller.delete(existingUser.id.value.toString())

            repo.findById(existingUser.id) shouldBe null
        }

        @Test
        fun `should invalidate cache entry after successful deletion`() {
            profileCache.put(UserProfile(existingUser.id, "alice@example.com"))

            controller.delete(existingUser.id.value.toString())

            profileCache.invalidatedIds shouldContain existingUser.id
            profileCache.get(existingUser.id) shouldBe null
        }

        @Test
        fun `should not invalidate cache when user is not found`() {
            val unknownId = UUID.randomUUID().toString()

            shouldThrow<UserNotFoundException> {
                controller.delete(unknownId)
            }

            profileCache.invalidatedIds shouldBe emptyList()
        }

        @Test
        fun `should propagate UserNotFoundException for unknown id`() {
            shouldThrow<UserNotFoundException> {
                controller.delete(UUID.randomUUID().toString())
            }
        }

        @Test
        fun `should throw 400 for malformed id`() {
            shouldThrow<ResponseStatusException> {
                controller.delete("bad-id")
            }.statusCode shouldBe HttpStatus.BAD_REQUEST
        }
    }
}
