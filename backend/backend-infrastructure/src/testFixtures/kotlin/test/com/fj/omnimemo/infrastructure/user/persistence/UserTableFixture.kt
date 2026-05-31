/*
 * UserTableFixture.kt
 *
 * $Since: 2026-05-26T00:00:00Z
 */
package test.com.fj.omnimemo.infrastructure.user.persistence

import com.fj.omnimemo.core.user.model.UserId
import com.fj.omnimemo.infrastructure.user.persistence.UserRepositoryImpl
import org.springframework.jdbc.core.JdbcTemplate

/**
 * Test helper that performs raw SQL operations against the `users` table.
 *
 * Column and table name literals are sourced from [UserRepositoryImpl] to keep
 * the fixture in sync with the production schema mapping automatically.
 *
 * @author Francesco Jo
 * @since 0.1.1
 * @version 0.1.1
 */
class UserTableFixture(private val jdbc: JdbcTemplate) {
    fun deleteAll() {
        jdbc.update("TRUNCATE ${UserRepositoryImpl.TABLE_NAME} CASCADE")
    }

    fun findEmailEncryptedBytes(id: UserId): ByteArray =
        jdbc.queryForMap(
            "SELECT ${UserRepositoryImpl.COL_EMAIL_ENCRYPTED}" +
                    " FROM ${UserRepositoryImpl.TABLE_NAME}" +
                    " WHERE ${UserRepositoryImpl.COL_ID} = ?",
            id.value,
        )[UserRepositoryImpl.COL_EMAIL_ENCRYPTED] as ByteArray
}
