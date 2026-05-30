/*
 * NoteControllerSupport.kt
 *
 * $Since: 2026-05-30T00:00:00Z
 */
package com.fj.omnimemo.api.endpoint.note.impl

import com.fj.omnimemo.core.note.model.NoteAccessLevel
import com.fj.omnimemo.core.note.model.NoteId
import com.fj.omnimemo.core.note.model.NoteLanguage
import com.fj.omnimemo.core.note.model.NoteStatus
import com.fj.omnimemo.core.user.model.UserId
import com.fj.omnimemo.core.util.parseUuidOrNull
import org.springframework.http.HttpStatus
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.server.ResponseStatusException

internal fun resolveRequesterId(): UserId? {
    val auth = SecurityContextHolder.getContext().authentication ?: return null
    if (!auth.isAuthenticated || auth.principal == "anonymousUser") return null
    return parseUuidOrNull(auth.principal as? String ?: return null)?.let { UserId(it) }
}

internal fun requireAuthenticated(): UserId =
    resolveRequesterId() ?: throw ResponseStatusException(HttpStatus.UNAUTHORIZED)

internal fun parseNoteId(raw: String): NoteId =
    parseUuidOrNull(raw)?.let { NoteId(it) }
        ?: throw ResponseStatusException(HttpStatus.BAD_REQUEST)

internal fun parseLanguage(code: String): NoteLanguage =
    try {
        NoteLanguage.fromCode(code)
    } catch (_: IllegalArgumentException) {
        throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Unknown language code: $code")
    }

internal fun parseAccessLevel(raw: String): NoteAccessLevel =
    try {
        NoteAccessLevel.valueOf(raw)
    } catch (_: IllegalArgumentException) {
        throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Unknown access level: $raw")
    }

internal fun parseStatus(raw: String): NoteStatus =
    try {
        NoteStatus.valueOf(raw)
    } catch (_: IllegalArgumentException) {
        throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Unknown status: $raw")
    }
