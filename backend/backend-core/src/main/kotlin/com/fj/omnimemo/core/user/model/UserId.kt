/*
 * UserId.kt
 *
 * $Since: 2026-05-23T00:00:00Z
 */
package com.fj.omnimemo.core.user.model

import com.github.f4b6a3.uuid.UuidCreator
import java.util.*

/**
 * Typed identity for a [User].
 *
 * Wraps a raw [UUID] to prevent accidental mixing of entity identifiers at
 * call sites. UUID v7 generation is centralised here so no other type needs
 * to import [UuidCreator] directly.
 *
 * @author Francesco Jo
 * @since 0.1.1
 * @version 0.1.1
 */
@JvmInline
value class UserId(val value: UUID) {
    companion object {
        /** Generates a new, time-ordered UUID v7 identity. */
        fun generate(): UserId = UserId(UuidCreator.getTimeOrderedEpoch())
    }
}
