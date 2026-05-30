/*
 * FindNoteUseCase.kt
 *
 * $Since: 2026-05-30T00:00:00Z
 */
package com.fj.omnimemo.core.note.usecase

import com.fj.omnimemo.core.note.exception.NoteAccessDeniedException
import com.fj.omnimemo.core.note.exception.NoteNotFoundException
import com.fj.omnimemo.core.note.model.Note
import com.fj.omnimemo.core.note.model.NoteAccessLevel
import com.fj.omnimemo.core.note.model.NoteId
import com.fj.omnimemo.core.note.model.NoteStatus
import com.fj.omnimemo.core.note.repository.NoteRepository
import com.fj.omnimemo.core.note.repository.NoteVersionRepository
import com.fj.omnimemo.core.user.model.UserId

/**
 * Retrieves a note by title or identity, enforcing access-level rules.
 *
 * Soft-deleted notes are treated as non-existent for unauthorised callers.
 *
 * Access rules:
 * - PUBLIC notes are visible to anyone.
 * - RESTRICTED notes require an authenticated caller ([requesterId] not null).
 * - PRIVATE notes are visible only to the author ([requesterId] == [Note.authorId]).
 * - DRAFT notes are visible only to the author regardless of access level.
 *
 * @author Francesco Jo
 * @since 0.2.0
 * @version 0.2.0
 */
class FindNoteUseCase(
    private val noteRepository: NoteRepository,
    private val noteVersionRepository: NoteVersionRepository,
) {
    data class Result(val note: Note, val content: String)

    fun findByTitle(title: String, requesterId: UserId?): Result {
        val note = noteRepository.findByTitle(title) ?: throw NoteNotFoundException(title)
        return buildResult(note, requesterId)
    }

    fun findById(id: NoteId, requesterId: UserId?): Result {
        val note = noteRepository.findById(id) ?: throw NoteNotFoundException(id)
        return buildResult(note, requesterId)
    }

    private fun buildResult(note: Note, requesterId: UserId?): Result {
        checkAccess(note, requesterId)
        val content = noteVersionRepository.findContent(note.id, note.currentVersion) ?: ""
        return Result(note, content)
    }

    private fun checkAccess(note: Note, requesterId: UserId?) {
        if (note.isSoftDeleted && requesterId != note.authorId) {
            throw NoteNotFoundException(note.id)
        }
        if (note.status == NoteStatus.DRAFT && requesterId != note.authorId) {
            throw NoteAccessDeniedException(note.id)
        }
        when (note.accessLevel) {
            NoteAccessLevel.PUBLIC -> Unit
            NoteAccessLevel.RESTRICTED -> {
                if (requesterId == null) throw NoteAccessDeniedException(note.id)
            }
            NoteAccessLevel.PRIVATE -> {
                if (requesterId != note.authorId) throw NoteAccessDeniedException(note.id)
            }
        }
    }
}
