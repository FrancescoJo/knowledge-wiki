/*
 * UserRepositoryImpl.kt
 *
 * $Since: 2026-05-23T00:00:00Z
 */
package com.fj.omnimemo.infrastructure.persistence.user

import com.fj.omnimemo.core.model.user.User
import com.fj.omnimemo.core.model.user.UserId
import com.fj.omnimemo.core.model.user.UserRepository
import org.springframework.beans.factory.annotation.Value
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.core.RowMapper
import org.springframework.stereotype.Repository
import java.security.SecureRandom
import java.sql.Timestamp
import java.util.*
import javax.crypto.Cipher
import javax.crypto.Mac
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

/**
 * Spring JDBC implementation of [UserRepository].
 *
 * [User.email] is encrypted at rest using AES-256-GCM with a per-record random
 * IV. Email lookups use a separate HMAC-SHA256 blind index so that plaintext
 * email is never stored in the database.
 *
 * Encryption logic is inlined here and will be extracted into a dedicated
 * cipher service in Group 3 (Step 6).
 *
 * @author Francesco Jo
 * @since 0.1.1
 * @version 0.1.1
 */
@Repository
class UserRepositoryImpl(
    private val jdbc: JdbcTemplate,
    @Value("\${app.security.aes-key}") aesKeyBase64: String,
    @Value("\${app.security.hmac-key}") hmacKeyBase64: String,
) : UserRepository {

    private val aesKey: ByteArray = Base64.getDecoder().decode(aesKeyBase64)
    private val hmacKey: ByteArray = Base64.getDecoder().decode(hmacKeyBase64)
    private val secureRandom = SecureRandom()

    private val rowMapper = RowMapper { rs, _ ->
        User.reconstitute(
            id = UserId(rs.getObject(COL_ID, UUID::class.java)),
            email = decryptEmail(rs.getBytes(COL_EMAIL_ENCRYPTED), rs.getBytes(COL_IV)),
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
        return jdbc.query(sql, rowMapper, hmacEmail(email)).firstOrNull()
    }

    override fun save(user: User): User {
        val iv = randomIv()
        val ciphertext = encryptEmail(user.email, iv)
        val hmac = hmacEmail(user.email)
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

    private fun encryptEmail(plaintext: String, iv: ByteArray): ByteArray {
        val cipher = Cipher.getInstance(AES_GCM_ALGORITHM)
        cipher.init(Cipher.ENCRYPT_MODE, SecretKeySpec(aesKey, AES_ALGORITHM), GCMParameterSpec(GCM_TAG_BITS, iv))
        return cipher.doFinal(plaintext.toByteArray(Charsets.UTF_8))
    }

    private fun decryptEmail(ciphertext: ByteArray, iv: ByteArray): String {
        val cipher = Cipher.getInstance(AES_GCM_ALGORITHM)
        cipher.init(Cipher.DECRYPT_MODE, SecretKeySpec(aesKey, AES_ALGORITHM), GCMParameterSpec(GCM_TAG_BITS, iv))
        return String(cipher.doFinal(ciphertext), Charsets.UTF_8)
    }

    private fun hmacEmail(plaintext: String): ByteArray {
        val mac = Mac.getInstance(HMAC_ALGORITHM)
        mac.init(SecretKeySpec(hmacKey, HMAC_ALGORITHM))
        return mac.doFinal(plaintext.toByteArray(Charsets.UTF_8))
    }

    private fun randomIv(): ByteArray {
        val iv = ByteArray(GCM_IV_BYTES)
        secureRandom.nextBytes(iv)
        return iv
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

        private const val AES_GCM_ALGORITHM = "AES/GCM/NoPadding"
        private const val AES_ALGORITHM = "AES"
        private const val HMAC_ALGORITHM = "HmacSHA256"
        private const val GCM_IV_BYTES = 12
        private const val GCM_TAG_BITS = 128
    }
}
