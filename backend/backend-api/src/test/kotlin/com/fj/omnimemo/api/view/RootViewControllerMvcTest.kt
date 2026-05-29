/*
 * RootViewControllerMvcTest.kt
 *
 * $Since: 2026-05-29T00:00:00Z
 */
package com.fj.omnimemo.api.view

import com.fj.omnimemo.core.test.annotation.MediumTest
import com.fj.omnimemo.core.user.UserProfileCache
import com.fj.omnimemo.core.user.model.UserProfile
import com.fj.omnimemo.core.user.model.UserId
import com.fj.omnimemo.infrastructure.security.JwtTokenService
import com.fj.omnimemo.view.RootViewController
import org.hamcrest.CoreMatchers.containsString
import org.hamcrest.CoreMatchers.not
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.mockito.BDDMockito.given
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.web.csrf.CsrfToken
import org.springframework.security.web.csrf.DefaultCsrfToken
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.RequestPostProcessor
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.content
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

/**
 * Medium tests for [RootViewController]: verifies HTML output reflects the
 * current authentication state via [com.fj.omnimemo.api.advice.GlobalModelAdvice].
 *
 * [addFilters] is false so security filters are not applied. The authenticated test
 * sets [SecurityContextHolder] directly and also injects a stub [CsrfToken] into the
 * request attributes so that Thymeleaf can evaluate `${_csrf.token}` in the logout button.
 *
 * Assertions use `class="login-btn"` and `class="user-btn"` (with `class=` prefix) to
 * avoid false matches against the CSS selectors `.login-btn` and `.user-btn` that are
 * always present in the page's style block.
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

    @MockBean private lateinit var userProfileCache: UserProfileCache
    // Required by SecurityConfiguration.
    @Suppress("UnusedPrivateProperty")
    @MockBean private lateinit var jwtTokenService: JwtTokenService

    @AfterEach
    fun tearDown() {
        SecurityContextHolder.clearContext()
    }

    @Test
    fun `GET root shows login button when not authenticated`() {
        mockMvc.perform(get("/"))
            .andExpect(status().isOk)
            .andExpect(content().string(containsString("class=\"login-btn\"")))
            .andExpect(content().string(not(containsString("class=\"user-btn\""))))
    }

    @Test
    fun `GET root shows user email and logout option in header when authenticated`() {
        val userId = UserId.generate()
        given(userProfileCache.get(userId)).willReturn(UserProfile(userId, "alice@example.com"))
        SecurityContextHolder.getContext().authentication =
            UsernamePasswordAuthenticationToken(userId.value.toString(), null, emptyList())

        mockMvc.perform(get("/").with(stubCsrfToken()))
            .andExpect(status().isOk)
            .andExpect(content().string(containsString("alice@example.com")))
            .andExpect(content().string(containsString("class=\"user-btn\"")))
            .andExpect(content().string(not(containsString("class=\"login-btn\""))))
    }

    companion object {
        // The logout button template references ${_csrf.token}. The CsrfFilter is excluded by
        // addFilters = false, so we inject a stub token directly into the request attributes to
        // prevent a Thymeleaf SpEL evaluation error on the authenticated view.
        private fun stubCsrfToken(): RequestPostProcessor = RequestPostProcessor { request: MockHttpServletRequest ->
            val token: CsrfToken = DefaultCsrfToken("X-XSRF-TOKEN", "_csrf", "test-token")
            request.setAttribute(CsrfToken::class.java.name, token)
            request.setAttribute("_csrf", token)
            request
        }
    }
}
