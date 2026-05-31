/*
 * FindNoteUseCaseTest.kt
 *
 * $Since: 2026-05-30T00:00:00Z
 */
package testcase.small.core.note.usecase

import com.fj.omnimemo.core.note.exception.NoteAccessDeniedException
import com.fj.omnimemo.core.note.exception.NoteNotFoundException
import com.fj.omnimemo.core.note.model.*
import com.fj.omnimemo.core.note.usecase.CreateNoteUseCase
import com.fj.omnimemo.core.note.usecase.FindNoteUseCase
import com.fj.omnimemo.core.note.usecase.SoftDeleteNoteUseCase
import com.fj.omnimemo.core.user.model.UserId
import io.kotest.assertions.assertSoftly
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import test.com.fj.omnimemo.core.annotation.SmallTest
import test.com.fj.omnimemo.core.note.repository.MockNoteAuditRepository
import test.com.fj.omnimemo.core.note.repository.MockNoteRepository
import test.com.fj.omnimemo.core.note.repository.MockNoteVersionRepository

@SmallTest
class FindNoteUseCaseTest {
    private lateinit var noteRepo: MockNoteRepository
    private lateinit var versionRepo: MockNoteVersionRepository
    private lateinit var createUseCase: CreateNoteUseCase
    private lateinit var softDeleteUseCase: SoftDeleteNoteUseCase
    private lateinit var sut: FindNoteUseCase

    private val authorId = UserId.generate()
    private val otherUserId = UserId.generate()

    @BeforeEach
    fun setUp() {
        noteRepo = MockNoteRepository()
        versionRepo = MockNoteVersionRepository()
        createUseCase = CreateNoteUseCase(noteRepo, versionRepo, MockNoteAuditRepository())
        softDeleteUseCase = SoftDeleteNoteUseCase(noteRepo, MockNoteAuditRepository())
        sut = FindNoteUseCase(noteRepo, versionRepo)
    }

    @AfterEach
    fun tearDown() {
        noteRepo.clear()
        versionRepo.clear()
    }

    private fun createPublishedNote(
        title: String = "My Note",
        content: String = "body",
        accessLevel: NoteAccessLevel = NoteAccessLevel.PUBLIC,
    ) = createUseCase.create(
        authorId, NoteLanguage.EN, title, content,
        accessLevel, NoteStatus.PUBLISHED, "127.0.0.1", null,
    )

    @Nested
    inner class FindById {

        @Test
        fun `should return note and content when note is public`() {
            val created = createPublishedNote(content = "hello")

            val result = sut.findById(created.id, requesterId = null)

            assertSoftly {
                result.note.id shouldBe created.id
                result.content shouldBe "hello"
            }
        }

        @Test
        fun `should throw NoteNotFoundException when note does not exist`() {
            shouldThrow<NoteNotFoundException> {
                sut.findById(NoteId.generate(), requesterId = null)
            }
        }

        @Test
        fun `should throw NoteAccessDeniedException when requester is anonymous and note is RESTRICTED`() {
            val note = createPublishedNote(accessLevel = NoteAccessLevel.RESTRICTED)

            shouldThrow<NoteAccessDeniedException> {
                sut.findById(note.id, requesterId = null)
            }
        }

        @Test
        fun `should return note when requester is authenticated and note is RESTRICTED`() {
            val note = createPublishedNote(accessLevel = NoteAccessLevel.RESTRICTED)

            val result = sut.findById(note.id, requesterId = otherUserId)

            result.note.id shouldBe note.id
        }

        @Test
        fun `should throw NoteAccessDeniedException when requester is not author and note is PRIVATE`() {
            val note = createPublishedNote(accessLevel = NoteAccessLevel.PRIVATE)

            shouldThrow<NoteAccessDeniedException> {
                sut.findById(note.id, requesterId = otherUserId)
            }
        }

        @Test
        fun `should return note when requester is author and note is PRIVATE`() {
            val note = createPublishedNote(accessLevel = NoteAccessLevel.PRIVATE)

            val result = sut.findById(note.id, requesterId = authorId)

            result.note.id shouldBe note.id
        }

        @Test
        fun `should throw NoteNotFoundException when note is soft-deleted and requester is not author`() {
            val note = createPublishedNote()
            softDeleteUseCase.softDelete(note.id, authorId, "127.0.0.1")

            shouldThrow<NoteNotFoundException> {
                sut.findById(note.id, requesterId = otherUserId)
            }
        }

        @Test
        fun `should throw NoteNotFoundException when note is soft-deleted and requester is anonymous`() {
            val note = createPublishedNote()
            softDeleteUseCase.softDelete(note.id, authorId, "127.0.0.1")

            shouldThrow<NoteNotFoundException> {
                sut.findById(note.id, requesterId = null)
            }
        }

        @Test
        fun `should throw NoteAccessDeniedException when note is DRAFT and requester is not author`() {
            val draft = createUseCase.create(
                authorId, NoteLanguage.EN, "Draft Note", "wip",
                NoteAccessLevel.PUBLIC, NoteStatus.DRAFT, "127.0.0.1", null,
            )

            shouldThrow<NoteAccessDeniedException> {
                sut.findById(draft.id, requesterId = otherUserId)
            }
        }

        @Test
        fun `should throw NoteAccessDeniedException when note is DRAFT and requester is anonymous`() {
            val draft = createUseCase.create(
                authorId, NoteLanguage.EN, "Draft Note", "wip",
                NoteAccessLevel.PUBLIC, NoteStatus.DRAFT, "127.0.0.1", null,
            )

            shouldThrow<NoteAccessDeniedException> {
                sut.findById(draft.id, requesterId = null)
            }
        }

        @Test
        fun `should allow author to access their own soft-deleted note`() {
            val note = createPublishedNote()
            softDeleteUseCase.softDelete(note.id, authorId, "127.0.0.1")

            val result = sut.findById(note.id, requesterId = authorId)

            result.note.id shouldBe note.id
        }

        @Test
        fun `should allow author to access their own draft`() {
            val draft = createUseCase.create(
                authorId, NoteLanguage.EN, "Draft Note", "wip",
                NoteAccessLevel.PUBLIC, NoteStatus.DRAFT, "127.0.0.1", null,
            )

            val result = sut.findById(draft.id, requesterId = authorId)

            result.note.id shouldBe draft.id
        }

        @Test
        fun `should return empty content when no version content exists for the note`() {
            val note = Note.create(authorId, NoteLanguage.EN, "Bare Note", NoteAccessLevel.PUBLIC, NoteStatus.PUBLISHED)
            noteRepo.save(note)

            val result = sut.findById(note.id, requesterId = null)

            result.content shouldBe ""
        }
    }

    @Nested
    inner class FindByTitle {

        @Test
        fun `should return note when title matches`() {
            createPublishedNote(title = "Kotlin Guide")

            val result = sut.findByTitle("Kotlin Guide", requesterId = null)

            result.note.title shouldBe "Kotlin Guide"
        }

        @Test
        fun `should throw NoteNotFoundException when title does not match`() {
            shouldThrow<NoteNotFoundException> {
                sut.findByTitle("Nonexistent", requesterId = null)
            }
        }
    }
}
