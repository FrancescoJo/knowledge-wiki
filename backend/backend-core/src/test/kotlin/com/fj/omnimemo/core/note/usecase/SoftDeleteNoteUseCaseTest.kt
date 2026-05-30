/*
 * SoftDeleteNoteUseCaseTest.kt
 *
 * $Since: 2026-05-30T00:00:00Z
 */
package com.fj.omnimemo.core.note.usecase

import com.fj.omnimemo.core.note.exception.NoteAccessDeniedException
import com.fj.omnimemo.core.note.exception.NoteAlreadyDeletedException
import com.fj.omnimemo.core.note.exception.NoteNotFoundException
import com.fj.omnimemo.core.note.model.NoteAccessLevel
import com.fj.omnimemo.core.note.model.NoteAction
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
import io.kotest.matchers.shouldNotBe
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

@SmallTest
class SoftDeleteNoteUseCaseTest {

    private val noteRepo = MockNoteRepository()
    private val versionRepo = MockNoteVersionRepository()
    private val auditRepo = MockNoteAuditRepository()
    private val createUseCase = CreateNoteUseCase(noteRepo, versionRepo, auditRepo)
    private val useCase = SoftDeleteNoteUseCase(noteRepo, auditRepo)

    private val authorId = UserId.generate()
    private val otherUserId = UserId.generate()

    @BeforeEach
    fun setUp() {
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

            val deleted = useCase.softDelete(note.id, authorId, "127.0.0.1")

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

            useCase.softDelete(note.id, authorId, "127.0.0.1")

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
                useCase.softDelete(NoteId.generate(), authorId, "127.0.0.1")
            }
        }

        @Test
        fun `should throw NoteAccessDeniedException when requester is not the author`() {
            val note = createNote()

            shouldThrow<NoteAccessDeniedException> {
                useCase.softDelete(note.id, otherUserId, "127.0.0.1")
            }
        }

        @Test
        fun `should throw NoteAlreadyDeletedException when note is already soft-deleted`() {
            val note = createNote()
            useCase.softDelete(note.id, authorId, "127.0.0.1")

            shouldThrow<NoteAlreadyDeletedException> {
                useCase.softDelete(note.id, authorId, "127.0.0.1")
            }
        }
    }
}
