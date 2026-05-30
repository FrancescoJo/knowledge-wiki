/*
 * UpdateNoteRequest.kt
 *
 * $Since: 2026-05-30T00:00:00Z
 */
package com.fj.omnimemo.api.endpoint.note.dto.request

/**
 * Request body for PUT [com.fj.omnimemo.api.endpoint.ApiPathsV1.NOTES]/{id}.
 *
 * [expectedVersion] is the [com.fj.omnimemo.core.note.model.Note.currentVersion] the client
 * last observed. If the note has since been updated by another request, the server returns
 * HTTP 409 (Stale version). A null [title] leaves the existing title unchanged.
 *
 * @author Francesco Jo
 * @since 0.2.0
 * @version 0.2.0
 */
data class UpdateNoteRequest(
    val expectedVersion: Int,
    val title: String?,
    val content: String,
    val accessLevel: String?,
    val status: String?,
    val summary: String?,
)
