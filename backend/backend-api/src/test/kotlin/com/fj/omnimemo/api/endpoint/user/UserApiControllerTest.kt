/*
 * UserApiControllerTest.kt
 *
 * $Since: 2026-05-25T00:00:00Z
 */
package com.fj.omnimemo.api.endpoint.user

import com.fj.omnimemo.core.test.annotation.SmallTest
import com.fj.omnimemo.core.user.model.User
import com.fj.omnimemo.core.user.model.UserId
import com.fj.omnimemo.core.user.repository.MockUserRepository
import com.fj.omnimemo.core.user.security.MockPasswordHasher
import com.fj.omnimemo.core.user.usecase.CreateUserUseCase
import com.fj.omnimemo.core.user.usecase.DeleteUserUseCase
import com.fj.omnimemo.core.user.usecase.FindUserUseCase
import com.fj.omnimemo.core.user.usecase.UpdateUserEmailUseCase
import com.fj.omnimemo.core.user.usecase.UpdateUserPasswordUseCase
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus
import org.springframework.web.server.ResponseStatusException
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertNull

@SmallTest
class UserApiControllerTest {

    private val repo = MockUserRepository()
    private val hasher = MockPasswordHasher()
    private val controller = UserApiController(
        createUserUseCase = CreateUserUseCase(repo, hasher),
        findUserUseCase = FindUserUseCase(repo),
        updateUserEmailUseCase = UpdateUserEmailUseCase(repo),
        updateUserPasswordUseCase = UpdateUserPasswordUseCase(repo, hasher),
        deleteUserUseCase = DeleteUserUseCase(repo),
    )

    private lateinit var existingUser: User

    @BeforeEach
    fun setUp() {
        repo.clear()
        existingUser = repo.save(User.create("alice@example.com", hasher.hash("secret")))
    }

    @Nested
    inner class FindById {

        @Test
        fun `should return UserResponse for an existing user`() {
            val response = controller.findById(existingUser.id.value.toString())

            assertEquals(existingUser.id.value.toString(), response.id)
            assertEquals("alice@example.com", response.email)
            assertNotNull(response.createdAt)
            assertNotNull(response.updatedAt)
        }

        @Test
        fun `should not expose password hash`() {
            val fields = UserResponse::class.java.declaredFields.map { it.name }
            assert("passwordHash" !in fields)
        }

        @Test
        fun `should throw 404 for unknown id`() {
            val ex = assertFailsWith<ResponseStatusException> {
                controller.findById(UUID.randomUUID().toString())
            }
            assertEquals(HttpStatus.NOT_FOUND, ex.statusCode)
        }

        @Test
        fun `should throw 400 for malformed id`() {
            val ex = assertFailsWith<ResponseStatusException> {
                controller.findById("not-a-uuid")
            }
            assertEquals(HttpStatus.BAD_REQUEST, ex.statusCode)
        }
    }

    @Nested
    inner class Create {

        @Test
        fun `should create and return UserResponse`() {
            val response = controller.create(CreateUserRequest("bob@example.com", "pass"))

            assertNotNull(response.id)
            assertEquals("bob@example.com", response.email)
            assertNotNull(repo.findByEmail("bob@example.com"))
        }
    }

    @Nested
    inner class UpdateEmail {

        @Test
        fun `should update email and return updated UserResponse`() {
            val response = controller.updateEmail(
                existingUser.id.value.toString(),
                UpdateEmailRequest("new@example.com"),
            )

            assertEquals("new@example.com", response.email)
        }

        @Test
        fun `should throw 404 for unknown id`() {
            val ex = assertFailsWith<ResponseStatusException> {
                controller.updateEmail(UUID.randomUUID().toString(), UpdateEmailRequest("x@example.com"))
            }
            assertEquals(HttpStatus.NOT_FOUND, ex.statusCode)
        }

        @Test
        fun `should throw 400 for malformed id`() {
            val ex = assertFailsWith<ResponseStatusException> {
                controller.updateEmail("bad-id", UpdateEmailRequest("x@example.com"))
            }
            assertEquals(HttpStatus.BAD_REQUEST, ex.statusCode)
        }
    }

    @Nested
    inner class UpdatePassword {

        @Test
        fun `should update password and return updated UserResponse`() {
            val response = controller.updatePassword(
                existingUser.id.value.toString(),
                UpdatePasswordRequest("newpass"),
            )

            assertEquals(existingUser.id.value.toString(), response.id)
        }

        @Test
        fun `should throw 404 for unknown id`() {
            val ex = assertFailsWith<ResponseStatusException> {
                controller.updatePassword(UUID.randomUUID().toString(), UpdatePasswordRequest("x"))
            }
            assertEquals(HttpStatus.NOT_FOUND, ex.statusCode)
        }
    }

    @Nested
    inner class Delete {

        @Test
        fun `should remove user from repository`() {
            controller.delete(existingUser.id.value.toString())

            assertNull(repo.findById(existingUser.id))
        }

        @Test
        fun `should throw 400 for malformed id`() {
            val ex = assertFailsWith<ResponseStatusException> {
                controller.delete("bad-id")
            }
            assertEquals(HttpStatus.BAD_REQUEST, ex.statusCode)
        }
    }
}
