/*
 * MockNoteAuditRepository.kt
 *
 * $Since: 2026-05-30T00:00:00Z
 */
package test.com.fj.omnimemo.core.note.repository

import com.fj.omnimemo.core.note.model.NoteAudit
import com.fj.omnimemo.core.note.model.NoteId
import com.fj.omnimemo.core.note.repository.NoteAuditRepository

/**
 * In-memory fake of [NoteAuditRepository] for use in Small tests.
 *
 * @author Francesco Jo
 * @since 0.2.0
 * @version 0.2.0
 */
class MockNoteAuditRepository : NoteAuditRepository {
    private val store = mutableListOf<NoteAudit>()

    override fun save(audit: NoteAudit) {
        store.add(audit)
    }

    override fun findAllByNoteId(noteId: NoteId): List<NoteAudit> =
        store.filter { it.noteId == noteId }

    fun savedAudits(): List<NoteAudit> = store.toList()

    fun clear() = store.clear()
}
