/*
 * RefreshTokenRepositoryImpl.kt
 *
 * $Since: 2026-05-25T00:00:00Z
 */
package com.fj.omnimemo.infrastructure.user.persistence

import com.fj.omnimemo.core.user.model.RefreshToken
import com.fj.omnimemo.core.user.repository.RefreshTokenRepository
import com.fj.omnimemo.core.user.model.UserId
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.core.RowMapper
import org.springframework.stereotype.Repository
import java.sql.Timestamp
import java.util.UUID

/**
 * Spring JDBC implementation of [RefreshTokenRepository].
 *
 * @author Francesco Jo
 * @since 0.1.1
 * @version 0.1.1
 */
@Repository
class RefreshTokenRepositoryImpl(private val jdbc: JdbcTemplate) : RefreshTokenRepository {

    private val rowMapper = RowMapper { rs, _ ->
        RefreshToken.create(
            token = rs.getString(COL_TOKEN),
            userId = UserId(rs.getObject(COL_USER_ID, UUID::class.java)),
            expiresAt = rs.getTimestamp(COL_EXPIRES_AT).toInstant(),
            createdAt = rs.getTimestamp(COL_CREATED_AT).toInstant(),
        )
    }

    override fun save(refreshToken: RefreshToken): RefreshToken {
        jdbc.update(
            """
            INSERT INTO $TABLE_NAME ($COL_TOKEN, $COL_USER_ID, $COL_EXPIRES_AT, $COL_CREATED_AT)
            VALUES (?, ?, ?, ?)
            """.trimIndent(),
            refreshToken.token,
            refreshToken.userId.value,
            Timestamp.from(refreshToken.expiresAt),
            Timestamp.from(refreshToken.createdAt),
        )
        return refreshToken
    }

    override fun findByToken(token: String): RefreshToken? =
        jdbc.query(
            """
            SELECT $COL_TOKEN, $COL_USER_ID, $COL_EXPIRES_AT, $COL_CREATED_AT
            FROM $TABLE_NAME
            WHERE $COL_TOKEN = ?
            """.trimIndent(),
            rowMapper,
            token,
        ).firstOrNull()

    override fun delete(token: String) {
        jdbc.update("DELETE FROM $TABLE_NAME WHERE $COL_TOKEN = ?", token)
    }

    companion object {
        private const val TABLE_NAME = "refresh_tokens"
        private const val COL_TOKEN = "token"
        private const val COL_USER_ID = "user_id"
        private const val COL_EXPIRES_AT = "expires_at"
        private const val COL_CREATED_AT = "created_at"
    }
}
