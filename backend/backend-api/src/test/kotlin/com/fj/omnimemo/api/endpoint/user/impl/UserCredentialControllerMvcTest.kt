/*
 * UserCredentialControllerMvcTest.kt
 *
 * $Since: 2026-05-30T00:00:00Z
 */
package com.fj.omnimemo.api.endpoint.user.impl

import com.fj.omnimemo.api.endpoint.ApiPathsV1
import com.fj.omnimemo.core.test.annotation.MediumTest
import com.fj.omnimemo.core.user.UserProfileCache
import com.fj.omnimemo.core.user.exception.UserNotFoundException
import com.fj.omnimemo.core.user.model.User
import com.fj.omnimemo.core.user.model.UserId
import com.fj.omnimemo.core.user.usecase.UpdateUserEmailUseCase
import com.fj.omnimemo.core.user.usecase.UpdateUserPasswordUseCase
import com.fj.omnimemo.infrastructure.security.JwtTokenService
import org.junit.jupiter.api.Test
import org.mockito.BDDMockito.given
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*
import java.util.*

/**
 * Medium Tests for [UserCredentialControllerImpl]: verifies HTTP routing, JSON
 * serialisation, and status codes for credential operations via the Spring MVC stack.
 *
 * Account operations are covered in [UserControllerMvcTest].
 * Security filters are excluded; authentication logic is covered separately.
 *
 * Kotlin value classes are inlined at non-null JVM call sites, so stubs use
 * concrete argument values (not anyArg()) to avoid NPE before Mockito intercepts.
 *
 * @author Francesco Jo
 * @since 0.2.0
 * @version 0.2.0
 */
@MediumTest
@WebMvcTest(controllers = [UserCredentialControllerImpl::class])
@AutoConfigureMockMvc(addFilters = false)
class UserCredentialControllerMvcTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @MockBean
    private lateinit var updateUserEmailUseCase: UpdateUserEmailUseCase

    @MockBean
    private lateinit var updateUserPasswordUseCase: UpdateUserPasswordUseCase

    @Suppress("UnusedPrivateProperty")
    @MockBean
    private lateinit var userProfileCache: UserProfileCache

    // Required by SecurityConfiguration; not referenced in test methods directly.
    @Suppress("UnusedPrivateProperty")
    @MockBean
    private lateinit var jwtTokenService: JwtTokenService

    private val existingUser = User.create("alice@example.com", "hashed")

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
}
