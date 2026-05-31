/*
 * NoteVersionRepositoryImpl.kt
 *
 * $Since: 2026-05-30T00:00:00Z
 */
package com.fj.omnimemo.infrastructure.note.persistence

import com.fj.omnimemo.core.note.model.NoteId
import com.fj.omnimemo.core.note.model.NoteVersion
import com.fj.omnimemo.core.note.repository.NoteVersionRepository
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.core.RowMapper
import org.springframework.stereotype.Repository

/**
 * Spring JDBC implementation of [NoteVersionRepository].
 *
 * Version 1 and every tenth version (10, 20, …) are stored as full-content
 * snapshots ([NoteVersion.isSnapshot] = true). All other versions are stored as
 * unified diff patches relative to the preceding content.
 *
 * [findContent] reconstructs the full text by locating the nearest preceding
 * snapshot and replaying any delta patches in order.
 *
 * @author Francesco Jo
 * @since 0.2.0
 * @version 0.2.0
 */
@Repository
class NoteVersionRepositoryImpl(private val jdbc: JdbcTemplate) : NoteVersionRepository {
    private val rowMapper = RowMapper { rs, _ ->
        NoteVersion.reconstitute(
            noteId = NoteId(rs.getObject(COL_NOTE_ID, java.util.UUID::class.java)),
            version = rs.getInt(COL_VERSION),
            content = rs.getString(COL_CONTENT),
            isSnapshot = rs.getBoolean(COL_IS_SNAPSHOT),
            createdAt = rs.getTimestamp(COL_CREATED_AT).toInstant(),
        )
    }

    override fun saveContent(noteId: NoteId, version: Int, fullContent: String) {
        val isSnapshot = version == 1 || version % 10 == 0
        val storedContent = if (isSnapshot) {
            fullContent
        } else {
            val previousContent = findContent(noteId, version - 1).orEmpty()
            MarkdownPatchCodec.generatePatch(previousContent, fullContent)
        }
        val sql = """
            INSERT INTO $TABLE_NAME ($COL_NOTE_ID, $COL_VERSION, $COL_CONTENT, $COL_IS_SNAPSHOT, $COL_CREATED_AT)
            VALUES (?, ?, ?, ?, NOW())
        """.trimIndent()
        jdbc.update(sql, noteId.value, version, storedContent, isSnapshot)
    }

    override fun findContent(noteId: NoteId, version: Int): String? {
        val versions = findVersionRange(noteId, version) ?: return null
        var content = versions.first().content
        for (v in versions.drop(1)) {
            content = MarkdownPatchCodec.applyPatch(content, v.content)
        }
        return content
    }

    override fun findAllByNoteId(noteId: NoteId): List<NoteVersion> {
        val sql = """
            SELECT $COL_NOTE_ID, $COL_VERSION, $COL_CONTENT, $COL_IS_SNAPSHOT, $COL_CREATED_AT
            FROM $TABLE_NAME
            WHERE $COL_NOTE_ID = ?
            ORDER BY $COL_VERSION
        """.trimIndent()
        return jdbc.query(sql, rowMapper, noteId.value)
    }

    /**
     * Returns the rows needed to reconstruct [targetVersion]: the nearest
     * preceding snapshot (inclusive of [targetVersion] itself if it is a
     * snapshot) and all delta rows up to and including [targetVersion].
     * Returns null when no record exists for [targetVersion].
     */
    @Suppress("ReturnCount")
    private fun findVersionRange(noteId: NoteId, targetVersion: Int): List<NoteVersion>? {
        val snapshotSql = """
            SELECT $COL_NOTE_ID, $COL_VERSION, $COL_CONTENT, $COL_IS_SNAPSHOT, $COL_CREATED_AT
            FROM $TABLE_NAME
            WHERE $COL_NOTE_ID = ? AND $COL_VERSION <= ? AND $COL_IS_SNAPSHOT = TRUE
            ORDER BY $COL_VERSION DESC
            LIMIT 1
        """.trimIndent()
        val snapshot = jdbc.query(snapshotSql, rowMapper, noteId.value, targetVersion).firstOrNull()
            ?: return null

        if (snapshot.version == targetVersion) return listOf(snapshot)

        val deltaSql = """
            SELECT $COL_NOTE_ID, $COL_VERSION, $COL_CONTENT, $COL_IS_SNAPSHOT, $COL_CREATED_AT
            FROM $TABLE_NAME
            WHERE $COL_NOTE_ID = ? AND $COL_VERSION > ? AND $COL_VERSION <= ? AND $COL_IS_SNAPSHOT = FALSE
            ORDER BY $COL_VERSION
        """.trimIndent()
        val deltas = jdbc.query(deltaSql, rowMapper, noteId.value, snapshot.version, targetVersion)

        val lastDelta = deltas.lastOrNull() ?: return null
        if (lastDelta.version != targetVersion) return null

        return listOf(snapshot) + deltas
    }

    companion object {
        internal const val TABLE_NAME = "note_versions"
        internal const val COL_NOTE_ID = "note_id"
        internal const val COL_VERSION = "version"
        internal const val COL_CONTENT = "content"
        internal const val COL_IS_SNAPSHOT = "is_snapshot"
        internal const val COL_CREATED_AT = "created_at"
    }
}
