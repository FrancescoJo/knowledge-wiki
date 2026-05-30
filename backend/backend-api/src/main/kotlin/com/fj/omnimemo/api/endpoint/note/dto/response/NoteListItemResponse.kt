/*
 * NoteListItemResponse.kt
 *
 * $Since: 2026-05-30T00:00:00Z
 */
package com.fj.omnimemo.api.endpoint.note.dto.response

import com.fj.omnimemo.core.note.model.Note

/**
 * Lightweight API response for a [Note] in a directory listing.
 * Content is excluded; use the single-note endpoint to fetch it.
 *
 * @author Francesco Jo
 * @since 0.2.0
 * @version 0.2.0
 */
data class NoteListItemResponse(
    val id: String,
    val title: String,
    val titleIndex: String,
    val accessLevel: String,
    val status: String,
    val authorId: String,
    val createdAt: String,
    val updatedAt: String,
)

internal fun Note.toListItemResponse() = NoteListItemResponse(
    id = id.value.toString(),
    title = title,
    titleIndex = titleIndex,
    accessLevel = accessLevel.name,
    status = status.name,
    authorId = authorId.value.toString(),
    createdAt = createdAt.toString(),
    updatedAt = updatedAt.toString(),
)
