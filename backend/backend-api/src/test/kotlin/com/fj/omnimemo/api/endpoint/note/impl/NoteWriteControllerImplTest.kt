/*
 * NoteWriteControllerImplTest.kt
 *
 * $Since: 2026-05-30T00:00:00Z
 */
package com.fj.omnimemo.api.endpoint.note.impl

import com.fj.omnimemo.api.endpoint.note.dto.request.CreateNoteRequest
import com.fj.omnimemo.api.endpoint.note.dto.request.UpdateNoteRequest
import com.fj.omnimemo.core.note.exception.NoteNotFoundException
import com.fj.omnimemo.core.note.model.Note
import com.fj.omnimemo.core.note.model.NoteId
import com.fj.omnimemo.core.note.repository.MockNoteAuditRepository
import com.fj.omnimemo.core.note.repository.MockNoteRepository
import com.fj.omnimemo.core.note.repository.MockNoteVersionRepository
import com.fj.omnimemo.core.note.usecase.CreateNoteUseCase
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
class NoteWriteControllerImplTest {

    private val noteRepo = MockNoteRepository()
    private val noteVersionRepo = MockNoteVersionRepository()
    private val noteAuditRepo = MockNoteAuditRepository()

    private val writeController = NoteWriteControllerImpl(
        createNoteUseCase = CreateNoteUseCase(noteRepo, noteVersionRepo, noteAuditRepo),
        updateNoteUseCase = UpdateNoteUseCase(noteRepo, noteVersionRepo, noteAuditRepo),
        softDeleteNoteUseCase = SoftDeleteNoteUseCase(noteRepo, noteAuditRepo),
    )

    private val authorId = UserId(UUID.randomUUID())
    private lateinit var existingNote: Note
    private val httpRequest = MockHttpServletRequest().apply { remoteAddr = "127.0.0.1" }

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
        existingNote = writeController.create(
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
    inner class Create {

        @Test
        fun `should create and return NoteResponse`() {
            val response = writeController.create(
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

            shouldThrow<ResponseStatusException> {
                writeController.create(
                    CreateNoteRequest("en", "Another Note", "content", "PUBLIC", "PUBLISHED", null),
                    httpRequest,
                )
            }.statusCode shouldBe HttpStatus.UNAUTHORIZED
        }

        @Test
        fun `should throw 400 for unknown language`() {
            shouldThrow<ResponseStatusException> {
                writeController.create(
                    CreateNoteRequest("zz", "Bad Lang Note", "content", "PUBLIC", "PUBLISHED", null),
                    httpRequest,
                )
            }.statusCode shouldBe HttpStatus.BAD_REQUEST
        }

        @Test
        fun `should throw 400 for unknown access level`() {
            shouldThrow<ResponseStatusException> {
                writeController.create(
                    CreateNoteRequest("en", "Bad AccessLevel Note", "content", "UNKNOWN", "PUBLISHED", null),
                    httpRequest,
                )
            }.statusCode shouldBe HttpStatus.BAD_REQUEST
        }

        @Test
        fun `should throw 400 for unknown status`() {
            shouldThrow<ResponseStatusException> {
                writeController.create(
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
            val response = writeController.update(
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

            shouldThrow<ResponseStatusException> {
                writeController.update(
                    existingNote.id.value.toString(),
                    UpdateNoteRequest(existingNote.currentVersion, null, "content", null, null, null),
                    httpRequest,
                )
            }.statusCode shouldBe HttpStatus.UNAUTHORIZED
        }

        @Test
        fun `should throw 400 for malformed note id`() {
            shouldThrow<ResponseStatusException> {
                writeController.update(
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
            writeController.delete(existingNote.id.value.toString(), httpRequest)

            val deleted = noteRepo.findById(existingNote.id)
            deleted?.isSoftDeleted shouldBe true
        }

        @Test
        fun `should throw 401 when unauthenticated`() {
            SecurityContextHolder.clearContext()

            shouldThrow<ResponseStatusException> {
                writeController.delete(existingNote.id.value.toString(), httpRequest)
            }.statusCode shouldBe HttpStatus.UNAUTHORIZED
        }

        @Test
        fun `should throw 400 for malformed note id`() {
            shouldThrow<ResponseStatusException> {
                writeController.delete("not-a-uuid", httpRequest)
            }.statusCode shouldBe HttpStatus.BAD_REQUEST
        }

        @Test
        fun `should throw NoteNotFoundException for unknown note id`() {
            shouldThrow<NoteNotFoundException> {
                writeController.delete(UUID.randomUUID().toString(), httpRequest)
            }
        }
    }
}
