/*
 * NoteControllerImpl.kt
 *
 * $Since: 2026-05-30T00:00:00Z
 */
package com.fj.omnimemo.api.endpoint.note.impl

import com.fj.omnimemo.api.endpoint.note.NoteController
import com.fj.omnimemo.api.endpoint.note.dto.response.NoteListItemResponse
import com.fj.omnimemo.api.endpoint.note.dto.response.NoteResponse
import com.fj.omnimemo.api.endpoint.note.dto.response.toListItemResponse
import com.fj.omnimemo.api.endpoint.note.dto.response.toResponse
import com.fj.omnimemo.core.note.usecase.FindNoteUseCase
import com.fj.omnimemo.core.note.usecase.ListNotesUseCase
import org.springframework.web.bind.annotation.RestController

/**
 * @author Francesco Jo
 * @since 0.2.0
 * @version 0.2.0
 */
@RestController
internal class NoteControllerImpl(
    private val findNoteUseCase: FindNoteUseCase,
    private val listNotesUseCase: ListNotesUseCase,
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
}
