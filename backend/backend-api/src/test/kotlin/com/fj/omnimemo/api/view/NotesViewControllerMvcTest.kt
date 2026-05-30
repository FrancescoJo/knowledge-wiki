/*
 * NotesViewControllerMvcTest.kt
 *
 * $Since: 2026-05-30T00:00:00Z
 */
package com.fj.omnimemo.api.view

import com.fj.omnimemo.core.note.exception.NoteNotFoundException
import com.fj.omnimemo.core.note.model.NoteLanguage
import com.fj.omnimemo.core.note.usecase.FindNoteUseCase
import com.fj.omnimemo.core.note.usecase.ListNotesUseCase
import com.fj.omnimemo.core.test.annotation.MediumTest
import com.fj.omnimemo.core.user.UserProfileCache
import com.fj.omnimemo.core.user.randomUser
import com.fj.omnimemo.core.note.randomNote
import com.fj.omnimemo.infrastructure.security.JwtTokenService
import com.fj.omnimemo.view.NotesViewController
import org.hamcrest.CoreMatchers.containsString
import org.junit.jupiter.api.Test
import org.mockito.BDDMockito.given
import org.mockito.Mockito
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.content
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

/**
 * Medium tests for [NotesViewController]: verifies the notes directory and single-note
 * pages render with correct HTML, breadcrumbs, and status codes.
 *
 * Security filters are excluded; authentication logic is covered separately.
 *
 * [NoteNotFoundException] triggers a 404 response via [ResponseStatusException].
 * The directory listing uses [anyArg] because [NoteLanguage] is a plain enum — no
 * value-class NPE concern applies here.
 *
 * @author Francesco Jo
 * @since 0.2.0
 * @version 0.2.0
 */
@MediumTest
@WebMvcTest(controllers = [NotesViewController::class])
@AutoConfigureMockMvc(addFilters = false)
class NotesViewControllerMvcTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @MockBean
    private lateinit var findNoteUseCase: FindNoteUseCase

    @MockBean
    private lateinit var listNotesUseCase: ListNotesUseCase

    @Suppress("UnusedPrivateProperty")
    @MockBean
    private lateinit var userProfileCache: UserProfileCache

    @Suppress("UnusedPrivateProperty")
    @MockBean
    private lateinit var jwtTokenService: JwtTokenService

    @Test
    fun `GET notes directory returns 200 with HTML content`() {
        given(listNotesUseCase.listByLanguage(anyArg(), anyArg())).willReturn(emptyMap())

        mockMvc.perform(get("/notes"))
            .andExpect(status().isOk)
            .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_HTML))
    }

    @Test
    fun `GET notes directory includes breadcrumb with Home and Contents links`() {
        given(listNotesUseCase.listByLanguage(anyArg(), anyArg())).willReturn(emptyMap())

        mockMvc.perform(get("/notes"))
            .andExpect(status().isOk)
            .andExpect(content().string(containsString("href=\"/\"")))
            .andExpect(content().string(containsString("href=\"/contents\"")))
    }

    @Test
    fun `GET notes by title returns 200 with rendered note content`() {
        val author = randomUser()
        val note = randomNote(authorId = author.id, title = "hello-world")
        val result = FindNoteUseCase.Result(note, "# Hello")
        given(listNotesUseCase.listByLanguage(anyArg(), anyArg())).willReturn(emptyMap())
        given(findNoteUseCase.findByTitle("hello-world", null)).willReturn(result)

        mockMvc.perform(get("/notes/hello-world"))
            .andExpect(status().isOk)
            .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_HTML))
            .andExpect(content().string(containsString("hello-world")))
    }

    @Test
    fun `GET notes by title returns 404 when note does not exist`() {
        given(listNotesUseCase.listByLanguage(anyArg(), anyArg())).willReturn(emptyMap())
        given(findNoteUseCase.findByTitle("unknown-note", null))
            .willThrow(NoteNotFoundException("unknown-note"))

        mockMvc.perform(get("/notes/unknown-note"))
            .andExpect(status().isNotFound)
    }

    companion object {
        @Suppress("UNCHECKED_CAST")
        private fun <T> anyArg(): T = Mockito.any<Any>() as T
    }
}
