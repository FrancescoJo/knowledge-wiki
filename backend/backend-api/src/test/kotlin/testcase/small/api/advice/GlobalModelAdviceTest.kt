/*
 * GlobalModelAdviceTest.kt
 *
 * $Since: 2026-05-29T00:00:00Z
 */
package testcase.small.api.advice

import com.fj.omnimemo.api.advice.GlobalModelAdvice
import com.fj.omnimemo.api.user.LruUserProfileCache
import com.fj.omnimemo.core.user.model.User
import com.fj.omnimemo.core.user.model.UserId
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.context.support.StaticMessageSource
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder
import test.com.fj.omnimemo.core.annotation.SmallTest
import test.com.fj.omnimemo.core.user.repository.MockUserRepository
import java.util.*

/**
 * Small tests for [GlobalModelAdvice.currentUserEmail].
 *
 * @author Francesco Jo
 * @since 0.1.1
 */
@SmallTest
class GlobalModelAdviceTest {
    private lateinit var userRepo: MockUserRepository
    private lateinit var sut: GlobalModelAdvice

    @BeforeEach
    fun setUp() {
        userRepo = MockUserRepository()
        sut = GlobalModelAdvice(
            "local",
            StaticMessageSource().apply { addMessage("nav.home", Locale.ENGLISH, "Home") },
            LruUserProfileCache(userRepo),
        )
    }

    @AfterEach
    fun tearDown() {
        SecurityContextHolder.clearContext()
        userRepo.clear()
    }

    @Test
    fun `currentUserEmail returns email when user is authenticated`() {
        val user = User.create("alice@example.com", "hash")
        userRepo.save(user)
        authenticate(user.id)

        sut.currentUserEmail() shouldBe "alice@example.com"
    }

    @Test
    fun `currentUserEmail returns null when SecurityContext has no authentication`() {
        SecurityContextHolder.clearContext()

        sut.currentUserEmail() shouldBe null
    }

    @Test
    fun `currentUserEmail returns null when principal is the anonymous user sentinel`() {
        SecurityContextHolder.getContext().authentication =
            UsernamePasswordAuthenticationToken("anonymousUser", null, emptyList())

        sut.currentUserEmail() shouldBe null
    }

    @Test
    fun `currentUserEmail returns null when principal UUID does not match any user`() {
        authenticate(UserId(UUID.randomUUID()))

        sut.currentUserEmail() shouldBe null
    }

    @Test
    fun `currentUserEmail returns null when principal is not a valid UUID`() {
        SecurityContextHolder.getContext().authentication =
            UsernamePasswordAuthenticationToken("not-a-uuid", null, emptyList())

        sut.currentUserEmail() shouldBe null
    }

    private fun authenticate(userId: UserId) {
        SecurityContextHolder.getContext().authentication =
            UsernamePasswordAuthenticationToken(userId.value.toString(), null, emptyList())
    }
}
