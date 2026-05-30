/*
 * NoteAccessDeniedException.kt
 *
 * $Since: 2026-05-30T00:00:00Z
 */
package com.fj.omnimemo.core.note.exception

import com.fj.omnimemo.core.exception.OmniMemoErrorCode
import com.fj.omnimemo.core.exception.OmniMemoExternalException
import com.fj.omnimemo.core.note.model.NoteId

/**
 * Thrown when the requesting user does not have permission to read or modify
 * the target note.
 *
 * @author Francesco Jo
 * @since 0.2.0
 * @version 0.2.0
 */
class NoteAccessDeniedException(noteId: NoteId) :
    OmniMemoExternalException("Access denied for note: ${noteId.value}") {
    override val errorCode = OmniMemoErrorCode.NOTE_ACCESS_DENIED
}
