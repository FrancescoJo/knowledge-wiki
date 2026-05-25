/*
 * UserRepositoryImpl.kt
 *
 * $Since: 2026-05-23T00:00:00Z
 */
package com.fj.omnimemo.infrastructure.user.persistence

import com.fj.omnimemo.core.user.model.User
import com.fj.omnimemo.core.user.model.UserId
import com.fj.omnimemo.core.user.model.UserRepository
import com.fj.omnimemo.infrastructure.security.AesGcmCipher
import com.fj.omnimemo.infrastructure.security.HmacBlindIndex
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.core.RowMapper
import org.springframework.stereotype.Repository
import java.sql.Timestamp
import java.util.*

/**
 * Spring JDBC implementation of [UserRepository].
 *
 * [User.email] is encrypted at rest using AES-256-GCM with a per-record random
 * IV. Email lookups use a separate HMAC-SHA256 blind index so that plaintext
 * email is never stored in the database.
 *
 * @author Francesco Jo
 * @since 0.1.1
 * @version 0.1.1
 */
@Repository
class UserRepositoryImpl(
    private val jdbc: JdbcTemplate,
    private val aesCipher: AesGcmCipher,
    private val hmacIndex: HmacBlindIndex,
) : UserRepository {

    private val rowMapper = RowMapper { rs, _ ->
        User.reconstitute(
            id = UserId(rs.getObject(COL_ID, UUID::class.java)),
            email = aesCipher.decrypt(rs.getBytes(COL_EMAIL_ENCRYPTED), rs.getBytes(COL_IV)),
            passwordHash = rs.getString(COL_PASSWORD_HASH),
            createdAt = rs.getTimestamp(COL_CREATED_AT).toInstant(),
            updatedAt = rs.getTimestamp(COL_UPDATED_AT).toInstant(),
        )
    }

    override fun findById(id: UserId): User? {
        val sql = """
            SELECT
                $COL_ID,
                $COL_EMAIL_ENCRYPTED,
                $COL_EMAIL_HMAC,
                $COL_PASSWORD_HASH,
                $COL_IV,
                $COL_CREATED_AT,
                $COL_UPDATED_AT
            FROM $TABLE_NAME
            WHERE $COL_ID = ?
        """.trimIndent()
        return jdbc.query(sql, rowMapper, id.value).firstOrNull()
    }

    override fun findByEmail(email: String): User? {
        val sql = """
            SELECT
                $COL_ID,
                $COL_EMAIL_ENCRYPTED,
                $COL_EMAIL_HMAC,
                $COL_PASSWORD_HASH,
                $COL_IV,
                $COL_CREATED_AT,
                $COL_UPDATED_AT
            FROM $TABLE_NAME
            WHERE $COL_EMAIL_HMAC = ?
        """.trimIndent()
        return jdbc.query(sql, rowMapper, hmacIndex.compute(email)).firstOrNull()
    }

    override fun save(user: User): User {
        val iv = aesCipher.generateIv()
        val ciphertext = aesCipher.encrypt(user.email, iv)
        val hmac = hmacIndex.compute(user.email)
        if (user.isNew) {
            val sql = """
                INSERT INTO $TABLE_NAME (
                    $COL_ID,
                    $COL_EMAIL_ENCRYPTED,
                    $COL_EMAIL_HMAC,
                    $COL_PASSWORD_HASH,
                    $COL_IV,
                    $COL_CREATED_AT,
                    $COL_UPDATED_AT
                ) VALUES (?, ?, ?, ?, ?, ?, ?)
            """.trimIndent()
            jdbc.update(
                sql,
                user.id.value, ciphertext, hmac, user.passwordHash, iv,
                Timestamp.from(user.createdAt), Timestamp.from(user.updatedAt),
            )
        } else {
            val sql = """
                UPDATE $TABLE_NAME
                SET
                    $COL_EMAIL_ENCRYPTED = ?,
                    $COL_EMAIL_HMAC = ?,
                    $COL_PASSWORD_HASH = ?,
                    $COL_IV = ?,
                    $COL_UPDATED_AT = ?
                WHERE $COL_ID = ?
            """.trimIndent()
            jdbc.update(
                sql,
                ciphertext, hmac, user.passwordHash, iv, Timestamp.from(user.updatedAt),
                user.id.value,
            )
        }
        return user
    }

    override fun delete(id: UserId) {
        jdbc.update("""
            DELETE FROM $TABLE_NAME
            WHERE $COL_ID = ?
        """.trimIndent(), id.value)
    }

    companion object {
        private const val TABLE_NAME = "users"
        private const val COL_ID = "id"
        private const val COL_EMAIL_ENCRYPTED = "email_encrypted"
        private const val COL_EMAIL_HMAC = "email_hmac"
        private const val COL_PASSWORD_HASH = "password_hash"
        private const val COL_IV = "iv"
        private const val COL_CREATED_AT = "created_at"
        private const val COL_UPDATED_AT = "updated_at"
    }
}
