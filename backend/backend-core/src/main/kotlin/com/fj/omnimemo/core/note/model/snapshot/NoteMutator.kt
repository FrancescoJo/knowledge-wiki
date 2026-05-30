/*
 * NoteMutator.kt
 *
 * $Since: 2026-05-30T00:00:00Z
 */
package com.fj.omnimemo.core.note.model.snapshot

import com.fj.omnimemo.core.note.model.Note
import com.fj.omnimemo.core.note.model.NoteAccessLevel
import com.fj.omnimemo.core.note.model.NoteId
import com.fj.omnimemo.core.note.model.NoteLanguage
import com.fj.omnimemo.core.note.model.NoteStatus
import com.fj.omnimemo.core.user.model.UserId
import java.time.Instant

// Not a data class: custom property setters are not compatible with the
// data class model. equals / hashCode / toString delegate to NoteData snapshot.
/**
 * Mutable implementation of [Note]. Obtain via [mutate]; discard after use.
 *
 * Assigning [title] recomputes [titleIndex] and advances [updatedAt].
 * Assigning [accessLevel], [status], or [currentVersion] also advances [updatedAt].
 * [softDeletedBy] and [softDeletedAt] are set explicitly without side effects.
 *
 * @author Francesco Jo
 * @since 0.2.0
 * @version 0.2.0
 */
internal class NoteMutator(
    override val id: NoteId,
    override val isNew: Boolean,
    override val language: NoteLanguage,
    title: String,
    accessLevel: NoteAccessLevel,
    status: NoteStatus,
    currentVersion: Int,
    override val authorId: UserId,
    softDeletedBy: UserId?,
    softDeletedAt: Instant?,
    override val createdAt: Instant,
    override var updatedAt: Instant,
) : Note.Mutator {

    override var title: String = title
        set(value) {
            field = value
            updatedAt = Instant.now()
        }

    override val titleIndex: String
        get() = language.extractTitleIndex(title)

    override var accessLevel: NoteAccessLevel = accessLevel
        set(value) {
            field = value
            updatedAt = Instant.now()
        }

    override var status: NoteStatus = status
        set(value) {
            field = value
            updatedAt = Instant.now()
        }

    override var currentVersion: Int = currentVersion
        set(value) {
            field = value
            updatedAt = Instant.now()
        }

    override var softDeletedBy: UserId? = softDeletedBy
    override var softDeletedAt: Instant? = softDeletedAt

    private fun snapshot() = NoteData(
        id, isNew, language, title, titleIndex, accessLevel, status,
        currentVersion, authorId, softDeletedBy, softDeletedAt, createdAt, updatedAt,
    )

    override fun equals(other: Any?) = other is NoteMutator && snapshot() == other.snapshot()
    override fun hashCode() = snapshot().hashCode()
    override fun toString() = snapshot().toString()
}
