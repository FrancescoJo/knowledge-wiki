/*
 * Note.kt
 *
 * $Since: 2026-05-30T00:00:00Z
 */
package com.fj.omnimemo.core.note.model

import com.fj.omnimemo.core.data.model.DateTimeAuditable
import com.fj.omnimemo.core.data.model.Persistable
import com.fj.omnimemo.core.note.model.Note.Companion.create
import com.fj.omnimemo.core.note.model.Note.Companion.reconstitute
import com.fj.omnimemo.core.note.model.snapshot.NoteData
import com.fj.omnimemo.core.user.model.UserId
import java.time.Instant

/**
 * A wiki note — the central content entity of the system.
 *
 * [title] is unique across all notes and serves as the URL path segment.
 * [titleIndex] is derived from [title] and [language]; it drives directory
 * navigation grouping (초성 for Korean, A-Z for English).
 *
 * [currentVersion] is used for optimistic locking on updates. It starts at 0
 * on creation and advances to 1 when the first [NoteVersion] is saved.
 *
 * Use [create] to instantiate a new note and [reconstitute] to restore one
 * from a persisted record.
 *
 * @author Francesco Jo
 * @since 0.2.0
 * @version 0.2.0
 */
interface Note : Persistable<NoteId>, DateTimeAuditable {
    override val id: NoteId
    override val isNew: Boolean
    val language: NoteLanguage
    val title: String
    val titleIndex: String
    val accessLevel: NoteAccessLevel
    val status: NoteStatus
    val currentVersion: Int
    val authorId: UserId
    val softDeletedBy: UserId?
    val softDeletedAt: Instant?

    val isSoftDeleted: Boolean get() = softDeletedAt != null

    /**
     * Grants write access to mutable business fields. Obtain via [mutate].
     *
     * Assigning [title] automatically recomputes [titleIndex] and advances
     * [updatedAt]. Assigning [accessLevel], [status], or [currentVersion]
     * advances [updatedAt]. [softDeletedBy] and [softDeletedAt] do not
     * advance [updatedAt] — the deletion timestamp is tracked separately.
     *
     * @since 0.2.0
     * @version 0.2.0
     */
    interface Mutator : Note, DateTimeAuditable.Mutator {
        override var title: String
        override var accessLevel: NoteAccessLevel
        override var status: NoteStatus
        override var currentVersion: Int
        override var softDeletedBy: UserId?
        override var softDeletedAt: Instant?
    }

    companion object {
        /**
         * Creates a new, unpersisted [Note]. A UUID v7 identity is generated
         * by the domain; [currentVersion] starts at 0 (no content yet).
         *
         * @since 0.2.0
         */
        fun create(
            authorId: UserId,
            language: NoteLanguage,
            title: String,
            accessLevel: NoteAccessLevel,
            status: NoteStatus,
        ): Note {
            val now = Instant.now()
            return NoteData(
                id = NoteId.generate(),
                isNew = true,
                language = language,
                title = title,
                titleIndex = language.extractTitleIndex(title),
                accessLevel = accessLevel,
                status = status,
                currentVersion = 0,
                authorId = authorId,
                softDeletedBy = null,
                softDeletedAt = null,
                createdAt = now,
                updatedAt = now,
            )
        }

        /**
         * Restores a [Note] from a persisted record. [isNew] is false.
         *
         * @since 0.2.0
         */
        fun reconstitute(
            id: NoteId,
            language: NoteLanguage,
            title: String,
            titleIndex: String,
            accessLevel: NoteAccessLevel,
            status: NoteStatus,
            currentVersion: Int,
            authorId: UserId,
            softDeletedBy: UserId?,
            softDeletedAt: Instant?,
            createdAt: Instant,
            updatedAt: Instant,
        ): Note = NoteData(
            id = id,
            isNew = false,
            language = language,
            title = title,
            titleIndex = titleIndex,
            accessLevel = accessLevel,
            status = status,
            currentVersion = currentVersion,
            authorId = authorId,
            softDeletedBy = softDeletedBy,
            softDeletedAt = softDeletedAt,
            createdAt = createdAt,
            updatedAt = updatedAt,
        )
    }
}
