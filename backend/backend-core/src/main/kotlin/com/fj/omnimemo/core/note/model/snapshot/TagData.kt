/*
 * TagData.kt
 *
 * $Since: 2026-05-30T00:00:00Z
 */
package com.fj.omnimemo.core.note.model.snapshot

import com.fj.omnimemo.core.note.model.Tag
import com.fj.omnimemo.core.note.model.TagId
import java.time.Instant

/**
 * Immutable snapshot of a [Tag].
 *
 * @author Francesco Jo
 * @since 0.2.0
 * @version 0.2.0
 */
internal data class TagData(
    override val id: TagId,
    override val name: String,
    override val createdAt: Instant,
) : Tag
