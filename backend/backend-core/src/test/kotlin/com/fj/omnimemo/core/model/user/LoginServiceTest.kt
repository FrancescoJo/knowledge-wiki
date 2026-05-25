/*
 * LoginServiceTest.kt
 *
 * $Since: 2026-05-25T00:00:00Z
 */
package com.fj.omnimemo.core.model.user

import com.fj.omnimemo.core.test.annotation.SmallTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.Duration
import java.time.Instant
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

@SmallTest
class LoginServiceTest {

    private val repo = MockUserRepository()
    private val hasher = MockPasswordHasher()
    private val tokenIssuer = MockTokenIssuer()
    private val refreshTokenRepo = MockRefreshTokenRepository()
    private val service = LoginService(repo, hasher, tokenIssuer, refreshTokenRepo, Duration.ofDays(14))

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

            val result = service.login("alice@example.com", "secret")

            assertEquals("token:${user.id.value}", result?.accessToken)
            assertNotNull(result?.refreshToken)
        }

        @Test
        fun `should return null when email does not exist`() {
            assertNull(service.login("nobody@example.com", "secret"))
        }

        @Test
        fun `should return null when password is wrong`() {
            val user = User.create("alice@example.com", hasher.hash("secret"))
            repo.save(user)

            assertNull(service.login("alice@example.com", "wrong-secret"))
        }
    }

    @Nested
    inner class Refresh {
        @Test
        fun `should return new LoginResult for a valid refresh token`() {
            val user = User.create("alice@example.com", hasher.hash("secret"))
            repo.save(user)
            val initial = service.login("alice@example.com", "secret")!!

            val result = service.refresh(initial.refreshToken)

            assertNotNull(result)
            assertEquals("token:${user.id.value}", result.accessToken)
        }

        @Test
        fun `should rotate refresh token on use`() {
            val user = User.create("alice@example.com", hasher.hash("secret"))
            repo.save(user)
            val initial = service.login("alice@example.com", "secret")!!
            val rotated = service.refresh(initial.refreshToken)!!

            assertNull(service.refresh(initial.refreshToken))
            assertNotNull(service.refresh(rotated.refreshToken))
        }

        @Test
        fun `should return null for an unknown refresh token`() {
            assertNull(service.refresh("unknown-token"))
        }

        @Test
        fun `should return null and delete an expired refresh token`() {
            val user = User.create("alice@example.com", hasher.hash("secret"))
            repo.save(user)
            val expired = RefreshToken.create(
                token = "expired-token",
                userId = user.id,
                expiresAt = Instant.now().minusSeconds(1),
                createdAt = Instant.now().minusSeconds(100),
            )
            refreshTokenRepo.save(expired)

            assertNull(service.refresh("expired-token"))
            assertNull(refreshTokenRepo.findByToken("expired-token"))
        }
    }
}
