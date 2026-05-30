/*
 * NoteAlreadyDeletedException.kt
 *
 * $Since: 2026-05-30T00:00:00Z
 */
package com.fj.omnimemo.core.note.exception

import com.fj.omnimemo.core.exception.OmniMemoErrorCode
import com.fj.omnimemo.core.exception.OmniMemoExternalException
import com.fj.omnimemo.core.note.model.NoteId

/**
 * Thrown when an operation is attempted on a note that has already been
 * soft-deleted.
 *
 * @author Francesco Jo
 * @since 0.2.0
 * @version 0.2.0
 */
class NoteAlreadyDeletedException(noteId: NoteId) :
    OmniMemoExternalException("Note is already deleted: ${noteId.value}") {
    override val errorCode = OmniMemoErrorCode.NOTE_ALREADY_DELETED
}
