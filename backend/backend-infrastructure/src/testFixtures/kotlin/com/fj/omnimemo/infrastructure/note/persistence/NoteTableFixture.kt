/*
 * NoteTableFixture.kt
 *
 * $Since: 2026-05-30T00:00:00Z
 */
package com.fj.omnimemo.infrastructure.note.persistence

import com.fj.omnimemo.core.note.model.NoteId
import org.springframework.jdbc.core.JdbcTemplate

/**
 * Test helper that performs raw SQL operations against the `notes`,
 * `note_versions`, and `note_audits` tables.
 *
 * Deletion order respects FK constraints: audits → versions → notes.
 *
 * @author Francesco Jo
 * @since 0.2.0
 * @version 0.2.0
 */
class NoteTableFixture(private val jdbc: JdbcTemplate) {

    fun deleteAll() {
        jdbc.update("DELETE FROM ${NoteAuditRepositoryImpl.TABLE_NAME}")
        jdbc.update("DELETE FROM ${NoteVersionRepositoryImpl.TABLE_NAME}")
        jdbc.update("DELETE FROM ${NoteRepositoryImpl.TABLE_NAME}")
    }

    fun countNotes(): Int =
        jdbc.queryForObject(
            "SELECT COUNT(*) FROM ${NoteRepositoryImpl.TABLE_NAME}",
            Int::class.java,
        ) ?: 0

    fun countVersions(noteId: NoteId): Int =
        jdbc.queryForObject(
            "SELECT COUNT(*) FROM ${NoteVersionRepositoryImpl.TABLE_NAME}" +
                    " WHERE ${NoteVersionRepositoryImpl.COL_NOTE_ID} = ?",
            Int::class.java,
            noteId.value,
        ) ?: 0

    fun isSnapshot(noteId: NoteId, version: Int): Boolean =
        jdbc.queryForObject(
            "SELECT ${NoteVersionRepositoryImpl.COL_IS_SNAPSHOT}" +
                    " FROM ${NoteVersionRepositoryImpl.TABLE_NAME}" +
                    " WHERE ${NoteVersionRepositoryImpl.COL_NOTE_ID} = ?" +
                    " AND ${NoteVersionRepositoryImpl.COL_VERSION} = ?",
            Boolean::class.java,
            noteId.value,
            version,
        ) ?: false
}
