/*
 * NoteExtensions.kt
 *
 * $Since: 2026-05-30T00:00:00Z
 */
package com.fj.omnimemo.core.note

import com.fj.omnimemo.core.note.model.Note
import com.fj.omnimemo.core.note.model.snapshot.NoteMutator

/**
 * Returns a [Note.Mutator] pre-populated from this instance.
 *
 * Assigning [Note.Mutator.title] recomputes [Note.Mutator.titleIndex] and
 * advances [Note.Mutator.updatedAt]. Assigning [Note.Mutator.accessLevel],
 * [Note.Mutator.status], or [Note.Mutator.currentVersion] also advances
 * [Note.Mutator.updatedAt].
 *
 * @since 0.2.0
 */
fun Note.mutate(): Note.Mutator = NoteMutator(
    id = id,
    isNew = isNew,
    language = language,
    title = title,
    accessLevel = accessLevel,
    status = status,
    currentVersion = currentVersion,
    authorId = authorId,
    softDeletedBy = softDeletedBy,
    softDeletedAt = softDeletedAt,
    createdAt = createdAt,
    updatedAt = updatedAt,
)
