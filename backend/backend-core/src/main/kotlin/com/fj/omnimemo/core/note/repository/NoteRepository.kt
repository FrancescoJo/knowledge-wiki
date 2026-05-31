/*
 * NoteRepository.kt
 *
 * $Since: 2026-05-30T00:00:00Z
 */
package com.fj.omnimemo.core.note.repository

import com.fj.omnimemo.core.note.model.Note
import com.fj.omnimemo.core.note.model.NoteId
import com.fj.omnimemo.core.note.model.NoteLanguage

/**
 * Persistence contract for [Note] entities.
 *
 * @author Francesco Jo
 * @since 0.2.0
 * @version 0.2.0
 */
interface NoteRepository {
    fun findById(id: NoteId): Note?

    fun findByTitle(title: String): Note?

    fun findAllByLanguage(language: NoteLanguage): List<Note>

    fun existsByTitle(title: String): Boolean

    fun save(note: Note): Note
}
