/*
 * ListNotesUseCase.kt
 *
 * $Since: 2026-05-30T00:00:00Z
 */
package com.fj.omnimemo.core.note.usecase

import com.fj.omnimemo.core.note.model.Note
import com.fj.omnimemo.core.note.model.NoteAccessLevel
import com.fj.omnimemo.core.note.model.NoteLanguage
import com.fj.omnimemo.core.note.model.NoteStatus
import com.fj.omnimemo.core.note.repository.NoteRepository
import com.fj.omnimemo.core.user.model.UserId

/**
 * Returns the directory listing of accessible, published notes for a language,
 * grouped by title index (초성 for Korean, A-Z for English).
 *
 * Only non-deleted notes that are visible to the requester are included.
 * DRAFT notes are excluded unless the requester is the author.
 *
 * @author Francesco Jo
 * @since 0.2.0
 * @version 0.2.0
 */
class ListNotesUseCase(
    private val noteRepository: NoteRepository,
) {
    fun listByLanguage(language: NoteLanguage, requesterId: UserId?): Map<String, List<Note>> =
        noteRepository.findAllByLanguage(language)
            .filter { !it.isSoftDeleted }
            .filter { isAccessible(it, requesterId) }
            .groupBy { it.titleIndex }

    private fun isAccessible(note: Note, requesterId: UserId?): Boolean {
        if (note.status == NoteStatus.DRAFT && requesterId != note.authorId) return false
        return when (note.accessLevel) {
            NoteAccessLevel.PUBLIC -> true
            NoteAccessLevel.RESTRICTED -> requesterId != null
            NoteAccessLevel.PRIVATE -> requesterId == note.authorId
        }
    }
}
