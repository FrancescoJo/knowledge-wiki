/*
 * RootViewControllerMvcTest.kt
 *
 * $Since: 2026-05-29T00:00:00Z
 */
package com.fj.omnimemo.view

import com.fj.omnimemo.core.test.annotation.MediumTest
import com.fj.omnimemo.core.user.model.User
import com.fj.omnimemo.core.user.model.UserId
import com.fj.omnimemo.core.user.usecase.FindUserUseCase
import com.fj.omnimemo.infrastructure.security.JwtTokenService
import org.hamcrest.CoreMatchers.containsString
import org.hamcrest.CoreMatchers.not
import org.junit.jupiter.api.Test
import org.mockito.BDDMockito.given
import org.mockito.Mockito
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.content
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.time.Instant

/**
 * Medium tests for [RootViewController]: verifies HTML output reflects the
 * current authentication state via [com.fj.omnimemo.api.advice.GlobalModelAdvice].
 *
 * @author Francesco Jo
 * @since 0.1.1
 */
@MediumTest
@WebMvcTest(controllers = [RootViewController::class])
@AutoConfigureMockMvc(addFilters = false)
class RootViewControllerMvcTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @MockBean private lateinit var findUserUseCase: FindUserUseCase
    // Required by SecurityConfiguration.
    @Suppress("UnusedPrivateProperty")
    @MockBean private lateinit var jwtTokenService: JwtTokenService

    @Test
    fun `GET root shows login button when not authenticated`() {
        mockMvc.perform(get("/"))
            .andExpect(status().isOk)
            .andExpect(content().string(containsString("login-btn")))
            .andExpect(content().string(not(containsString("user-btn"))))
    }

    @Test
    fun `GET root shows user email and logout option in header when authenticated`() {
        val userId = UserId.generate()
        val user = User.reconstitute(
            id = userId,
            email = "alice@example.com",
            passwordHash = "hash",
            createdAt = Instant.now(),
            updatedAt = Instant.now(),
        )
        given(findUserUseCase.findById(anyArg())).willReturn(user)

        mockMvc.perform(
            get("/").with(
                authentication(UsernamePasswordAuthenticationToken(userId.value.toString(), null, emptyList()))
            )
        )
            .andExpect(status().isOk)
            .andExpect(content().string(containsString("alice@example.com")))
            .andExpect(content().string(containsString("user-btn")))
            .andExpect(content().string(not(containsString("login-btn"))))
    }

    companion object {
        @Suppress("UNCHECKED_CAST")
        private fun <T> anyArg(): T = Mockito.any<Any>() as T
    }
}
