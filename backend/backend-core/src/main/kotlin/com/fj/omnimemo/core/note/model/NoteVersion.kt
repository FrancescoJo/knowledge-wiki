/*
 * NoteVersion.kt
 *
 * $Since: 2026-05-30T00:00:00Z
 */
package com.fj.omnimemo.core.note.model

import com.fj.omnimemo.core.note.model.NoteVersion.Companion.create
import com.fj.omnimemo.core.note.model.NoteVersion.Companion.reconstitute
import com.fj.omnimemo.core.note.model.snapshot.NoteVersionData
import java.time.Instant

/**
 * An immutable version record for a [Note].
 *
 * [content] holds either the full Markdown text ([isSnapshot] = true) or a
 * unified diff patch ([isSnapshot] = false). The infrastructure layer is
 * responsible for deciding which form to use and for reconstructing the full
 * content when [isSnapshot] is false.
 *
 * Versions are append-only: once created they are never modified.
 *
 * Use [create] when saving a new version and [reconstitute] when loading from
 * the data store.
 *
 * @author Francesco Jo
 * @since 0.2.0
 * @version 0.2.0
 */
interface NoteVersion {
    val noteId: NoteId
    val version: Int
    val content: String
    val isSnapshot: Boolean
    val createdAt: Instant

    companion object {
        fun create(
            noteId: NoteId,
            version: Int,
            content: String,
            isSnapshot: Boolean,
        ): NoteVersion = NoteVersionData(
            noteId = noteId,
            version = version,
            content = content,
            isSnapshot = isSnapshot,
            createdAt = Instant.now(),
        )

        fun reconstitute(
            noteId: NoteId,
            version: Int,
            content: String,
            isSnapshot: Boolean,
            createdAt: Instant,
        ): NoteVersion = NoteVersionData(
            noteId = noteId,
            version = version,
            content = content,
            isSnapshot = isSnapshot,
            createdAt = createdAt,
        )
    }
}
