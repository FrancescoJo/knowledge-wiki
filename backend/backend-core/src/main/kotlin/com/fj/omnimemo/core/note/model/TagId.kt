/*
 * TagId.kt
 *
 * $Since: 2026-05-30T00:00:00Z
 */
package com.fj.omnimemo.core.note.model

import com.github.f4b6a3.uuid.UuidCreator
import java.util.*

/**
 * Typed identity for a [Tag].
 *
 * @author Francesco Jo
 * @since 0.2.0
 * @version 0.2.0
 */
@JvmInline
value class TagId(val value: UUID) {
    companion object {
        fun generate(): TagId = TagId(UuidCreator.getTimeOrderedEpoch())
    }
}
