/*
 * MockNoteVersionRepository.kt
 *
 * $Since: 2026-05-30T00:00:00Z
 */
package com.fj.omnimemo.core.note.repository

import com.fj.omnimemo.core.note.model.NoteId
import com.fj.omnimemo.core.note.model.NoteVersion
import java.time.Instant

/**
 * In-memory fake of [NoteVersionRepository] for use in Small tests.
 *
 * All versions are stored as full-content snapshots; delta/snapshot logic is
 * an infrastructure concern and is not exercised here.
 *
 * @author Francesco Jo
 * @since 0.2.0
 * @version 0.2.0
 */
class MockNoteVersionRepository : NoteVersionRepository {

    private val store = mutableMapOf<Pair<NoteId, Int>, String>()

    override fun saveContent(noteId: NoteId, version: Int, fullContent: String) {
        store[noteId to version] = fullContent
    }

    override fun findContent(noteId: NoteId, version: Int): String? = store[noteId to version]

    override fun findAllByNoteId(noteId: NoteId): List<NoteVersion> =
        store.entries
            .filter { it.key.first == noteId }
            .map { (key, content) ->
                NoteVersion.reconstitute(key.first, key.second, content, true, Instant.EPOCH)
            }
            .sortedBy { it.version }

    fun clear() = store.clear()
}
