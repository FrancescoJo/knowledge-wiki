/*
 * Tag.kt
 *
 * $Since: 2026-05-30T00:00:00Z
 */
package com.fj.omnimemo.core.note.model

import com.fj.omnimemo.core.note.model.Tag.Companion.create
import com.fj.omnimemo.core.note.model.Tag.Companion.reconstitute
import com.fj.omnimemo.core.note.model.snapshot.TagData
import java.time.Instant

/**
 * A free-form label that can be attached to multiple [Note]s.
 *
 * Tags are immutable once created — [name] never changes. A tag whose name
 * does not yet exist is created on first use; orphaned tags are retained
 * (not auto-deleted when all notes detach).
 *
 * Use [create] to instantiate a new tag and [reconstitute] to restore one
 * from a persisted record.
 *
 * @author Francesco Jo
 * @since 0.2.0
 * @version 0.2.0
 */
interface Tag {
    val id: TagId
    val name: String
    val createdAt: Instant

    companion object {
        fun create(name: String): Tag = TagData(
            id = TagId.generate(),
            name = name,
            createdAt = Instant.now(),
        )

        fun reconstitute(
            id: TagId,
            name: String,
            createdAt: Instant,
        ): Tag = TagData(
            id = id,
            name = name,
            createdAt = createdAt,
        )
    }
}
