/*
 * NoteVersionRepository.kt
 *
 * $Since: 2026-05-30T00:00:00Z
 */
package com.fj.omnimemo.core.note.repository

import com.fj.omnimemo.core.note.model.NoteId
import com.fj.omnimemo.core.note.model.NoteVersion

/**
 * Persistence contract for [NoteVersion] records.
 *
 * [saveContent] accepts the full Markdown content; the implementation decides
 * whether to store it as a snapshot or a delta patch according to the version
 * number (snapshot at version 1 and every tenth version thereafter).
 *
 * [findContent] reconstructs the full Markdown text for the given version by
 * applying any delta patches on top of the nearest preceding snapshot.
 *
 * @author Francesco Jo
 * @since 0.2.0
 * @version 0.2.0
 */
interface NoteVersionRepository {
    fun saveContent(noteId: NoteId, version: Int, fullContent: String)
    fun findContent(noteId: NoteId, version: Int): String?
    fun findAllByNoteId(noteId: NoteId): List<NoteVersion>
}
