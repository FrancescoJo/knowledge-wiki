/*
 * UserRepositoryImplTest.kt
 *
 * $Since: 2026-05-23T00:00:00Z
 */
package com.fj.omnimemo.infrastructure.user.persistence

import com.fj.omnimemo.core.user.model.User
import com.fj.omnimemo.core.user.model.UserId
import com.fj.omnimemo.core.user.mutate
import com.fj.omnimemo.infrastructure.security.AesGcmCipher
import com.fj.omnimemo.infrastructure.security.HmacBlindIndex
import com.fj.omnimemo.infrastructure.test.PostgresContainerSupport
import com.fj.omnimemo.core.test.annotation.MediumTest
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.datasource.DriverManagerDataSource
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull

@MediumTest
@Testcontainers
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class UserRepositoryImplTest {

    companion object {
        @Container
        @JvmField
        val postgres: PostgreSQLContainer<*> = PostgresContainerSupport.newContainer()

        private val TEST_AES_CIPHER = AesGcmCipher(ByteArray(32) { it.toByte() })
        private val TEST_HMAC_INDEX = HmacBlindIndex(ByteArray(32) { (it + 32).toByte() })

        private fun loadCreateTableSql(): String {
            val stream = UserRepositoryImplTest::class.java.classLoader
                .getResourceAsStream("db/changelog/v0.1/create-users-table.sql")
                ?: error("create-users-table.sql not found on classpath")
            return stream.bufferedReader().use { reader ->
                reader.readLines()
                    .filter { !it.startsWith("--liquibase") && !it.startsWith("--changeset") }
                    .joinToString("\n")
                    .trim()
            }
        }
    }

    private lateinit var jdbc: JdbcTemplate
    private lateinit var repo: UserRepositoryImpl

    @BeforeAll
    fun setUpDatabase() {
        val ds = DriverManagerDataSource(postgres.jdbcUrl, postgres.username, postgres.password)
        jdbc = JdbcTemplate(ds)
        jdbc.execute(loadCreateTableSql())
        repo = UserRepositoryImpl(jdbc, TEST_AES_CIPHER, TEST_HMAC_INDEX)
    }

    @BeforeEach
    fun cleanUsers() {
        jdbc.update("DELETE FROM users")
    }

    @Nested
    inner class FindById {
        @Test
        fun `should return user when user is saved`() {
            val user = User.create("alice@example.com", "hash")
            repo.save(user)

            val found = repo.findById(user.id)

            assertNotNull(found)
            assertEquals(user.id, found.id)
            assertEquals("alice@example.com", found.email)
            assertEquals("hash", found.passwordHash)
        }

        @Test
        fun `should return null when user does not exist`() {
            assertNull(repo.findById(UserId.generate()))
        }
    }

    @Nested
    inner class FindByEmail {
        @Test
        fun `should return user when email matches`() {
            val user = User.create("alice@example.com", "hash")
            repo.save(user)

            val found = repo.findByEmail("alice@example.com")

            assertNotNull(found)
            assertEquals(user.id, found.id)
        }

        @Test
        fun `should return null when email does not match`() {
            assertNull(repo.findByEmail("nobody@example.com"))
        }
    }

    @Test
    fun `save should store email as ciphertext not plaintext`() {
        val user = User.create("alice@example.com", "hash")
        repo.save(user)

        val row = jdbc.queryForMap("SELECT email_encrypted FROM users WHERE id = ?", user.id.value)
        val storedBytes = row["email_encrypted"] as ByteArray

        assertFalse(storedBytes.contentEquals("alice@example.com".toByteArray(Charsets.UTF_8)))
    }

    @Test
    fun `save should update existing user when user is persisted`() {
        val user = User.create("alice@example.com", "hash1")
        repo.save(user)
        val persisted = repo.findById(user.id)!!

        val updated = persisted.mutate().also { it.passwordHash = "hash2" }
        repo.save(updated)

        assertEquals("hash2", repo.findById(user.id)?.passwordHash)
    }

    @Test
    fun `delete should remove user when user exists`() {
        val user = User.create("alice@example.com", "hash")
        repo.save(user)
        repo.delete(user.id)

        assertNull(repo.findById(user.id))
    }
}
