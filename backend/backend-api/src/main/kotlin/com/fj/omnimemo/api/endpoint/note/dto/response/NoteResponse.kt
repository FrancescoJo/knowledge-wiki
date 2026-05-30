/*
 * NoteResponse.kt
 *
 * $Since: 2026-05-30T00:00:00Z
 */
package com.fj.omnimemo.api.endpoint.note.dto.response

import com.fj.omnimemo.core.note.model.Note

/**
 * API response representing a single [Note] with its current content.
 *
 * @author Francesco Jo
 * @since 0.2.0
 * @version 0.2.0
 */
data class NoteResponse(
    val id: String,
    val language: String,
    val title: String,
    val titleIndex: String,
    val accessLevel: String,
    val status: String,
    val currentVersion: Int,
    val authorId: String,
    val content: String,
    val softDeleted: Boolean,
    val createdAt: String,
    val updatedAt: String,
)

internal fun Note.toResponse(content: String) = NoteResponse(
    id = id.value.toString(),
    language = language.code,
    title = title,
    titleIndex = titleIndex,
    accessLevel = accessLevel.name,
    status = status.name,
    currentVersion = currentVersion,
    authorId = authorId.value.toString(),
    content = content,
    softDeleted = isSoftDeleted,
    createdAt = createdAt.toString(),
    updatedAt = updatedAt.toString(),
)
