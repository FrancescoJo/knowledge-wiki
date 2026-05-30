/*
 * DuplicateNoteTitleException.kt
 *
 * $Since: 2026-05-30T00:00:00Z
 */
package com.fj.omnimemo.core.note.exception

import com.fj.omnimemo.core.exception.OmniMemoErrorCode
import com.fj.omnimemo.core.exception.OmniMemoExternalException

/**
 * Thrown when a create or rename operation would produce a title that is already
 * in use by another note.
 *
 * @author Francesco Jo
 * @since 0.2.0
 * @version 0.2.0
 */
class DuplicateNoteTitleException(title: String) :
    OmniMemoExternalException("Note title already exists: $title") {
    override val errorCode = OmniMemoErrorCode.DUPLICATE_NOTE_TITLE
}
