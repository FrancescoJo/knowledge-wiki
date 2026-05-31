/*
 * TagRepository.kt
 *
 * $Since: 2026-05-30T00:00:00Z
 */
package com.fj.omnimemo.core.note.repository

import com.fj.omnimemo.core.note.model.Tag
import com.fj.omnimemo.core.note.model.TagId

/**
 * Persistence contract for [Tag] entities.
 *
 * @author Francesco Jo
 * @since 0.2.0
 * @version 0.2.0
 */
interface TagRepository {
    fun findById(id: TagId): Tag?

    fun findByName(name: String): Tag?

    fun save(tag: Tag): Tag
}
