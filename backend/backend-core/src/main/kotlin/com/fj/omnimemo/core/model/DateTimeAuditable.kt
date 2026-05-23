/*
 * DateTimeAuditable.kt
 *
 * $Since: 2026-05-23T00:00:00Z
 */
package com.fj.omnimemo.core.model

import java.time.Instant

/**
 * Captures the creation and last-update instants of a domain object.
 *
 * [createdAt] is immutable once set; a record's birth time never changes.
 * Only [updatedAt] is exposed for mutation through [Mutator].
 *
 * @author Francesco Jo
 * @since 0.1.1
 * @version 0.1.1
 */
interface DateTimeAuditable {
    val createdAt: Instant
    val updatedAt: Instant

    /**
     * Grants write access to [updatedAt]. [createdAt] remains immutable.
     *
     * @since 0.1.1
     * @version 0.1.1
     */
    interface Mutator : DateTimeAuditable {
        override var updatedAt: Instant
    }
}
