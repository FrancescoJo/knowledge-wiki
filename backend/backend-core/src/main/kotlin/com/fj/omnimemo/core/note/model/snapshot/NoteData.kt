/*
 * NoteData.kt
 *
 * $Since: 2026-05-30T00:00:00Z
 */
package com.fj.omnimemo.core.note.model.snapshot

import com.fj.omnimemo.core.note.model.*
import com.fj.omnimemo.core.user.model.UserId
import java.time.Instant

/**
 * Immutable snapshot of a [Note]. Used for all read paths.
 *
 * @author Francesco Jo
 * @since 0.2.0
 * @version 0.2.0
 */
internal data class NoteData(
    override val id: NoteId,
    override val isNew: Boolean,
    override val language: NoteLanguage,
    override val title: String,
    override val titleIndex: String,
    override val accessLevel: NoteAccessLevel,
    override val status: NoteStatus,
    override val currentVersion: Int,
    override val authorId: UserId,
    override val softDeletedBy: UserId?,
    override val softDeletedAt: Instant?,
    override val createdAt: Instant,
    override val updatedAt: Instant,
) : Note
