/*
 * CreateNoteUseCase.kt
 *
 * $Since: 2026-05-30T00:00:00Z
 */
package com.fj.omnimemo.core.note.usecase

import com.fj.omnimemo.core.annotation.NamedArguments
import com.fj.omnimemo.core.note.exception.DuplicateNoteTitleException
import com.fj.omnimemo.core.note.model.*
import com.fj.omnimemo.core.note.mutate
import com.fj.omnimemo.core.note.repository.NoteAuditRepository
import com.fj.omnimemo.core.note.repository.NoteRepository
import com.fj.omnimemo.core.note.repository.NoteVersionRepository
import com.fj.omnimemo.core.user.model.UserId

/**
 * Creates a new note with its first version.
 *
 * Sequence (must run in a single transaction):
 * 1. Guard against duplicate title.
 * 2. INSERT the note row with [currentVersion] = 0.
 * 3. INSERT version 1 content (infrastructure decides snapshot vs delta).
 * 4. UPDATE note to [currentVersion] = 1.
 * 5. INSERT audit entry.
 *
 * @author Francesco Jo
 * @since 0.2.0
 * @version 0.2.0
 */
class CreateNoteUseCase(
    private val noteRepository: NoteRepository,
    private val noteVersionRepository: NoteVersionRepository,
    private val noteAuditRepository: NoteAuditRepository,
) {
    @NamedArguments
    fun create(
        authorId: UserId,
        language: NoteLanguage,
        title: String,
        content: String,
        accessLevel: NoteAccessLevel,
        status: NoteStatus,
        remoteIp: String,
        summary: String?,
    ): Note {
        if (noteRepository.existsByTitle(title)) throw DuplicateNoteTitleException(title)

        var note = noteRepository.save(Note.create(authorId, language, title, accessLevel, status))

        val firstVersion = 1
        noteVersionRepository.saveContent(note.id, firstVersion, content)

        val mutator = note.mutate()
        mutator.currentVersion = firstVersion
        note = noteRepository.save(mutator)

        noteAuditRepository.save(
            NoteAudit.create(note.id, firstVersion, NoteAction.CREATE, authorId, remoteIp, summary)
        )

        return note
    }
}
