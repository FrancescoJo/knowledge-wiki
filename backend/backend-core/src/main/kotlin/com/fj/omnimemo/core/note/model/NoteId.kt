/*
 * NoteId.kt
 *
 * $Since: 2026-05-30T00:00:00Z
 */
package com.fj.omnimemo.core.note.model

import com.github.f4b6a3.uuid.UuidCreator
import java.util.*

/**
 * Typed identity for a [Note].
 *
 * Wraps a raw [UUID] to prevent accidental mixing of entity identifiers at
 * call sites. UUID v7 generation is centralised here so no other type needs
 * to import [UuidCreator] directly.
 *
 * @author Francesco Jo
 * @since 0.2.0
 * @version 0.2.0
 */
@JvmInline
value class NoteId(val value: UUID) {
    companion object {
        /** Generates a new, time-ordered UUID v7 identity. */
        fun generate(): NoteId = NoteId(UuidCreator.getTimeOrderedEpoch())
    }
}
