/*
 * NoteControllerImplTest.kt
 *
 * $Since: 2026-05-30T00:00:00Z
 */
package testcase.small.api.endpoint.note.impl

import com.fj.omnimemo.api.endpoint.note.impl.NoteControllerImpl
import com.fj.omnimemo.core.note.exception.NoteAccessDeniedException
import com.fj.omnimemo.core.note.exception.NoteNotFoundException
import com.fj.omnimemo.core.note.model.Note
import com.fj.omnimemo.core.note.model.NoteAccessLevel
import com.fj.omnimemo.core.note.model.NoteLanguage
import com.fj.omnimemo.core.note.model.NoteStatus
import com.fj.omnimemo.core.note.usecase.CreateNoteUseCase
import com.fj.omnimemo.core.note.usecase.FindNoteUseCase
import com.fj.omnimemo.core.note.usecase.ListNotesUseCase
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
import test.com.fj.omnimemo.core.annotation.SmallTest
import test.com.fj.omnimemo.core.note.repository.MockNoteAuditRepository
import test.com.fj.omnimemo.core.note.repository.MockNoteRepository
import test.com.fj.omnimemo.core.note.repository.MockNoteVersionRepository
import java.util.*

@SmallTest
class NoteControllerImplTest {
    private lateinit var noteRepo: MockNoteRepository
    private lateinit var noteVersionRepo: MockNoteVersionRepository
    private lateinit var noteAuditRepo: MockNoteAuditRepository
    private lateinit var createNoteUseCase: CreateNoteUseCase
    private lateinit var sut: NoteControllerImpl

    private val authorId = UserId(UUID.randomUUID())
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
        createNoteUseCase = CreateNoteUseCase(noteRepo, noteVersionRepo, noteAuditRepo)
        sut = NoteControllerImpl(
            findNoteUseCase = FindNoteUseCase(noteRepo, noteVersionRepo),
            listNotesUseCase = ListNotesUseCase(noteRepo),
        )

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
        noteRepo.clear()
        noteVersionRepo.clear()
        noteAuditRepo.clear()
    }

    @Nested
    inner class List {

        @Test
        fun `should return notes grouped by title index`() {
            val result = sut.list("en")

            result shouldNotBe null
            result["G"]?.any { it.title == "Getting Started" } shouldBe true
        }

        @Test
        fun `should return empty map when no notes exist for language`() {
            val result = sut.list("ko")

            result shouldBe emptyMap()
        }

        @Test
        fun `should throw 400 for unknown language code`() {
            shouldThrow<ResponseStatusException> {
                sut.list("xx")
            }.statusCode shouldBe HttpStatus.BAD_REQUEST
        }
    }

    @Nested
    inner class FindById {

        @Test
        fun `should return NoteResponse for an existing note`() {
            val response = sut.findById(existingNote.id.value.toString())

            response.title shouldBe "Getting Started"
            response.language shouldBe "en"
            response.authorId shouldBe authorId.value.toString()
        }

        @Test
        fun `should throw NoteNotFoundException for unknown note id`() {
            shouldThrow<NoteNotFoundException> {
                sut.findById(UUID.randomUUID().toString())
            }
        }

        @Test
        fun `should throw 400 for malformed note id`() {
            shouldThrow<ResponseStatusException> {
                sut.findById("not-a-uuid")
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
                sut.findById(privateNote.id.value.toString())
            }
        }
    }
}
