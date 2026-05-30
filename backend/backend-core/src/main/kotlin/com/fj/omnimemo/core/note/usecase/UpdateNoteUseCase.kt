/*
 * UpdateNoteUseCase.kt
 *
 * $Since: 2026-05-30T00:00:00Z
 */
package com.fj.omnimemo.core.note.usecase

import com.fj.omnimemo.core.annotation.NamedArguments
import com.fj.omnimemo.core.note.exception.DuplicateNoteTitleException
import com.fj.omnimemo.core.note.exception.NoteAccessDeniedException
import com.fj.omnimemo.core.note.exception.NoteAlreadyDeletedException
import com.fj.omnimemo.core.note.exception.NoteNotFoundException
import com.fj.omnimemo.core.note.exception.StaleNoteVersionException
import com.fj.omnimemo.core.note.model.Note
import com.fj.omnimemo.core.note.model.NoteAccessLevel
import com.fj.omnimemo.core.note.model.NoteAction
import com.fj.omnimemo.core.note.model.NoteAudit
import com.fj.omnimemo.core.note.model.NoteId
import com.fj.omnimemo.core.note.model.NoteStatus
import com.fj.omnimemo.core.note.repository.NoteAuditRepository
import com.fj.omnimemo.core.note.repository.NoteRepository
import com.fj.omnimemo.core.note.repository.NoteVersionRepository
import com.fj.omnimemo.core.note.mutate
import com.fj.omnimemo.core.user.model.UserId

/**
 * Updates the content and/or metadata of an existing note.
 *
 * Optimistic locking: [expectedVersion] must equal the note's [Note.currentVersion]
 * at the time of the call; a mismatch raises [StaleNoteVersionException].
 *
 * Only the author may edit a note in v0.2.
 *
 * Sequence (must run in a single transaction):
 * 1. Load and validate the note (exists, not deleted, requester is author, version matches).
 * 2. Guard against duplicate title when the title is being changed.
 * 3. INSERT the new version content.
 * 4. UPDATE the note metadata and advance [currentVersion].
 * 5. INSERT audit entry.
 *
 * @author Francesco Jo
 * @since 0.2.0
 * @version 0.2.0
 */
class UpdateNoteUseCase(
    private val noteRepository: NoteRepository,
    private val noteVersionRepository: NoteVersionRepository,
    private val noteAuditRepository: NoteAuditRepository,
) {
    @NamedArguments
    fun update(
        noteId: NoteId,
        editorId: UserId,
        expectedVersion: Int,
        title: String?,
        content: String,
        accessLevel: NoteAccessLevel?,
        status: NoteStatus?,
        remoteIp: String,
        summary: String?,
    ): Note {
        val note = noteRepository.findById(noteId) ?: throw NoteNotFoundException(noteId)

        if (note.isSoftDeleted) throw NoteAlreadyDeletedException(noteId)
        if (editorId != note.authorId) throw NoteAccessDeniedException(noteId)
        if (note.currentVersion != expectedVersion) {
            throw StaleNoteVersionException(noteId, expectedVersion, note.currentVersion)
        }
        if (title != null && title != note.title && noteRepository.existsByTitle(title)) {
            throw DuplicateNoteTitleException(title)
        }

        val newVersion = note.currentVersion + 1
        noteVersionRepository.saveContent(note.id, newVersion, content)

        val mutator = note.mutate()
        if (title != null) mutator.title = title
        if (accessLevel != null) mutator.accessLevel = accessLevel
        if (status != null) mutator.status = status
        mutator.currentVersion = newVersion

        val updated = noteRepository.save(mutator)

        noteAuditRepository.save(
            NoteAudit.create(note.id, newVersion, NoteAction.EDIT, editorId, remoteIp, summary)
        )

        return updated
    }
}
