/*
 * NoteControllerImplTest.kt
 *
 * $Since: 2026-05-30T00:00:00Z
 */
package com.fj.omnimemo.api.endpoint.note.impl

import com.fj.omnimemo.api.endpoint.note.dto.request.CreateNoteRequest
import com.fj.omnimemo.api.endpoint.note.dto.request.UpdateNoteRequest
import com.fj.omnimemo.core.note.exception.NoteAccessDeniedException
import com.fj.omnimemo.core.note.exception.NoteNotFoundException
import com.fj.omnimemo.core.note.model.Note
import com.fj.omnimemo.core.note.model.NoteAccessLevel
import com.fj.omnimemo.core.note.model.NoteId
import com.fj.omnimemo.core.note.model.NoteLanguage
import com.fj.omnimemo.core.note.model.NoteStatus
import com.fj.omnimemo.core.note.repository.MockNoteAuditRepository
import com.fj.omnimemo.core.note.repository.MockNoteRepository
import com.fj.omnimemo.core.note.repository.MockNoteVersionRepository
import com.fj.omnimemo.core.note.usecase.CreateNoteUseCase
import com.fj.omnimemo.core.note.usecase.FindNoteUseCase
import com.fj.omnimemo.core.note.usecase.ListNotesUseCase
import com.fj.omnimemo.core.note.usecase.SoftDeleteNoteUseCase
import com.fj.omnimemo.core.note.usecase.UpdateNoteUseCase
import com.fj.omnimemo.core.test.annotation.SmallTest
import com.fj.omnimemo.core.user.model.UserId
import io.kotest.assertions.assertSoftly
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

    private val controller = NoteControllerImpl(
        findNoteUseCase = FindNoteUseCase(noteRepo, noteVersionRepo),
        listNotesUseCase = ListNotesUseCase(noteRepo),
        createNoteUseCase = CreateNoteUseCase(noteRepo, noteVersionRepo, noteAuditRepo),
        updateNoteUseCase = UpdateNoteUseCase(noteRepo, noteVersionRepo, noteAuditRepo),
        softDeleteNoteUseCase = SoftDeleteNoteUseCase(noteRepo, noteAuditRepo),
    )

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
        existingNote = controller.create(
            CreateNoteRequest(
                language = "en",
                title = "Getting Started",
                content = "# Hello",
                accessLevel = "PUBLIC",
                status = "PUBLISHED",
                summary = null,
            ),
            httpRequest,
        ).let { noteRepo.findById(NoteId(UUID.fromString(it.id)))!! }
    }

    @AfterEach
    fun tearDown() {
        SecurityContextHolder.clearContext()
    }

    @Nested
    inner class List {

        @Test
        fun `should return notes grouped by title index`() {
            val result = controller.list("en")

            result shouldNotBe null
            result["G"]?.any { it.title == "Getting Started" } shouldBe true
        }

        @Test
        fun `should return empty map when no notes exist for language`() {
            val result = controller.list("ko")

            result shouldBe emptyMap()
        }

        @Test
        fun `should throw 400 for unknown language code`() {
            shouldThrow<ResponseStatusException> {
                controller.list("xx")
            }.statusCode shouldBe HttpStatus.BAD_REQUEST
        }
    }

    @Nested
    inner class FindById {

        @Test
        fun `should return NoteResponse for an existing note`() {
            val response = controller.findById(existingNote.id.value.toString())

            assertSoftly {
                response.id shouldBe existingNote.id.value.toString()
                response.title shouldBe "Getting Started"
                response.language shouldBe "en"
                response.accessLevel shouldBe "PUBLIC"
                response.status shouldBe "PUBLISHED"
                response.authorId shouldBe authorId.value.toString()
                response.content shouldBe "# Hello"
            }
        }

        @Test
        fun `should throw NoteNotFoundException for unknown note id`() {
            shouldThrow<NoteNotFoundException> {
                controller.findById(UUID.randomUUID().toString())
            }
        }

        @Test
        fun `should throw 400 for malformed note id`() {
            shouldThrow<ResponseStatusException> {
                controller.findById("not-a-uuid")
            }.statusCode shouldBe HttpStatus.BAD_REQUEST
        }

        @Test
        fun `should throw NoteAccessDeniedException for private note accessed anonymously`() {
            val httpRequest = MockHttpServletRequest().apply { remoteAddr = "127.0.0.1" }
            val privateNote = controller.create(
                CreateNoteRequest("en", "Private Note", "secret", "PRIVATE", "PUBLISHED", null),
                httpRequest,
            ).let { noteRepo.findById(NoteId(UUID.fromString(it.id)))!! }

            SecurityContextHolder.clearContext()

            shouldThrow<NoteAccessDeniedException> {
                controller.findById(privateNote.id.value.toString())
            }
        }
    }

    @Nested
    inner class Create {

        @Test
        fun `should create and return NoteResponse`() {
            val httpRequest = MockHttpServletRequest().apply { remoteAddr = "127.0.0.1" }

            val response = controller.create(
                CreateNoteRequest("en", "New Note", "content here", "PUBLIC", "PUBLISHED", "init"),
                httpRequest,
            )

            assertSoftly {
                response.id shouldNotBe null
                response.title shouldBe "New Note"
                response.language shouldBe "en"
                response.currentVersion shouldBe 1
                response.content shouldBe "content here"
            }
        }

        @Test
        fun `should throw 401 when unauthenticated`() {
            SecurityContextHolder.clearContext()
            val httpRequest = MockHttpServletRequest().apply { remoteAddr = "127.0.0.1" }

            shouldThrow<ResponseStatusException> {
                controller.create(
                    CreateNoteRequest("en", "Another Note", "content", "PUBLIC", "PUBLISHED", null),
                    httpRequest,
                )
            }.statusCode shouldBe HttpStatus.UNAUTHORIZED
        }

        @Test
        fun `should throw 400 for unknown language`() {
            val httpRequest = MockHttpServletRequest().apply { remoteAddr = "127.0.0.1" }

            shouldThrow<ResponseStatusException> {
                controller.create(
                    CreateNoteRequest("zz", "Bad Lang Note", "content", "PUBLIC", "PUBLISHED", null),
                    httpRequest,
                )
            }.statusCode shouldBe HttpStatus.BAD_REQUEST
        }

        @Test
        fun `should throw 400 for unknown access level`() {
            val httpRequest = MockHttpServletRequest().apply { remoteAddr = "127.0.0.1" }

            shouldThrow<ResponseStatusException> {
                controller.create(
                    CreateNoteRequest("en", "Bad AccessLevel Note", "content", "UNKNOWN", "PUBLISHED", null),
                    httpRequest,
                )
            }.statusCode shouldBe HttpStatus.BAD_REQUEST
        }

        @Test
        fun `should throw 400 for unknown status`() {
            val httpRequest = MockHttpServletRequest().apply { remoteAddr = "127.0.0.1" }

            shouldThrow<ResponseStatusException> {
                controller.create(
                    CreateNoteRequest("en", "Bad Status Note", "content", "PUBLIC", "UNKNOWN", null),
                    httpRequest,
                )
            }.statusCode shouldBe HttpStatus.BAD_REQUEST
        }
    }

    @Nested
    inner class Update {

        @Test
        fun `should update and return NoteResponse`() {
            val httpRequest = MockHttpServletRequest().apply { remoteAddr = "127.0.0.1" }

            val response = controller.update(
                existingNote.id.value.toString(),
                UpdateNoteRequest(
                    expectedVersion = existingNote.currentVersion,
                    title = "Updated Title",
                    content = "updated content",
                    accessLevel = null,
                    status = null,
                    summary = "fix typo",
                ),
                httpRequest,
            )

            assertSoftly {
                response.title shouldBe "Updated Title"
                response.content shouldBe "updated content"
            }
        }

        @Test
        fun `should throw 401 when unauthenticated`() {
            SecurityContextHolder.clearContext()
            val httpRequest = MockHttpServletRequest().apply { remoteAddr = "127.0.0.1" }

            shouldThrow<ResponseStatusException> {
                controller.update(
                    existingNote.id.value.toString(),
                    UpdateNoteRequest(existingNote.currentVersion, null, "content", null, null, null),
                    httpRequest,
                )
            }.statusCode shouldBe HttpStatus.UNAUTHORIZED
        }

        @Test
        fun `should throw 400 for malformed note id`() {
            val httpRequest = MockHttpServletRequest().apply { remoteAddr = "127.0.0.1" }

            shouldThrow<ResponseStatusException> {
                controller.update(
                    "not-a-uuid",
                    UpdateNoteRequest(1, null, "content", null, null, null),
                    httpRequest,
                )
            }.statusCode shouldBe HttpStatus.BAD_REQUEST
        }
    }

    @Nested
    inner class Delete {

        @Test
        fun `should soft-delete a note without throwing`() {
            val httpRequest = MockHttpServletRequest().apply { remoteAddr = "127.0.0.1" }

            controller.delete(existingNote.id.value.toString(), httpRequest)

            val deleted = noteRepo.findById(existingNote.id)
            deleted?.isSoftDeleted shouldBe true
        }

        @Test
        fun `should throw 401 when unauthenticated`() {
            SecurityContextHolder.clearContext()
            val httpRequest = MockHttpServletRequest().apply { remoteAddr = "127.0.0.1" }

            shouldThrow<ResponseStatusException> {
                controller.delete(existingNote.id.value.toString(), httpRequest)
            }.statusCode shouldBe HttpStatus.UNAUTHORIZED
        }

        @Test
        fun `should throw 400 for malformed note id`() {
            val httpRequest = MockHttpServletRequest().apply { remoteAddr = "127.0.0.1" }

            shouldThrow<ResponseStatusException> {
                controller.delete("not-a-uuid", httpRequest)
            }.statusCode shouldBe HttpStatus.BAD_REQUEST
        }

        @Test
        fun `should throw NoteNotFoundException for unknown note id`() {
            val httpRequest = MockHttpServletRequest().apply { remoteAddr = "127.0.0.1" }

            shouldThrow<NoteNotFoundException> {
                controller.delete(UUID.randomUUID().toString(), httpRequest)
            }
        }
    }
}
