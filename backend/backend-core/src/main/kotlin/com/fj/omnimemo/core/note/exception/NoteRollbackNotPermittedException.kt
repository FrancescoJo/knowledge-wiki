/*
 * NoteRollbackNotPermittedException.kt
 *
 * $Since: 2026-05-30T00:00:00Z
 */
package com.fj.omnimemo.core.note.exception

import com.fj.omnimemo.core.exception.OmniMemoErrorCode
import com.fj.omnimemo.core.exception.OmniMemoExternalException
import com.fj.omnimemo.core.note.model.NoteId

/**
 * Thrown when a rollback is requested by a user who is not the original author
 * of the note. Only the author may roll back to a previous version.
 *
 * @author Francesco Jo
 * @since 0.2.0
 * @version 0.2.0
 */
class NoteRollbackNotPermittedException(noteId: NoteId) :
    OmniMemoExternalException("Rollback not permitted for note: ${noteId.value}") {
    override val errorCode = OmniMemoErrorCode.NOTE_ROLLBACK_NOT_PERMITTED
}
