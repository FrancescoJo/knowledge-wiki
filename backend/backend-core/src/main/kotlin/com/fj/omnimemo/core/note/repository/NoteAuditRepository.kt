/*
 * NoteAuditRepository.kt
 *
 * $Since: 2026-05-30T00:00:00Z
 */
package com.fj.omnimemo.core.note.repository

import com.fj.omnimemo.core.note.model.NoteAudit
import com.fj.omnimemo.core.note.model.NoteId

/**
 * Persistence contract for [NoteAudit] records.
 *
 * Audit entries are append-only and are never updated or deleted.
 *
 * @author Francesco Jo
 * @since 0.2.0
 * @version 0.2.0
 */
interface NoteAuditRepository {
    fun save(audit: NoteAudit)
    fun findAllByNoteId(noteId: NoteId): List<NoteAudit>
}
