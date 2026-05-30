/*
 * NoteController.kt
 *
 * $Since: 2026-05-30T00:00:00Z
 */
package com.fj.omnimemo.api.endpoint.note

import com.fj.omnimemo.api.endpoint.ApiPathsV1
import com.fj.omnimemo.api.endpoint.note.dto.request.CreateNoteRequest
import com.fj.omnimemo.api.endpoint.note.dto.request.UpdateNoteRequest
import com.fj.omnimemo.api.endpoint.note.dto.response.NoteListItemResponse
import com.fj.omnimemo.api.endpoint.note.dto.response.NoteResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.parameters.RequestBody
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.servlet.http.HttpServletRequest
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.*

/**
 * REST API contract for wiki note management.
 *
 * GET endpoints are accessible anonymously; access control for restricted and
 * private notes is enforced by the domain use cases. Write operations
 * (POST, PUT, DELETE) require an authenticated session.
 *
 * Implementations live in the `impl` sub-package.
 *
 * @author Francesco Jo
 * @since 0.2.0
 * @version 0.2.0
 */
@RequestMapping(produces = [MediaType.APPLICATION_JSON_VALUE])
@Tag(name = "Note", description = "Wiki note management")
interface NoteController {

    @Operation(
        summary = "List notes by language",
        description = "Returns notes grouped by title index (e.g. A-Z for English, ㄱ-ㅎ for Korean). " +
                "Each map entry is a list of [NoteListItemResponse] objects.",
        responses = [
            ApiResponse(
                responseCode = "200", description = "Notes grouped by title index",
                content = [Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = Schema(ref = "#/components/schemas/v1.note.NoteListItemResponse"),
                )],
            ),
            ApiResponse(
                responseCode = "400", description = "Unknown language code",
                content = [Content(schema = Schema(ref = "#/components/schemas/common.ErrorResponseEnvelope"))],
            ),
        ],
    )
    @GetMapping(ApiPathsV1.NOTES)
    fun list(@RequestParam language: String): Map<String, List<NoteListItemResponse>>

    @Operation(
        summary = "Find note by ID",
        responses = [
            ApiResponse(
                responseCode = "200", description = "Note found",
                content = [Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = Schema(ref = "#/components/schemas/v1.note.NoteResponse"),
                )],
            ),
            ApiResponse(
                responseCode = "400", description = "Malformed note ID",
                content = [Content(schema = Schema(ref = "#/components/schemas/common.ErrorResponseEnvelope"))],
            ),
            ApiResponse(
                responseCode = "403", description = "Access denied",
                content = [Content(schema = Schema(ref = "#/components/schemas/common.ErrorResponseEnvelope"))],
            ),
            ApiResponse(
                responseCode = "404", description = "Note not found",
                content = [Content(schema = Schema(ref = "#/components/schemas/common.ErrorResponseEnvelope"))],
            ),
        ],
    )
    @GetMapping("${ApiPathsV1.NOTES}/{id}")
    fun findById(@PathVariable id: String): NoteResponse

    @Operation(
        summary = "Create a new note",
        requestBody = RequestBody(
            content = [Content(
                mediaType = MediaType.APPLICATION_JSON_VALUE,
                schema = Schema(ref = "#/components/schemas/v1.note.CreateNoteRequest"),
            )],
        ),
        responses = [
            ApiResponse(
                responseCode = "201", description = "Note created",
                content = [Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = Schema(ref = "#/components/schemas/v1.note.NoteResponse"),
                )],
            ),
            ApiResponse(
                responseCode = "400", description = "Invalid field value",
                content = [Content(schema = Schema(ref = "#/components/schemas/common.ErrorResponseEnvelope"))],
            ),
            ApiResponse(
                responseCode = "409", description = "Duplicate title",
                content = [Content(schema = Schema(ref = "#/components/schemas/common.ErrorResponseEnvelope"))],
            ),
        ],
    )
    @PostMapping(ApiPathsV1.NOTES, consumes = [MediaType.APPLICATION_JSON_VALUE])
    @ResponseStatus(HttpStatus.CREATED)
    fun create(
        @org.springframework.web.bind.annotation.RequestBody request: CreateNoteRequest,
        httpRequest: HttpServletRequest,
    ): NoteResponse

    @Operation(
        summary = "Update a note",
        requestBody = RequestBody(
            content = [Content(
                mediaType = MediaType.APPLICATION_JSON_VALUE,
                schema = Schema(ref = "#/components/schemas/v1.note.UpdateNoteRequest"),
            )],
        ),
        responses = [
            ApiResponse(
                responseCode = "200", description = "Note updated",
                content = [Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = Schema(ref = "#/components/schemas/v1.note.NoteResponse"),
                )],
            ),
            ApiResponse(
                responseCode = "400", description = "Malformed note ID or invalid field value",
                content = [Content(schema = Schema(ref = "#/components/schemas/common.ErrorResponseEnvelope"))],
            ),
            ApiResponse(
                responseCode = "403", description = "Access denied",
                content = [Content(schema = Schema(ref = "#/components/schemas/common.ErrorResponseEnvelope"))],
            ),
            ApiResponse(
                responseCode = "404", description = "Note not found",
                content = [Content(schema = Schema(ref = "#/components/schemas/common.ErrorResponseEnvelope"))],
            ),
            ApiResponse(
                responseCode = "409", description = "Stale version or duplicate title",
                content = [Content(schema = Schema(ref = "#/components/schemas/common.ErrorResponseEnvelope"))],
            ),
        ],
    )
    @PutMapping("${ApiPathsV1.NOTES}/{id}", consumes = [MediaType.APPLICATION_JSON_VALUE])
    fun update(
        @PathVariable id: String,
        @org.springframework.web.bind.annotation.RequestBody request: UpdateNoteRequest,
        httpRequest: HttpServletRequest,
    ): NoteResponse

    @Operation(
        summary = "Soft-delete a note",
        responses = [
            ApiResponse(responseCode = "204", description = "Note deleted"),
            ApiResponse(
                responseCode = "400", description = "Malformed note ID",
                content = [Content(schema = Schema(ref = "#/components/schemas/common.ErrorResponseEnvelope"))],
            ),
            ApiResponse(
                responseCode = "403", description = "Access denied",
                content = [Content(schema = Schema(ref = "#/components/schemas/common.ErrorResponseEnvelope"))],
            ),
            ApiResponse(
                responseCode = "404", description = "Note not found",
                content = [Content(schema = Schema(ref = "#/components/schemas/common.ErrorResponseEnvelope"))],
            ),
        ],
    )
    @DeleteMapping("${ApiPathsV1.NOTES}/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun delete(@PathVariable id: String, httpRequest: HttpServletRequest)
}
