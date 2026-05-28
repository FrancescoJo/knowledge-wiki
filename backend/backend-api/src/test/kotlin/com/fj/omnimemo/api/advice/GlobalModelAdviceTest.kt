/*
 * GlobalModelAdviceTest.kt
 *
 * $Since: 2026-05-29T00:00:00Z
 */
package com.fj.omnimemo.api.advice

import com.fj.omnimemo.core.test.annotation.SmallTest
import com.fj.omnimemo.core.user.model.User
import com.fj.omnimemo.core.user.model.UserId
import com.fj.omnimemo.core.user.repository.MockUserRepository
import com.fj.omnimemo.core.user.usecase.FindUserUseCase
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.springframework.context.support.StaticMessageSource
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder
import java.util.Locale
import java.util.UUID

/**
 * Small tests for [GlobalModelAdvice.currentUserEmail].
 *
 * @author Francesco Jo
 * @since 0.1.1
 */
@SmallTest
class GlobalModelAdviceTest {

    private val userRepo = MockUserRepository()
    private val findUserUseCase = FindUserUseCase(userRepo)
    private val messageSource = StaticMessageSource().apply {
        addMessage("nav.home", Locale.ENGLISH, "Home")
    }
    private val advice = GlobalModelAdvice("local", messageSource, findUserUseCase)

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

        advice.currentUserEmail() shouldBe "alice@example.com"
    }

    @Test
    fun `currentUserEmail returns null when SecurityContext has no authentication`() {
        SecurityContextHolder.clearContext()

        advice.currentUserEmail() shouldBe null
    }

    @Test
    fun `currentUserEmail returns null when principal is the anonymous user sentinel`() {
        SecurityContextHolder.getContext().authentication =
            UsernamePasswordAuthenticationToken("anonymousUser", null, emptyList())

        advice.currentUserEmail() shouldBe null
    }

    @Test
    fun `currentUserEmail returns null when principal UUID does not match any user`() {
        authenticate(UserId(UUID.randomUUID()))

        advice.currentUserEmail() shouldBe null
    }

    @Test
    fun `currentUserEmail returns null when principal is not a valid UUID`() {
        SecurityContextHolder.getContext().authentication =
            UsernamePasswordAuthenticationToken("not-a-uuid", null, emptyList())

        advice.currentUserEmail() shouldBe null
    }

    private fun authenticate(userId: UserId) {
        SecurityContextHolder.getContext().authentication =
            UsernamePasswordAuthenticationToken(userId.value.toString(), null, emptyList())
    }
}
