/*
 * CreateNoteRequest.kt
 *
 * $Since: 2026-05-30T00:00:00Z
 */
package com.fj.omnimemo.api.endpoint.note.dto.request

/**
 * Request body for POST [com.fj.omnimemo.api.endpoint.ApiPathsV1.NOTES].
 *
 * [language] must be a code recognised by [com.fj.omnimemo.core.note.model.NoteLanguage]
 * (e.g. "en", "ko"). [accessLevel] must match a [com.fj.omnimemo.core.note.model.NoteAccessLevel]
 * name. [status] must match a [com.fj.omnimemo.core.note.model.NoteStatus] name.
 *
 * @author Francesco Jo
 * @since 0.2.0
 * @version 0.2.0
 */
data class CreateNoteRequest(
    val language: String,
    val title: String,
    val content: String,
    val accessLevel: String,
    val status: String,
    val summary: String?,
)
