/*
 * NoteWriteControllerImplTest.kt
 *
 * $Since: 2026-05-30T00:00:00Z
 */
package testcase.small.api.endpoint.note.impl

import com.fj.omnimemo.api.endpoint.note.dto.request.CreateNoteRequest
import com.fj.omnimemo.api.endpoint.note.dto.request.UpdateNoteRequest
import com.fj.omnimemo.api.endpoint.note.impl.NoteWriteControllerImpl
import com.fj.omnimemo.core.note.exception.NoteNotFoundException
import com.fj.omnimemo.core.note.model.Note
import com.fj.omnimemo.core.note.model.NoteId
import com.fj.omnimemo.core.note.usecase.CreateNoteUseCase
import com.fj.omnimemo.core.note.usecase.SoftDeleteNoteUseCase
import com.fj.omnimemo.core.note.usecase.UpdateNoteUseCase
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
import test.com.fj.omnimemo.core.annotation.SmallTest
import test.com.fj.omnimemo.core.note.repository.MockNoteAuditRepository
import test.com.fj.omnimemo.core.note.repository.MockNoteRepository
import test.com.fj.omnimemo.core.note.repository.MockNoteVersionRepository
import java.util.*

@SmallTest
class NoteWriteControllerImplTest {
    private lateinit var noteRepo: MockNoteRepository
    private lateinit var noteVersionRepo: MockNoteVersionRepository
    private lateinit var noteAuditRepo: MockNoteAuditRepository
    private lateinit var sut: NoteWriteControllerImpl

    private val authorId = UserId(UUID.randomUUID())
    private val httpRequest = MockHttpServletRequest().apply { remoteAddr = "127.0.0.1" }
    private lateinit var existingNote: Note

    private fun authenticateAs(userId: UserId) {
        val auth = UsernamePasswordAuthenticationToken(userId.value.toString(), null, emptyList())
        SecurityContextHolder.getContext().authentication = auth
    }

    @BeforeEach
    fun setUp() {
        noteRepo = MockNoteRepository()
        noteVersionRepo = MockNoteVersionRepository()
        noteAuditRepo = MockNoteAuditRepository()
        sut = NoteWriteControllerImpl(
            createNoteUseCase = CreateNoteUseCase(noteRepo, noteVersionRepo, noteAuditRepo),
            updateNoteUseCase = UpdateNoteUseCase(noteRepo, noteVersionRepo, noteAuditRepo),
            softDeleteNoteUseCase = SoftDeleteNoteUseCase(noteRepo, noteAuditRepo),
        )

        authenticateAs(authorId)
        existingNote = sut.create(
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
        noteRepo.clear()
        noteVersionRepo.clear()
        noteAuditRepo.clear()
    }

    @Nested
    inner class Create {

        @Test
        fun `should create and return NoteResponse`() {
            val response = sut.create(
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
                sut.create(
                    CreateNoteRequest("en", "Another Note", "content", "PUBLIC", "PUBLISHED", null),
                    httpRequest,
                )
            }.statusCode shouldBe HttpStatus.UNAUTHORIZED
        }

        @Test
        fun `should throw 400 for unknown language`() {
            shouldThrow<ResponseStatusException> {
                sut.create(
                    CreateNoteRequest("zz", "Bad Lang Note", "content", "PUBLIC", "PUBLISHED", null),
                    httpRequest,
                )
            }.statusCode shouldBe HttpStatus.BAD_REQUEST
        }

        @Test
        fun `should throw 400 for unknown access level`() {
            shouldThrow<ResponseStatusException> {
                sut.create(
                    CreateNoteRequest("en", "Bad AccessLevel Note", "content", "UNKNOWN", "PUBLISHED", null),
                    httpRequest,
                )
            }.statusCode shouldBe HttpStatus.BAD_REQUEST
        }

        @Test
        fun `should throw 400 for unknown status`() {
            shouldThrow<ResponseStatusException> {
                sut.create(
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
            val response = sut.update(
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
                sut.update(
                    existingNote.id.value.toString(),
                    UpdateNoteRequest(existingNote.currentVersion, null, "content", null, null, null),
                    httpRequest,
                )
            }.statusCode shouldBe HttpStatus.UNAUTHORIZED
        }

        @Test
        fun `should throw 400 for malformed note id`() {
            shouldThrow<ResponseStatusException> {
                sut.update(
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
            sut.delete(existingNote.id.value.toString(), httpRequest)

            val deleted = noteRepo.findById(existingNote.id)
            deleted?.isSoftDeleted shouldBe true
        }

        @Test
        fun `should throw 401 when unauthenticated`() {
            SecurityContextHolder.clearContext()

            shouldThrow<ResponseStatusException> {
                sut.delete(existingNote.id.value.toString(), httpRequest)
            }.statusCode shouldBe HttpStatus.UNAUTHORIZED
        }

        @Test
        fun `should throw 400 for malformed note id`() {
            shouldThrow<ResponseStatusException> {
                sut.delete("not-a-uuid", httpRequest)
            }.statusCode shouldBe HttpStatus.BAD_REQUEST
        }

        @Test
        fun `should throw NoteNotFoundException for unknown note id`() {
            shouldThrow<NoteNotFoundException> {
                sut.delete(UUID.randomUUID().toString(), httpRequest)
            }
        }
    }
}
