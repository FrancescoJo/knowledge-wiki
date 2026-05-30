/*
 * FindNoteUseCaseTest.kt
 *
 * $Since: 2026-05-30T00:00:00Z
 */
package com.fj.omnimemo.core.note.usecase

import com.fj.omnimemo.core.note.exception.NoteAccessDeniedException
import com.fj.omnimemo.core.note.exception.NoteNotFoundException
import com.fj.omnimemo.core.note.model.NoteAccessLevel
import com.fj.omnimemo.core.note.model.NoteId
import com.fj.omnimemo.core.note.model.NoteLanguage
import com.fj.omnimemo.core.note.model.NoteStatus
import com.fj.omnimemo.core.note.repository.MockNoteAuditRepository
import com.fj.omnimemo.core.note.repository.MockNoteRepository
import com.fj.omnimemo.core.note.repository.MockNoteVersionRepository
import com.fj.omnimemo.core.test.annotation.SmallTest
import com.fj.omnimemo.core.user.model.UserId
import io.kotest.assertions.assertSoftly
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

@SmallTest
class FindNoteUseCaseTest {

    private val noteRepo = MockNoteRepository()
    private val versionRepo = MockNoteVersionRepository()
    private val createUseCase = CreateNoteUseCase(noteRepo, versionRepo, MockNoteAuditRepository())
    private val softDeleteUseCase = SoftDeleteNoteUseCase(noteRepo, MockNoteAuditRepository())
    private val useCase = FindNoteUseCase(noteRepo, versionRepo)

    private val authorId = UserId.generate()
    private val otherUserId = UserId.generate()

    @BeforeEach
    fun setUp() {
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

            val result = useCase.findById(created.id, requesterId = null)

            assertSoftly {
                result.note.id shouldBe created.id
                result.content shouldBe "hello"
            }
        }

        @Test
        fun `should throw NoteNotFoundException when note does not exist`() {
            shouldThrow<NoteNotFoundException> {
                useCase.findById(NoteId.generate(), requesterId = null)
            }
        }

        @Test
        fun `should throw NoteAccessDeniedException when requester is anonymous and note is RESTRICTED`() {
            val note = createPublishedNote(accessLevel = NoteAccessLevel.RESTRICTED)

            shouldThrow<NoteAccessDeniedException> {
                useCase.findById(note.id, requesterId = null)
            }
        }

        @Test
        fun `should return note when requester is authenticated and note is RESTRICTED`() {
            val note = createPublishedNote(accessLevel = NoteAccessLevel.RESTRICTED)

            val result = useCase.findById(note.id, requesterId = otherUserId)

            result.note.id shouldBe note.id
        }

        @Test
        fun `should throw NoteAccessDeniedException when requester is not author and note is PRIVATE`() {
            val note = createPublishedNote(accessLevel = NoteAccessLevel.PRIVATE)

            shouldThrow<NoteAccessDeniedException> {
                useCase.findById(note.id, requesterId = otherUserId)
            }
        }

        @Test
        fun `should return note when requester is author and note is PRIVATE`() {
            val note = createPublishedNote(accessLevel = NoteAccessLevel.PRIVATE)

            val result = useCase.findById(note.id, requesterId = authorId)

            result.note.id shouldBe note.id
        }

        @Test
        fun `should throw NoteNotFoundException when note is soft-deleted and requester is not author`() {
            val note = createPublishedNote()
            softDeleteUseCase.softDelete(note.id, authorId, "127.0.0.1")

            shouldThrow<NoteNotFoundException> {
                useCase.findById(note.id, requesterId = otherUserId)
            }
        }

        @Test
        fun `should throw NoteAccessDeniedException when note is DRAFT and requester is not author`() {
            val draft = createUseCase.create(
                authorId, NoteLanguage.EN, "Draft Note", "wip",
                NoteAccessLevel.PUBLIC, NoteStatus.DRAFT, "127.0.0.1", null,
            )

            shouldThrow<NoteAccessDeniedException> {
                useCase.findById(draft.id, requesterId = otherUserId)
            }
        }
    }

    @Nested
    inner class FindByTitle {

        @Test
        fun `should return note when title matches`() {
            createPublishedNote(title = "Kotlin Guide")

            val result = useCase.findByTitle("Kotlin Guide", requesterId = null)

            result.note.title shouldBe "Kotlin Guide"
        }

        @Test
        fun `should throw NoteNotFoundException when title does not match`() {
            shouldThrow<NoteNotFoundException> {
                useCase.findByTitle("Nonexistent", requesterId = null)
            }
        }
    }
}
