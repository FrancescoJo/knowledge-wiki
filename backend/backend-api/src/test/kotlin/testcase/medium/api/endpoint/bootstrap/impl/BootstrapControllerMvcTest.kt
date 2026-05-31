/*
 * BootstrapControllerMvcTest.kt
 *
 * $Since: 2026-05-26T00:00:00Z
 */
package testcase.medium.api.endpoint.bootstrap.impl

import com.fj.omnimemo.api.endpoint.ApiPathsV1
import com.fj.omnimemo.api.endpoint.bootstrap.impl.BootstrapControllerImpl
import com.fj.omnimemo.core.user.UserProfileCache
import com.fj.omnimemo.core.user.exception.RedundantBootstrapProhibitedException
import com.fj.omnimemo.core.user.model.User
import com.fj.omnimemo.core.user.usecase.BootstrapUserUseCase
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
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*
import test.com.fj.omnimemo.core.annotation.MediumTest

/**
 * Medium Tests for [BootstrapControllerImpl]: verifies HTTP routing, JSON
 * serialisation, and status codes via the Spring MVC stack.
 *
 * Security filters are excluded here; IP restriction is enforced by
 * [com.fj.omnimemo.api.config.SecurityConfiguration] and is validated by the
 * Large Tests.
 *
 * @author Francesco Jo
 * @since 0.1.1
 * @version 0.1.1
 */
@MediumTest
@WebMvcTest(controllers = [BootstrapControllerImpl::class])
@AutoConfigureMockMvc(addFilters = false)
class BootstrapControllerMvcTest {
    @Autowired
    private lateinit var mockMvc: MockMvc

    @MockBean
    private lateinit var bootstrapUserUseCase: BootstrapUserUseCase

    // Required by GlobalModelAdvice; not referenced in test methods directly.
    @Suppress("UnusedPrivateProperty")
    @MockBean
    private lateinit var userProfileCache: UserProfileCache

    @Suppress("UnusedPrivateProperty")
    @MockBean
    private lateinit var jwtTokenService: JwtTokenService

    private val user = User.create("alice@example.com", "hashed")

    @Test
    fun `POST bootstrap users returns 201 with JSON user body`() {
        given(bootstrapUserUseCase.bootstrap(anyArg(), anyArg())).willReturn(user)

        mockMvc.perform(
            post(ApiPathsV1.BOOTSTRAP_USERS)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"email":"alice@example.com","password":"secret"}""")
        )
            .andExpect(status().isCreated)
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.body.email").value("alice@example.com"))
            .andExpect(jsonPath("$.body.id").isString)
    }

    @Test
    fun `POST bootstrap users returns 409 when users already exist`() {
        given(bootstrapUserUseCase.bootstrap(anyArg(), anyArg()))
            .willThrow(RedundantBootstrapProhibitedException())

        mockMvc.perform(
            post(ApiPathsV1.BOOTSTRAP_USERS)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"email":"alice@example.com","password":"secret"}""")
        )
            .andExpect(status().isConflict)
    }

    companion object {
        @Suppress("UNCHECKED_CAST")
        private fun <T> anyArg(): T = Mockito.any<Any>() as T
    }
}
