/*
 * NoteControllerImpl.kt
 *
 * $Since: 2026-05-30T00:00:00Z
 */
package com.fj.omnimemo.api.endpoint.note.impl

import com.fj.omnimemo.api.endpoint.note.NoteController
import com.fj.omnimemo.api.endpoint.note.dto.request.CreateNoteRequest
import com.fj.omnimemo.api.endpoint.note.dto.request.UpdateNoteRequest
import com.fj.omnimemo.api.endpoint.note.dto.response.NoteListItemResponse
import com.fj.omnimemo.api.endpoint.note.dto.response.NoteResponse
import com.fj.omnimemo.api.endpoint.note.dto.response.toListItemResponse
import com.fj.omnimemo.api.endpoint.note.dto.response.toResponse
import com.fj.omnimemo.core.note.model.NoteAccessLevel
import com.fj.omnimemo.core.note.model.NoteId
import com.fj.omnimemo.core.note.model.NoteLanguage
import com.fj.omnimemo.core.note.model.NoteStatus
import com.fj.omnimemo.core.note.usecase.CreateNoteUseCase
import com.fj.omnimemo.core.note.usecase.FindNoteUseCase
import com.fj.omnimemo.core.note.usecase.ListNotesUseCase
import com.fj.omnimemo.core.note.usecase.SoftDeleteNoteUseCase
import com.fj.omnimemo.core.note.usecase.UpdateNoteUseCase
import com.fj.omnimemo.core.user.model.UserId
import com.fj.omnimemo.core.util.parseUuidOrNull
import jakarta.servlet.http.HttpServletRequest
import org.springframework.http.HttpStatus
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ResponseStatusException

/**
 * @author Francesco Jo
 * @since 0.2.0
 * @version 0.2.0
 */
@RestController
internal class NoteControllerImpl(
    private val findNoteUseCase: FindNoteUseCase,
    private val listNotesUseCase: ListNotesUseCase,
    private val createNoteUseCase: CreateNoteUseCase,
    private val updateNoteUseCase: UpdateNoteUseCase,
    private val softDeleteNoteUseCase: SoftDeleteNoteUseCase,
) : NoteController {

    override fun list(language: String): Map<String, List<NoteListItemResponse>> {
        val lang = parseLanguage(language)
        val requesterId = resolveRequesterId()
        return listNotesUseCase.listByLanguage(lang, requesterId)
            .mapValues { (_, notes) -> notes.map { it.toListItemResponse() } }
    }

    override fun findById(id: String): NoteResponse {
        val requesterId = resolveRequesterId()
        val result = findNoteUseCase.findById(parseNoteId(id), requesterId)
        return result.note.toResponse(result.content)
    }

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

    private fun resolveRequesterId(): UserId? {
        val auth = SecurityContextHolder.getContext().authentication ?: return null
        if (!auth.isAuthenticated || auth.principal == "anonymousUser") return null
        return parseUuidOrNull(auth.principal as? String ?: return null)?.let { UserId(it) }
    }

    private fun requireAuthenticated(): UserId =
        resolveRequesterId() ?: throw ResponseStatusException(HttpStatus.UNAUTHORIZED)

    private fun parseNoteId(raw: String): NoteId =
        parseUuidOrNull(raw)?.let { NoteId(it) }
            ?: throw ResponseStatusException(HttpStatus.BAD_REQUEST)

    private fun parseLanguage(code: String): NoteLanguage =
        try {
            NoteLanguage.fromCode(code)
        } catch (_: IllegalArgumentException) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Unknown language code: $code")
        }

    private fun parseAccessLevel(raw: String): NoteAccessLevel =
        try {
            NoteAccessLevel.valueOf(raw)
        } catch (_: IllegalArgumentException) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Unknown access level: $raw")
        }

    private fun parseStatus(raw: String): NoteStatus =
        try {
            NoteStatus.valueOf(raw)
        } catch (_: IllegalArgumentException) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Unknown status: $raw")
        }
}
