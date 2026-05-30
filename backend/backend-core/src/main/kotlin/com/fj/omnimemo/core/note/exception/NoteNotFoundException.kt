/*
 * NoteNotFoundException.kt
 *
 * $Since: 2026-05-30T00:00:00Z
 */
package com.fj.omnimemo.core.note.exception

import com.fj.omnimemo.core.exception.OmniMemoErrorCode
import com.fj.omnimemo.core.exception.OmniMemoExternalException
import com.fj.omnimemo.core.note.model.NoteId

/**
 * Thrown when an operation targets a [NoteId] or title that does not exist,
 * or when a soft-deleted note is requested by an unauthorised caller.
 *
 * @author Francesco Jo
 * @since 0.2.0
 * @version 0.2.0
 */
class NoteNotFoundException(identifier: Any) :
    OmniMemoExternalException("Note not found: $identifier") {
    override val errorCode = OmniMemoErrorCode.NOTE_NOT_FOUND
}
