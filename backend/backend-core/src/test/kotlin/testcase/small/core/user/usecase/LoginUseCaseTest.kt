/*
 * LoginUseCaseTest.kt
 *
 * $Since: 2026-05-25T00:00:00Z
 */
package testcase.small.core.user.usecase

import com.fj.omnimemo.core.user.exception.PasswordMismatchException
import com.fj.omnimemo.core.user.exception.RefreshTokenNotFoundException
import com.fj.omnimemo.core.user.exception.TokenExpiredException
import com.fj.omnimemo.core.user.model.RefreshToken
import com.fj.omnimemo.core.user.model.User
import com.fj.omnimemo.core.user.usecase.LoginUseCase
import io.kotest.assertions.assertSoftly
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import test.com.fj.omnimemo.core.annotation.SmallTest
import test.com.fj.omnimemo.core.security.MockTokenIssuer
import test.com.fj.omnimemo.core.user.repository.MockRefreshTokenRepository
import test.com.fj.omnimemo.core.user.repository.MockUserRepository
import test.com.fj.omnimemo.core.user.security.MockPasswordHasher
import java.time.Duration
import java.time.Instant

@SmallTest
class LoginUseCaseTest {
    private lateinit var repo: MockUserRepository
    private lateinit var hasher: MockPasswordHasher
    private lateinit var tokenIssuer: MockTokenIssuer
    private lateinit var refreshTokenRepo: MockRefreshTokenRepository
    private lateinit var sut: LoginUseCase

    @BeforeEach
    fun setUp() {
        repo = MockUserRepository()
        hasher = MockPasswordHasher()
        tokenIssuer = MockTokenIssuer()
        refreshTokenRepo = MockRefreshTokenRepository()
        sut = LoginUseCase(repo, hasher, tokenIssuer, refreshTokenRepo, Duration.ofDays(14))
    }

    @AfterEach
    fun tearDown() {
        repo.clear()
        refreshTokenRepo.clear()
    }

    @Nested
    inner class Login {
        @Test
        fun `should return LoginResult with both tokens for valid credentials`() {
            val user = User.create("alice@example.com", hasher.hash("secret"))
            repo.save(user)

            val result = sut.login("alice@example.com", "secret")

            assertSoftly {
                result.accessToken shouldBe "token:${user.id.value}"
                result.refreshToken shouldNotBe null
            }
        }

        @Test
        fun `should throw PasswordMismatchException when email does not exist`() {
            shouldThrow<PasswordMismatchException> {
                sut.login("nobody@example.com", "secret")
            }
        }

        @Test
        fun `should throw PasswordMismatchException when password is wrong`() {
            val user = User.create("alice@example.com", hasher.hash("secret"))
            repo.save(user)

            shouldThrow<PasswordMismatchException> {
                sut.login("alice@example.com", "wrong-secret")
            }
        }
    }

    @Nested
    inner class Logout {
        @Test
        fun `should delete refresh token on logout`() {
            val user = User.create("alice@example.com", hasher.hash("secret"))
            repo.save(user)
            val loginResult = sut.login("alice@example.com", "secret")

            sut.logout(loginResult.refreshToken)

            refreshTokenRepo.findByToken(loginResult.refreshToken) shouldBe null
        }

        @Test
        fun `should not throw when refresh token does not exist`() {
            sut.logout("unknown-token")
        }
    }

    @Nested
    inner class Refresh {
        @Test
        fun `should return new LoginResult for a valid refresh token`() {
            val user = User.create("alice@example.com", hasher.hash("secret"))
            repo.save(user)
            val initial = sut.login("alice@example.com", "secret")

            val result = sut.refresh(initial.refreshToken)

            result.accessToken shouldBe "token:${user.id.value}"
        }

        @Test
        fun `should rotate refresh token on use`() {
            val user = User.create("alice@example.com", hasher.hash("secret"))
            repo.save(user)
            val initial = sut.login("alice@example.com", "secret")
            val rotated = sut.refresh(initial.refreshToken)

            shouldThrow<RefreshTokenNotFoundException> { sut.refresh(initial.refreshToken) }
            sut.refresh(rotated.refreshToken).accessToken shouldBe "token:${user.id.value}"
        }

        @Test
        fun `should throw RefreshTokenNotFoundException for an unknown refresh token`() {
            shouldThrow<RefreshTokenNotFoundException> {
                sut.refresh("unknown-token")
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
                shouldThrow<TokenExpiredException> { sut.refresh("expired-token") }
                refreshTokenRepo.findByToken("expired-token") shouldBe null
            }
        }
    }
}
