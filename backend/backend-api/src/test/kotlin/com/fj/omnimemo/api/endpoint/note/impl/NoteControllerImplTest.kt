/*
 * NoteControllerImplTest.kt
 *
 * $Since: 2026-05-30T00:00:00Z
 */
package com.fj.omnimemo.api.endpoint.note.impl

import com.fj.omnimemo.core.note.exception.NoteAccessDeniedException
import com.fj.omnimemo.core.note.exception.NoteNotFoundException
import com.fj.omnimemo.core.note.model.Note
import com.fj.omnimemo.core.note.model.NoteAccessLevel
import com.fj.omnimemo.core.note.model.NoteLanguage
import com.fj.omnimemo.core.note.model.NoteStatus
import com.fj.omnimemo.core.note.repository.MockNoteAuditRepository
import com.fj.omnimemo.core.note.repository.MockNoteRepository
import com.fj.omnimemo.core.note.repository.MockNoteVersionRepository
import com.fj.omnimemo.core.note.usecase.CreateNoteUseCase
import com.fj.omnimemo.core.note.usecase.FindNoteUseCase
import com.fj.omnimemo.core.note.usecase.ListNotesUseCase
import com.fj.omnimemo.core.test.annotation.SmallTest
import com.fj.omnimemo.core.user.model.UserId
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.server.ResponseStatusException
import java.util.*

@SmallTest
class NoteControllerImplTest {

    private val noteRepo = MockNoteRepository()
    private val noteVersionRepo = MockNoteVersionRepository()
    private val noteAuditRepo = MockNoteAuditRepository()

    private val readController = NoteControllerImpl(
        findNoteUseCase = FindNoteUseCase(noteRepo, noteVersionRepo),
        listNotesUseCase = ListNotesUseCase(noteRepo),
    )
    private val createNoteUseCase = CreateNoteUseCase(noteRepo, noteVersionRepo, noteAuditRepo)

    private val authorId = UserId(UUID.randomUUID())
    private lateinit var existingNote: Note

    private fun authenticateAs(userId: UserId) {
        val auth = UsernamePasswordAuthenticationToken(userId.value.toString(), null, emptyList())
        SecurityContextHolder.getContext().authentication = auth
    }

    @BeforeEach
    fun setUp() {
        noteRepo.clear()
        noteVersionRepo.clear()
        noteAuditRepo.clear()

        authenticateAs(authorId)
        val httpRequest = MockHttpServletRequest().apply { remoteAddr = "127.0.0.1" }
        existingNote = createNoteUseCase.create(
            authorId = authorId,
            language = NoteLanguage.EN,
            title = "Getting Started",
            content = "# Hello",
            accessLevel = NoteAccessLevel.PUBLIC,
            status = NoteStatus.PUBLISHED,
            remoteIp = httpRequest.remoteAddr,
            summary = null,
        )
    }

    @AfterEach
    fun tearDown() {
        SecurityContextHolder.clearContext()
    }

    @Nested
    inner class List {

        @Test
        fun `should return notes grouped by title index`() {
            val result = readController.list("en")

            result shouldNotBe null
            result["G"]?.any { it.title == "Getting Started" } shouldBe true
        }

        @Test
        fun `should return empty map when no notes exist for language`() {
            val result = readController.list("ko")

            result shouldBe emptyMap()
        }

        @Test
        fun `should throw 400 for unknown language code`() {
            shouldThrow<ResponseStatusException> {
                readController.list("xx")
            }.statusCode shouldBe HttpStatus.BAD_REQUEST
        }
    }

    @Nested
    inner class FindById {

        @Test
        fun `should return NoteResponse for an existing note`() {
            val response = readController.findById(existingNote.id.value.toString())

            response.title shouldBe "Getting Started"
            response.language shouldBe "en"
            response.authorId shouldBe authorId.value.toString()
        }

        @Test
        fun `should throw NoteNotFoundException for unknown note id`() {
            shouldThrow<NoteNotFoundException> {
                readController.findById(UUID.randomUUID().toString())
            }
        }

        @Test
        fun `should throw 400 for malformed note id`() {
            shouldThrow<ResponseStatusException> {
                readController.findById("not-a-uuid")
            }.statusCode shouldBe HttpStatus.BAD_REQUEST
        }

        @Test
        fun `should throw NoteAccessDeniedException for private note accessed anonymously`() {
            val privateNote = createNoteUseCase.create(
                authorId = authorId,
                language = NoteLanguage.EN,
                title = "Private Note",
                content = "secret",
                accessLevel = NoteAccessLevel.PRIVATE,
                status = NoteStatus.PUBLISHED,
                remoteIp = "127.0.0.1",
                summary = null,
            )

            SecurityContextHolder.clearContext()

            shouldThrow<NoteAccessDeniedException> {
                readController.findById(privateNote.id.value.toString())
            }
        }
    }
}
