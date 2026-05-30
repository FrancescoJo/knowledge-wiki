/*
 * NotesViewController.kt
 *
 * $Since: 2026-05-30T00:00:00Z
 */
package com.fj.omnimemo.view

import com.fj.omnimemo.core.note.exception.NoteAccessDeniedException
import com.fj.omnimemo.core.note.exception.NoteNotFoundException
import com.fj.omnimemo.core.note.model.NoteLanguage
import com.fj.omnimemo.core.note.usecase.FindNoteUseCase
import com.fj.omnimemo.core.note.usecase.ListNotesUseCase
import jakarta.servlet.http.HttpServletRequest
import org.springframework.context.MessageSource
import java.net.URLDecoder
import java.nio.charset.StandardCharsets
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.server.ResponseStatusException
import java.util.Locale
import java.util.TreeMap

/**
 * View endpoints for the notes directory and individual note pages.
 *
 * GET /notes renders the directory listing for the current locale's language.
 * GET /notes/{title} renders a single note identified by its full title path.
 *
 * The language is derived from the request locale; unknown locales fall back to English.
 * The title for single-note requests is extracted from the servlet path after the `/notes/` prefix.
 *
 * @author Francesco Jo
 * @since 0.2.0
 */
@Controller
class NotesViewController(
    private val findNoteUseCase: FindNoteUseCase,
    private val listNotesUseCase: ListNotesUseCase,
    private val messageSource: MessageSource,
) {

    @GetMapping("/notes")
    fun directory(locale: Locale, model: Model): String {
        val language = resolveLanguage(locale)
        val requesterId = resolveCurrentUserId()
        model.addAttribute("language", language.code)
        model.addAttribute("directory", TreeMap(listNotesUseCase.listByLanguage(language, requesterId)))
        model.addAttribute(
            "breadcrumbs", listOf(
                BreadcrumbItem(messageSource.getMessage("nav.home", null, locale), "/"),
                BreadcrumbItem(messageSource.getMessage("nav.contents", null, locale), "/contents"),
                BreadcrumbItem(messageSource.getMessage("nav.notes", null, locale)),
            )
        )
        return "notes"
    }

    @GetMapping("/notes/**")
    fun note(request: HttpServletRequest, locale: Locale, model: Model): String {
        val title = URLDecoder.decode(
            request.requestURI.removePrefix(request.contextPath).removePrefix("/notes/"),
            StandardCharsets.UTF_8,
        )
        if (title.isBlank()) return directory(locale, model)

        val language = resolveLanguage(locale)
        val requesterId = resolveCurrentUserId()
        val directory = TreeMap(listNotesUseCase.listByLanguage(language, requesterId))

        val result = try {
            findNoteUseCase.findByTitle(title, requesterId)
        } catch (_: NoteNotFoundException) {
            throw ResponseStatusException(HttpStatus.NOT_FOUND)
        } catch (_: NoteAccessDeniedException) {
            throw ResponseStatusException(HttpStatus.FORBIDDEN)
        }

        model.addAttribute("note", result.note)
        model.addAttribute("rawContent", result.content)
        model.addAttribute("canEdit", requesterId != null && requesterId == result.note.authorId)
        model.addAttribute("language", language.code)
        model.addAttribute("directory", directory)
        model.addAttribute(
            "breadcrumbs", listOf(
                BreadcrumbItem(messageSource.getMessage("nav.home", null, locale), "/"),
                BreadcrumbItem(messageSource.getMessage("nav.contents", null, locale), "/contents"),
                BreadcrumbItem(messageSource.getMessage("nav.notes", null, locale), "/notes"),
                BreadcrumbItem(result.note.title),
            )
        )
        return "note"
    }

    private fun resolveLanguage(locale: Locale): NoteLanguage =
        try {
            NoteLanguage.fromCode(locale.language)
        } catch (_: IllegalArgumentException) {
            NoteLanguage.EN
        }
}
