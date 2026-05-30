/*
 * NoteAuditData.kt
 *
 * $Since: 2026-05-30T00:00:00Z
 */
package com.fj.omnimemo.core.note.model.snapshot

import com.fj.omnimemo.core.note.model.NoteAction
import com.fj.omnimemo.core.note.model.NoteAudit
import com.fj.omnimemo.core.note.model.NoteId
import com.fj.omnimemo.core.user.model.UserId
import java.time.Instant
import java.util.*

/**
 * Immutable snapshot of a [NoteAudit].
 *
 * @author Francesco Jo
 * @since 0.2.0
 * @version 0.2.0
 */
internal data class NoteAuditData(
    override val id: UUID,
    override val noteId: NoteId,
    override val version: Int,
    override val action: NoteAction,
    override val actorId: UserId,
    override val remoteIp: String,
    override val summary: String?,
    override val createdAt: Instant,
) : NoteAudit
