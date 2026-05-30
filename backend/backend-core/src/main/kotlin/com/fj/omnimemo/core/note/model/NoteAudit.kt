/*
 * NoteAudit.kt
 *
 * $Since: 2026-05-30T00:00:00Z
 */
package com.fj.omnimemo.core.note.model

import com.fj.omnimemo.core.annotation.NamedArguments
import com.fj.omnimemo.core.note.model.NoteAudit.Companion.create
import com.fj.omnimemo.core.note.model.NoteAudit.Companion.reconstitute
import com.fj.omnimemo.core.note.model.snapshot.NoteAuditData
import com.fj.omnimemo.core.user.model.UserId
import com.github.f4b6a3.uuid.UuidCreator
import java.time.Instant
import java.util.*

/**
 * An immutable audit entry recording a single action taken on a [Note].
 *
 * One audit entry is created per version-changing operation (create, edit,
 * rollback, delete, restore). Audit entries are append-only.
 *
 * Use [create] when recording a new action and [reconstitute] when loading
 * from the data store.
 *
 * @author Francesco Jo
 * @since 0.2.0
 * @version 0.2.0
 */
interface NoteAudit {
    val id: UUID
    val noteId: NoteId
    val version: Int
    val action: NoteAction
    val actorId: UserId
    val remoteIp: String
    val summary: String?
    val createdAt: Instant

    companion object {
        @NamedArguments
        fun create(
            noteId: NoteId,
            version: Int,
            action: NoteAction,
            actorId: UserId,
            remoteIp: String,
            summary: String?,
        ): NoteAudit = NoteAuditData(
            id = UuidCreator.getTimeOrderedEpoch(),
            noteId = noteId,
            version = version,
            action = action,
            actorId = actorId,
            remoteIp = remoteIp,
            summary = summary,
            createdAt = Instant.now(),
        )

        @NamedArguments
        fun reconstitute(
            id: UUID,
            noteId: NoteId,
            version: Int,
            action: NoteAction,
            actorId: UserId,
            remoteIp: String,
            summary: String?,
            createdAt: Instant,
        ): NoteAudit = NoteAuditData(
            id = id,
            noteId = noteId,
            version = version,
            action = action,
            actorId = actorId,
            remoteIp = remoteIp,
            summary = summary,
            createdAt = createdAt,
        )
    }
}
