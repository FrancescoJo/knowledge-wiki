/*
 * StaleNoteVersionException.kt
 *
 * $Since: 2026-05-30T00:00:00Z
 */
package com.fj.omnimemo.core.note.exception

import com.fj.omnimemo.core.exception.OmniMemoErrorCode
import com.fj.omnimemo.core.exception.OmniMemoExternalException
import com.fj.omnimemo.core.note.model.NoteId

/**
 * Thrown when an update is submitted against an outdated version of a note
 * (optimistic locking failure).
 *
 * The caller should reload the latest version, re-apply their changes, and
 * resubmit.
 *
 * @author Francesco Jo
 * @since 0.2.0
 * @version 0.2.0
 */
class StaleNoteVersionException(
    noteId: NoteId,
    expected: Int,
    actual: Int,
) : OmniMemoExternalException(
    "Stale version for note ${noteId.value}: expected $expected but was $actual"
) {
    override val errorCode = OmniMemoErrorCode.STALE_NOTE_VERSION
}
