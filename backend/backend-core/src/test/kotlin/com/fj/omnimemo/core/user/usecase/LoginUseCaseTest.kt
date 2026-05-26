/*
 * LoginUseCaseTest.kt
 *
 * $Since: 2026-05-25T00:00:00Z
 */
package com.fj.omnimemo.core.user.usecase

import com.fj.omnimemo.core.user.exception.PasswordMismatchException
import com.fj.omnimemo.core.user.exception.RefreshTokenNotFoundException
import com.fj.omnimemo.core.user.exception.TokenExpiredException
import com.fj.omnimemo.core.security.MockTokenIssuer
import com.fj.omnimemo.core.test.annotation.SmallTest
import com.fj.omnimemo.core.user.model.RefreshToken
import com.fj.omnimemo.core.user.model.User
import com.fj.omnimemo.core.user.repository.MockRefreshTokenRepository
import com.fj.omnimemo.core.user.repository.MockUserRepository
import com.fj.omnimemo.core.user.security.MockPasswordHasher
import io.kotest.assertions.assertSoftly
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.Duration
import java.time.Instant

@SmallTest
class LoginUseCaseTest {

    private val repo = MockUserRepository()
    private val hasher = MockPasswordHasher()
    private val tokenIssuer = MockTokenIssuer()
    private val refreshTokenRepo = MockRefreshTokenRepository()
    private val useCase = LoginUseCase(repo, hasher, tokenIssuer, refreshTokenRepo, Duration.ofDays(14))

    @BeforeEach
    fun setUp() {
        repo.clear()
        refreshTokenRepo.clear()
    }

    @Nested
    inner class Login {
        @Test
        fun `should return LoginResult with both tokens for valid credentials`() {
            val user = User.create("alice@example.com", hasher.hash("secret"))
            repo.save(user)

            val result = useCase.login("alice@example.com", "secret")

            assertSoftly {
                result.accessToken shouldBe "token:${user.id.value}"
                result.refreshToken shouldNotBe null
            }
        }

        @Test
        fun `should throw PasswordMismatchException when email does not exist`() {
            shouldThrow<PasswordMismatchException> {
                useCase.login("nobody@example.com", "secret")
            }
        }

        @Test
        fun `should throw PasswordMismatchException when password is wrong`() {
            val user = User.create("alice@example.com", hasher.hash("secret"))
            repo.save(user)

            shouldThrow<PasswordMismatchException> {
                useCase.login("alice@example.com", "wrong-secret")
            }
        }
    }

    @Nested
    inner class Logout {
        @Test
        fun `should delete refresh token on logout`() {
            val user = User.create("alice@example.com", hasher.hash("secret"))
            repo.save(user)
            val loginResult = useCase.login("alice@example.com", "secret")

            useCase.logout(loginResult.refreshToken)

            refreshTokenRepo.findByToken(loginResult.refreshToken) shouldBe null
        }

        @Test
        fun `should not throw when refresh token does not exist`() {
            useCase.logout("unknown-token")
        }
    }

    @Nested
    inner class Refresh {
        @Test
        fun `should return new LoginResult for a valid refresh token`() {
            val user = User.create("alice@example.com", hasher.hash("secret"))
            repo.save(user)
            val initial = useCase.login("alice@example.com", "secret")

            val result = useCase.refresh(initial.refreshToken)

            result.accessToken shouldBe "token:${user.id.value}"
        }

        @Test
        fun `should rotate refresh token on use`() {
            val user = User.create("alice@example.com", hasher.hash("secret"))
            repo.save(user)
            val initial = useCase.login("alice@example.com", "secret")
            val rotated = useCase.refresh(initial.refreshToken)

            shouldThrow<RefreshTokenNotFoundException> { useCase.refresh(initial.refreshToken) }
            useCase.refresh(rotated.refreshToken).accessToken shouldBe "token:${user.id.value}"
        }

        @Test
        fun `should throw RefreshTokenNotFoundException for an unknown refresh token`() {
            shouldThrow<RefreshTokenNotFoundException> {
                useCase.refresh("unknown-token")
            }
        }

        @Test
        fun `should throw TokenExpiredException and delete an expired refresh token`() {
            val user = User.create("alice@example.com", hasher.hash("secret"))
            repo.save(user)
            val expired = RefreshToken.create(
                token = "expired-token",
                userId = user.id,
                expiresAt = Instant.now().minusSeconds(1),
                createdAt = Instant.now().minusSeconds(100),
            )
            refreshTokenRepo.save(expired)

            assertSoftly {
                shouldThrow<TokenExpiredException> { useCase.refresh("expired-token") }
                refreshTokenRepo.findByToken("expired-token") shouldBe null
            }
        }
    }
}
