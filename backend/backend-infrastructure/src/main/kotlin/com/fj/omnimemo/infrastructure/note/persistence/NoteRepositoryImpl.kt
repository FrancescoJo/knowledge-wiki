/*
 * NoteRepositoryImpl.kt
 *
 * $Since: 2026-05-30T00:00:00Z
 */
package com.fj.omnimemo.infrastructure.note.persistence

import com.fj.omnimemo.core.note.model.*
import com.fj.omnimemo.core.note.repository.NoteRepository
import com.fj.omnimemo.core.user.model.UserId
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.core.RowMapper
import org.springframework.stereotype.Repository
import java.sql.Timestamp
import java.util.*

/**
 * Spring JDBC implementation of [NoteRepository].
 *
 * @author Francesco Jo
 * @since 0.2.0
 * @version 0.2.0
 */
@Repository
class NoteRepositoryImpl(private val jdbc: JdbcTemplate) : NoteRepository {
    private val rowMapper = RowMapper { rs, _ ->
        val softDeletedBy = rs.getObject(COL_SOFT_DELETED_BY, UUID::class.java)
        val softDeletedAt = rs.getTimestamp(COL_SOFT_DELETED_AT)
        Note.reconstitute(
            id = NoteId(rs.getObject(COL_ID, UUID::class.java)),
            language = NoteLanguage.fromCode(rs.getString(COL_LANGUAGE)),
            title = rs.getString(COL_TITLE),
            titleIndex = rs.getString(COL_TITLE_INDEX),
            accessLevel = NoteAccessLevel.valueOf(rs.getString(COL_ACCESS_LEVEL)),
            status = NoteStatus.valueOf(rs.getString(COL_STATUS)),
            currentVersion = rs.getInt(COL_CURRENT_VERSION),
            authorId = UserId(rs.getObject(COL_AUTHOR_ID, UUID::class.java)),
            softDeletedBy = softDeletedBy?.let { UserId(it) },
            softDeletedAt = softDeletedAt?.toInstant(),
            createdAt = rs.getTimestamp(COL_CREATED_AT).toInstant(),
            updatedAt = rs.getTimestamp(COL_UPDATED_AT).toInstant(),
        )
    }

    override fun findById(id: NoteId): Note? {
        val sql = """
            SELECT $SELECT_COLUMNS
            FROM $TABLE_NAME
            WHERE $COL_ID = ?
        """.trimIndent()
        return jdbc.query(sql, rowMapper, id.value).firstOrNull()
    }

    override fun findByTitle(title: String): Note? {
        val sql = """
            SELECT $SELECT_COLUMNS
            FROM $TABLE_NAME
            WHERE $COL_TITLE = ?
        """.trimIndent()
        return jdbc.query(sql, rowMapper, title).firstOrNull()
    }

    override fun findAllByLanguage(language: NoteLanguage): List<Note> {
        val sql = """
            SELECT $SELECT_COLUMNS
            FROM $TABLE_NAME
            WHERE $COL_LANGUAGE = ?
            ORDER BY $COL_TITLE_INDEX, $COL_TITLE
        """.trimIndent()
        return jdbc.query(sql, rowMapper, language.code)
    }

    override fun existsByTitle(title: String): Boolean =
        jdbc.queryForObject(
            "SELECT EXISTS(SELECT 1 FROM $TABLE_NAME WHERE $COL_TITLE = ?)",
            Boolean::class.java,
            title,
        )

    override fun save(note: Note): Note {
        if (note.isNew) {
            val sql = """
                INSERT INTO $TABLE_NAME (
                    $COL_ID, $COL_LANGUAGE, $COL_TITLE, $COL_TITLE_INDEX,
                    $COL_ACCESS_LEVEL, $COL_STATUS, $COL_CURRENT_VERSION,
                    $COL_AUTHOR_ID, $COL_SOFT_DELETED_BY, $COL_SOFT_DELETED_AT,
                    $COL_CREATED_AT, $COL_UPDATED_AT
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """.trimIndent()
            jdbc.update(
                sql,
                note.id.value, note.language.code, note.title, note.titleIndex,
                note.accessLevel.name, note.status.name, note.currentVersion,
                note.authorId.value, note.softDeletedBy?.value, note.softDeletedAt?.let { Timestamp.from(it) },
                Timestamp.from(note.createdAt), Timestamp.from(note.updatedAt),
            )
        } else {
            val sql = """
                UPDATE $TABLE_NAME SET
                    $COL_TITLE = ?, $COL_TITLE_INDEX = ?, $COL_ACCESS_LEVEL = ?,
                    $COL_STATUS = ?, $COL_CURRENT_VERSION = ?,
                    $COL_SOFT_DELETED_BY = ?, $COL_SOFT_DELETED_AT = ?,
                    $COL_UPDATED_AT = ?
                WHERE $COL_ID = ?
            """.trimIndent()
            jdbc.update(
                sql,
                note.title, note.titleIndex, note.accessLevel.name,
                note.status.name, note.currentVersion,
                note.softDeletedBy?.value, note.softDeletedAt?.let { Timestamp.from(it) },
                Timestamp.from(note.updatedAt),
                note.id.value,
            )
        }
        return note
    }

    companion object {
        internal const val TABLE_NAME = "notes"
        internal const val COL_ID = "id"
        internal const val COL_LANGUAGE = "language"
        internal const val COL_TITLE = "title"
        internal const val COL_TITLE_INDEX = "title_index"
        internal const val COL_ACCESS_LEVEL = "access_level"
        internal const val COL_STATUS = "status"
        internal const val COL_CURRENT_VERSION = "current_version"
        internal const val COL_AUTHOR_ID = "author_id"
        internal const val COL_SOFT_DELETED_BY = "soft_deleted_by"
        internal const val COL_SOFT_DELETED_AT = "soft_deleted_at"
        internal const val COL_CREATED_AT = "created_at"
        internal const val COL_UPDATED_AT = "updated_at"

        private val SELECT_COLUMNS = listOf(
            COL_ID, COL_LANGUAGE, COL_TITLE, COL_TITLE_INDEX,
            COL_ACCESS_LEVEL, COL_STATUS, COL_CURRENT_VERSION,
            COL_AUTHOR_ID, COL_SOFT_DELETED_BY, COL_SOFT_DELETED_AT,
            COL_CREATED_AT, COL_UPDATED_AT,
        ).joinToString(", ")
    }
}
