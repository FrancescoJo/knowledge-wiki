/*
 * NoteWriteControllerImpl.kt
 *
 * $Since: 2026-05-30T00:00:00Z
 */
package com.fj.omnimemo.api.endpoint.note.impl

import com.fj.omnimemo.api.endpoint.note.NoteWriteController
import com.fj.omnimemo.api.endpoint.note.dto.request.CreateNoteRequest
import com.fj.omnimemo.api.endpoint.note.dto.request.UpdateNoteRequest
import com.fj.omnimemo.api.endpoint.note.dto.response.NoteResponse
import com.fj.omnimemo.api.endpoint.note.dto.response.toResponse
import com.fj.omnimemo.core.note.usecase.CreateNoteUseCase
import com.fj.omnimemo.core.note.usecase.SoftDeleteNoteUseCase
import com.fj.omnimemo.core.note.usecase.UpdateNoteUseCase
import jakarta.servlet.http.HttpServletRequest
import org.springframework.web.bind.annotation.RestController

/**
 * @author Francesco Jo
 * @since 0.2.0
 * @version 0.2.0
 */
@RestController
internal class NoteWriteControllerImpl(
    private val createNoteUseCase: CreateNoteUseCase,
    private val updateNoteUseCase: UpdateNoteUseCase,
    private val softDeleteNoteUseCase: SoftDeleteNoteUseCase,
) : NoteWriteController {
    override fun create(request: CreateNoteRequest, httpRequest: HttpServletRequest): NoteResponse {
        val authorId = requireAuthenticated()
        val note = createNoteUseCase.create(
            authorId = authorId,
            language = parseLanguage(request.language),
            title = request.title,
            content = request.content,
            accessLevel = parseAccessLevel(request.accessLevel),
            status = parseStatus(request.status),
            remoteIp = httpRequest.remoteAddr,
            summary = request.summary,
        )
        return note.toResponse(request.content)
    }

    override fun update(id: String, request: UpdateNoteRequest, httpRequest: HttpServletRequest): NoteResponse {
        val requesterId = requireAuthenticated()
        val note = updateNoteUseCase.update(
            noteId = parseNoteId(id),
            editorId = requesterId,
            expectedVersion = request.expectedVersion,
            title = request.title,
            content = request.content,
            accessLevel = request.accessLevel?.let { parseAccessLevel(it) },
            status = request.status?.let { parseStatus(it) },
            remoteIp = httpRequest.remoteAddr,
            summary = request.summary,
        )
        return note.toResponse(request.content)
    }

    override fun delete(id: String, httpRequest: HttpServletRequest) {
        val requesterId = requireAuthenticated()
        softDeleteNoteUseCase.softDelete(parseNoteId(id), requesterId, httpRequest.remoteAddr)
    }
}
