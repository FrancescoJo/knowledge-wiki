/*
 * NoteVersionData.kt
 *
 * $Since: 2026-05-30T00:00:00Z
 */
package com.fj.omnimemo.core.note.model.snapshot

import com.fj.omnimemo.core.note.model.NoteId
import com.fj.omnimemo.core.note.model.NoteVersion
import java.time.Instant

/**
 * Immutable snapshot of a [NoteVersion].
 *
 * @author Francesco Jo
 * @since 0.2.0
 * @version 0.2.0
 */
internal data class NoteVersionData(
    override val noteId: NoteId,
    override val version: Int,
    override val content: String,
    override val isSnapshot: Boolean,
    override val createdAt: Instant,
) : NoteVersion
