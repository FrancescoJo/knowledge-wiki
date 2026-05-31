/*
 * SoftDeleteNoteUseCase.kt
 *
 * $Since: 2026-05-30T00:00:00Z
 */
package com.fj.omnimemo.core.note.usecase

import com.fj.omnimemo.core.note.exception.NoteAccessDeniedException
import com.fj.omnimemo.core.note.exception.NoteAlreadyDeletedException
import com.fj.omnimemo.core.note.exception.NoteNotFoundException
import com.fj.omnimemo.core.note.model.Note
import com.fj.omnimemo.core.note.model.NoteAction
import com.fj.omnimemo.core.note.model.NoteAudit
import com.fj.omnimemo.core.note.model.NoteId
import com.fj.omnimemo.core.note.mutate
import com.fj.omnimemo.core.note.repository.NoteAuditRepository
import com.fj.omnimemo.core.note.repository.NoteRepository
import com.fj.omnimemo.core.user.model.UserId
import java.time.Instant

/**
 * Soft-deletes a note. The note remains in the data store but is hidden from
 * directory listings and regular reads.
 *
 * Only the author may delete a note in v0.2.
 *
 * @author Francesco Jo
 * @since 0.2.0
 * @version 0.2.0
 */
class SoftDeleteNoteUseCase(
    private val noteRepository: NoteRepository,
    private val noteAuditRepository: NoteAuditRepository,
) {
    fun softDelete(noteId: NoteId, deleterId: UserId, remoteIp: String): Note {
        val note = noteRepository.findById(noteId) ?: throw NoteNotFoundException(noteId)

        if (note.isSoftDeleted) throw NoteAlreadyDeletedException(noteId)
        if (deleterId != note.authorId) throw NoteAccessDeniedException(noteId)

        val now = Instant.now()
        val mutator = note.mutate()
        mutator.softDeletedBy = deleterId
        mutator.softDeletedAt = now

        val deleted = noteRepository.save(mutator)

        noteAuditRepository.save(
            NoteAudit.create(note.id, note.currentVersion, NoteAction.DELETE, deleterId, remoteIp, null)
        )

        return deleted
    }
}
