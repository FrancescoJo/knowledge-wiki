/*
 * UserRepositoryImplTest.kt
 *
 * $Since: 2026-05-23T00:00:00Z
 */
package testcase.medium.infrastructure.user.persistence

import com.fj.omnimemo.core.user.model.User
import com.fj.omnimemo.core.user.model.UserId
import com.fj.omnimemo.core.user.mutate
import com.fj.omnimemo.infrastructure.security.AesGcmCipher
import com.fj.omnimemo.infrastructure.security.HmacBlindIndex
import com.fj.omnimemo.infrastructure.user.persistence.UserRepositoryImpl
import io.kotest.assertions.assertSoftly
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import test.com.fj.omnimemo.core.annotation.MediumTest
import testcase.medium.infrastructure.InfrastructureTestDatabase
import test.com.fj.omnimemo.infrastructure.user.persistence.UserTableFixture

/**
 * Medium Tests for [UserRepositoryImpl]: verifies persistence behaviour against
 * a real PostgreSQL instance shared via [InfrastructureTestDatabase].
 *
 * @author Francesco Jo
 * @since 0.1.1
 * @version 0.1.1
 */
@MediumTest
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class UserRepositoryImplTest {
    private val jdbc = InfrastructureTestDatabase.jdbc
    private val repo = UserRepositoryImpl(jdbc, TEST_AES_CIPHER, TEST_HMAC_INDEX)
    private val users = UserTableFixture(jdbc)

    @BeforeEach
    fun cleanUsers() {
        users.deleteAll()
    }

    @Nested
    inner class FindById {
        @Test
        fun `should return user when user is saved`() {
            val user = User.create("alice@example.com", "hash")
            repo.save(user)

            val found = repo.findById(user.id)

            assertSoftly {
                found shouldNotBe null
                found?.id shouldBe user.id
                found?.email shouldBe "alice@example.com"
                found?.passwordHash shouldBe "hash"
            }
        }

        @Test
        fun `should return null when user does not exist`() {
            repo.findById(UserId.generate()) shouldBe null
        }
    }

    @Nested
    inner class FindByEmail {
        @Test
        fun `should return user when email matches`() {
            val user = User.create("alice@example.com", "hash")
            repo.save(user)

            val found = repo.findByEmail("alice@example.com")

            assertSoftly {
                found shouldNotBe null
                found?.id shouldBe user.id
            }
        }

        @Test
        fun `should return null when email does not match`() {
            repo.findByEmail("nobody@example.com") shouldBe null
        }
    }

    @Test
    fun `save should store email as ciphertext not plaintext`() {
        val user = User.create("alice@example.com", "hash")
        repo.save(user)

        val storedBytes = users.findEmailEncryptedBytes(user.id)

        storedBytes.contentEquals("alice@example.com".toByteArray(Charsets.UTF_8)) shouldBe false
    }

    @Test
    fun `save should update existing user when user is persisted`() {
        val user = User.create("alice@example.com", "hash1")
        repo.save(user)
        val persisted = repo.findById(user.id)!!

        val updated = persisted.mutate().also { it.passwordHash = "hash2" }
        repo.save(updated)

        repo.findById(user.id)?.passwordHash shouldBe "hash2"
    }

    @Test
    fun `delete should remove user when user exists`() {
        val user = User.create("alice@example.com", "hash")
        repo.save(user)
        repo.delete(user.id)

        repo.findById(user.id) shouldBe null
    }

    @Nested
    inner class HasAny {
        @Test
        fun `should return false when no users exist`() {
            repo.hasAny() shouldBe false
        }

        @Test
        fun `should return true when at least one user exists`() {
            repo.save(User.create("alice@example.com", "hash"))

            repo.hasAny() shouldBe true
        }
    }

    companion object {
        private val TEST_AES_CIPHER = AesGcmCipher(ByteArray(32) { it.toByte() })
        private val TEST_HMAC_INDEX = HmacBlindIndex(ByteArray(32) { (it + 32).toByte() })
    }
}
