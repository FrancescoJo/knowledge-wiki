/*
 * NoteAuditRepositoryImpl.kt
 *
 * $Since: 2026-05-30T00:00:00Z
 */
package com.fj.omnimemo.infrastructure.note.persistence

import com.fj.omnimemo.core.note.model.NoteAction
import com.fj.omnimemo.core.note.model.NoteAudit
import com.fj.omnimemo.core.note.model.NoteId
import com.fj.omnimemo.core.note.repository.NoteAuditRepository
import com.fj.omnimemo.core.user.model.UserId
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.core.RowMapper
import org.springframework.stereotype.Repository
import java.util.*

/**
 * Spring JDBC implementation of [NoteAuditRepository].
 *
 * Audit entries are append-only; no update or delete operations are provided.
 *
 * @author Francesco Jo
 * @since 0.2.0
 * @version 0.2.0
 */
@Repository
class NoteAuditRepositoryImpl(private val jdbc: JdbcTemplate) : NoteAuditRepository {
    private val rowMapper = RowMapper { rs, _ ->
        NoteAudit.reconstitute(
            id = rs.getObject(COL_ID, UUID::class.java),
            noteId = NoteId(rs.getObject(COL_NOTE_ID, UUID::class.java)),
            version = rs.getInt(COL_VERSION),
            action = NoteAction.valueOf(rs.getString(COL_ACTION)),
            actorId = UserId(rs.getObject(COL_ACTOR_ID, UUID::class.java)),
            remoteIp = rs.getString(COL_REMOTE_IP),
            summary = rs.getString(COL_SUMMARY),
            createdAt = rs.getTimestamp(COL_CREATED_AT).toInstant(),
        )
    }

    override fun save(audit: NoteAudit) {
        val sql = """
            INSERT INTO $TABLE_NAME (
                $COL_ID, $COL_NOTE_ID, $COL_VERSION, $COL_ACTION,
                $COL_ACTOR_ID, $COL_REMOTE_IP, $COL_SUMMARY, $COL_CREATED_AT
            ) VALUES (?, ?, ?, ?, ?, ?::INET, ?, NOW())
        """.trimIndent()
        jdbc.update(
            sql,
            audit.id, audit.noteId.value, audit.version, audit.action.name,
            audit.actorId.value, audit.remoteIp, audit.summary,
        )
    }

    override fun findAllByNoteId(noteId: NoteId): List<NoteAudit> {
        val sql = """
            SELECT $COL_ID, $COL_NOTE_ID, $COL_VERSION, $COL_ACTION,
                   $COL_ACTOR_ID, host($COL_REMOTE_IP) AS $COL_REMOTE_IP, $COL_SUMMARY, $COL_CREATED_AT
            FROM $TABLE_NAME
            WHERE $COL_NOTE_ID = ?
            ORDER BY $COL_CREATED_AT
        """.trimIndent()
        return jdbc.query(sql, rowMapper, noteId.value)
    }

    companion object {
        internal const val TABLE_NAME = "note_audits"
        internal const val COL_ID = "id"
        internal const val COL_NOTE_ID = "note_id"
        internal const val COL_VERSION = "version"
        internal const val COL_ACTION = "action"
        internal const val COL_ACTOR_ID = "actor_id"
        internal const val COL_REMOTE_IP = "remote_ip"
        internal const val COL_SUMMARY = "summary"
        internal const val COL_CREATED_AT = "created_at"
    }
}
