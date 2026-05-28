/*
 * UserControllerMvcTest.kt
 *
 * $Since: 2026-05-26T00:00:00Z
 */
package com.fj.omnimemo.api.endpoint.user.impl

import com.fj.omnimemo.api.endpoint.ApiPathsV1
import com.fj.omnimemo.core.user.exception.UserNotFoundException
import com.fj.omnimemo.core.test.annotation.MediumTest
import com.fj.omnimemo.core.user.model.User
import com.fj.omnimemo.core.user.model.UserId
import com.fj.omnimemo.core.user.usecase.CreateUserUseCase
import com.fj.omnimemo.core.user.usecase.DeleteUserUseCase
import com.fj.omnimemo.core.user.usecase.FindUserUseCase
import com.fj.omnimemo.core.user.usecase.UpdateUserEmailUseCase
import com.fj.omnimemo.core.user.usecase.UpdateUserPasswordUseCase
import com.fj.omnimemo.infrastructure.security.JwtTokenService
import org.junit.jupiter.api.Test
import org.mockito.BDDMockito.given
import org.mockito.Mockito
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.content
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.util.UUID

/**
 * Medium Tests for [UserControllerImpl]: verifies HTTP path routing, request/response
 * JSON serialisation, and status codes via the Spring MVC stack.
 *
 * Security filters are excluded here; authentication logic is covered separately in
 * [com.fj.omnimemo.api.security.JwtAuthenticationFilterTest] and
 * [com.fj.omnimemo.api.endpoint.health.HealthControllerSpec].
 *
 * @author Francesco Jo
 * @since 0.1.1
 * @version 0.1.1
 */
@MediumTest
@WebMvcTest(controllers = [UserControllerImpl::class])
@AutoConfigureMockMvc(addFilters = false)
class UserControllerMvcTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @MockBean private lateinit var createUserUseCase: CreateUserUseCase
    @MockBean private lateinit var findUserUseCase: FindUserUseCase
    @MockBean private lateinit var updateUserEmailUseCase: UpdateUserEmailUseCase
    @MockBean private lateinit var updateUserPasswordUseCase: UpdateUserPasswordUseCase
    // Injected into controller via Spring DI; not referenced in test methods directly.
    @Suppress("UnusedPrivateProperty")
    @MockBean private lateinit var deleteUserUseCase: DeleteUserUseCase
    // Required by SecurityConfiguration; not referenced in test methods directly.
    @Suppress("UnusedPrivateProperty")
    @MockBean private lateinit var jwtTokenService: JwtTokenService

    private val existingUser = User.create("alice@example.com", "hashed")

    @Test
    fun `GET users by id returns 200 with JSON user body`() {
        val uuid = UUID.randomUUID()
        given(findUserUseCase.findById(UserId(uuid))).willReturn(existingUser)

        mockMvc.perform(get("${ApiPathsV1.USERS}/$uuid"))
            .andExpect(status().isOk)
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.body.email").value("alice@example.com"))
            .andExpect(jsonPath("$.body.id").isString)
            .andExpect(jsonPath("$.body.passwordHash").doesNotExist())
    }

    @Test
    fun `GET users by id returns 404 when user is not found`() {
        val uuid = UUID.randomUUID()
        given(findUserUseCase.findById(UserId(uuid))).willReturn(null)

        mockMvc.perform(get("${ApiPathsV1.USERS}/$uuid"))
            .andExpect(status().isNotFound)
    }

    @Test
    fun `GET users by id returns 400 for a malformed UUID`() {
        mockMvc.perform(get("${ApiPathsV1.USERS}/not-a-uuid"))
            .andExpect(status().isBadRequest)
    }

    @Test
    fun `POST users returns 201 with JSON user body`() {
        given(createUserUseCase.create(anyArg(), anyArg())).willReturn(existingUser)

        mockMvc.perform(
            post(ApiPathsV1.USERS)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"email":"alice@example.com","password":"secret"}""")
        )
            .andExpect(status().isCreated)
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.body.email").value("alice@example.com"))
    }

    @Test
    fun `PUT users email returns 200 with updated user`() {
        val uuid = UUID.randomUUID()
        given(updateUserEmailUseCase.updateEmail(UserId(uuid), "new@example.com")).willReturn(existingUser)

        mockMvc.perform(
            put("${ApiPathsV1.USERS}/$uuid/email")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"email":"new@example.com"}""")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.body.email").value("alice@example.com"))
    }

    @Test
    fun `PUT users password returns 200 with updated user`() {
        val uuid = UUID.randomUUID()
        given(updateUserPasswordUseCase.updatePassword(UserId(uuid), "newpass")).willReturn(existingUser)

        mockMvc.perform(
            put("${ApiPathsV1.USERS}/$uuid/password")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"password":"newpass"}""")
        )
            .andExpect(status().isOk)
    }

    @Test
    fun `PUT users email returns 404 when user does not exist`() {
        val uuid = UUID.randomUUID()
        given(updateUserEmailUseCase.updateEmail(UserId(uuid), "new@example.com"))
            .willThrow(UserNotFoundException(UserId(uuid)))

        mockMvc.perform(
            put("${ApiPathsV1.USERS}/$uuid/email")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"email":"new@example.com"}""")
        )
            .andExpect(status().isNotFound)
    }

    @Test
    fun `DELETE users returns 204 with no body`() {
        mockMvc.perform(delete("${ApiPathsV1.USERS}/${UUID.randomUUID()}"))
            .andExpect(status().isNoContent)
    }

    companion object {
        // Mockito.any() returns null; T : Any would cause Kotlin to emit a runtime null-check
        // on the cast inside this helper, throwing NPE before Mockito can intercept. Keeping T
        // unconstrained makes the cast erasure-safe so Mockito records the matcher as intended.
        @Suppress("UNCHECKED_CAST")
        private fun <T> anyArg(): T = Mockito.any<Any>() as T
    }
}
