/*
 * ContentsViewControllerMvcTest.kt
 *
 * $Since: 2026-05-30T00:00:00Z
 */
package testcase.medium.api.view

import com.fj.omnimemo.core.user.UserProfileCache
import com.fj.omnimemo.infrastructure.security.JwtTokenService
import com.fj.omnimemo.view.ContentsViewController
import org.hamcrest.CoreMatchers.containsString
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.content
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import test.com.fj.omnimemo.core.annotation.MediumTest

/**
 * Medium tests for [ContentsViewController]: verifies the contents index page renders
 * with correct HTML and breadcrumb structure.
 *
 * Security filters are excluded; authentication logic is covered separately.
 *
 * @author Francesco Jo
 * @since 0.2.0
 * @version 0.2.0
 */
@MediumTest
@WebMvcTest(controllers = [ContentsViewController::class])
@AutoConfigureMockMvc(addFilters = false)
class ContentsViewControllerMvcTest {
    @Autowired
    private lateinit var mockMvc: MockMvc

    @Suppress("UnusedPrivateProperty")
    @MockBean
    private lateinit var userProfileCache: UserProfileCache

    @Suppress("UnusedPrivateProperty")
    @MockBean
    private lateinit var jwtTokenService: JwtTokenService

    @Test
    fun `GET contents returns 200 with HTML content`() {
        mockMvc.perform(get("/contents"))
            .andExpect(status().isOk)
            .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_HTML))
            .andExpect(content().string(containsString("Contents")))
    }

    @Test
    fun `GET contents includes breadcrumb with Home link`() {
        mockMvc.perform(get("/contents"))
            .andExpect(status().isOk)
            .andExpect(content().string(containsString("href=\"/\"")))
    }

    @Test
    fun `GET contents includes Notes link`() {
        mockMvc.perform(get("/contents"))
            .andExpect(status().isOk)
            .andExpect(content().string(containsString("href=\"/notes\"")))
    }
}
