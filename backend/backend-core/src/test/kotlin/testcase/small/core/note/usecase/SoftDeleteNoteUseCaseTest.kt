/*
 * SoftDeleteNoteUseCaseTest.kt
 *
 * $Since: 2026-05-30T00:00:00Z
 */
package testcase.small.core.note.usecase

import com.fj.omnimemo.core.note.exception.NoteAccessDeniedException
import com.fj.omnimemo.core.note.exception.NoteAlreadyDeletedException
import com.fj.omnimemo.core.note.exception.NoteNotFoundException
import com.fj.omnimemo.core.note.model.*
import com.fj.omnimemo.core.note.usecase.CreateNoteUseCase
import com.fj.omnimemo.core.note.usecase.SoftDeleteNoteUseCase
import com.fj.omnimemo.core.user.model.UserId
import io.kotest.assertions.assertSoftly
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import test.com.fj.omnimemo.core.annotation.SmallTest
import test.com.fj.omnimemo.core.note.repository.MockNoteAuditRepository
import test.com.fj.omnimemo.core.note.repository.MockNoteRepository
import test.com.fj.omnimemo.core.note.repository.MockNoteVersionRepository

@SmallTest
class SoftDeleteNoteUseCaseTest {
    private lateinit var noteRepo: MockNoteRepository
    private lateinit var versionRepo: MockNoteVersionRepository
    private lateinit var auditRepo: MockNoteAuditRepository
    private lateinit var createUseCase: CreateNoteUseCase
    private lateinit var sut: SoftDeleteNoteUseCase

    private val authorId = UserId.generate()
    private val otherUserId = UserId.generate()

    @BeforeEach
    fun setUp() {
        noteRepo = MockNoteRepository()
        versionRepo = MockNoteVersionRepository()
        auditRepo = MockNoteAuditRepository()
        createUseCase = CreateNoteUseCase(noteRepo, versionRepo, auditRepo)
        sut = SoftDeleteNoteUseCase(noteRepo, auditRepo)
    }

    @AfterEach
    fun tearDown() {
        noteRepo.clear()
        versionRepo.clear()
        auditRepo.clear()
    }

    private fun createNote() = createUseCase.create(
        authorId, NoteLanguage.EN, "My Note", "content",
        NoteAccessLevel.PUBLIC, NoteStatus.PUBLISHED, "127.0.0.1", null,
    )

    @Nested
    inner class SoftDelete {

        @Test
        fun `should set softDeletedBy and softDeletedAt`() {
            val note = createNote()

            val deleted = sut.softDelete(note.id, authorId, "127.0.0.1")

            assertSoftly {
                deleted.softDeletedBy shouldBe authorId
                deleted.softDeletedAt shouldNotBe null
                deleted.isSoftDeleted shouldBe true
            }
        }

        @Test
        fun `should record a DELETE audit entry`() {
            val note = createNote()
            auditRepo.clear()

            sut.softDelete(note.id, authorId, "127.0.0.1")

            val audits = auditRepo.savedAudits()
            assertSoftly {
                audits.size shouldBe 1
                audits[0].action shouldBe NoteAction.DELETE
                audits[0].actorId shouldBe authorId
            }
        }

        @Test
        fun `should throw NoteNotFoundException when note does not exist`() {
            shouldThrow<NoteNotFoundException> {
                sut.softDelete(NoteId.generate(), authorId, "127.0.0.1")
            }
        }

        @Test
        fun `should throw NoteAccessDeniedException when requester is not the author`() {
            val note = createNote()

            shouldThrow<NoteAccessDeniedException> {
                sut.softDelete(note.id, otherUserId, "127.0.0.1")
            }
        }

        @Test
        fun `should throw NoteAlreadyDeletedException when note is already soft-deleted`() {
            val note = createNote()
            sut.softDelete(note.id, authorId, "127.0.0.1")

            shouldThrow<NoteAlreadyDeletedException> {
                sut.softDelete(note.id, authorId, "127.0.0.1")
            }
        }
    }
}
