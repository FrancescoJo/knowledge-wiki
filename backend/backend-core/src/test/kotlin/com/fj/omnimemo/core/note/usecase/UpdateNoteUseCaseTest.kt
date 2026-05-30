/*
 * UpdateNoteUseCaseTest.kt
 *
 * $Since: 2026-05-30T00:00:00Z
 */
package com.fj.omnimemo.core.note.usecase

import com.fj.omnimemo.core.note.exception.DuplicateNoteTitleException
import com.fj.omnimemo.core.note.exception.NoteAccessDeniedException
import com.fj.omnimemo.core.note.exception.NoteAlreadyDeletedException
import com.fj.omnimemo.core.note.exception.NoteNotFoundException
import com.fj.omnimemo.core.note.exception.StaleNoteVersionException
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
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

@SmallTest
class UpdateNoteUseCaseTest {

    private val noteRepo = MockNoteRepository()
    private val versionRepo = MockNoteVersionRepository()
    private val auditRepo = MockNoteAuditRepository()
    private val createUseCase = CreateNoteUseCase(noteRepo, versionRepo, auditRepo)
    private val softDeleteUseCase = SoftDeleteNoteUseCase(noteRepo, auditRepo)
    private val useCase = UpdateNoteUseCase(noteRepo, versionRepo, auditRepo)

    private val authorId = UserId.generate()
    private val otherUserId = UserId.generate()

    @BeforeEach
    fun setUp() {
        noteRepo.clear()
        versionRepo.clear()
        auditRepo.clear()
    }

    private fun createNote(title: String = "Original Title") = createUseCase.create(
        authorId, NoteLanguage.EN, title, "original content",
        NoteAccessLevel.PUBLIC, NoteStatus.PUBLISHED, "127.0.0.1", null,
    )

    @Nested
    inner class Update {

        @Test
        fun `should increment currentVersion and store new content`() {
            val note = createNote()

            val updated = useCase.update(
                note.id, authorId, expectedVersion = 1,
                title = null, content = "updated content",
                accessLevel = null, status = null, "127.0.0.1", null,
            )

            assertSoftly {
                updated.currentVersion shouldBe 2
                versionRepo.findContent(note.id, 2) shouldBe "updated content"
            }
        }

        @Test
        fun `should update title and recompute titleIndex`() {
            val note = createNote(title = "Alpha")

            val updated = useCase.update(
                note.id, authorId, expectedVersion = 1,
                title = "Beta Guide", content = "content",
                accessLevel = null, status = null, "127.0.0.1", null,
            )

            assertSoftly {
                updated.title shouldBe "Beta Guide"
                updated.titleIndex shouldBe "B"
            }
        }

        @Test
        fun `should record an EDIT audit entry`() {
            val note = createNote()
            auditRepo.clear()

            useCase.update(
                note.id, authorId, expectedVersion = 1,
                title = null, content = "v2",
                accessLevel = null, status = null, "10.0.0.1", "minor fix",
            )

            val audits = auditRepo.savedAudits()
            assertSoftly {
                audits.size shouldBe 1
                audits[0].action shouldBe NoteAction.EDIT
                audits[0].version shouldBe 2
                audits[0].summary shouldBe "minor fix"
            }
        }

        @Test
        fun `should throw NoteNotFoundException when note does not exist`() {
            shouldThrow<NoteNotFoundException> {
                useCase.update(
                    NoteId.generate(), authorId, expectedVersion = 1,
                    title = null, content = "", accessLevel = null, status = null, "127.0.0.1", null,
                )
            }
        }

        @Test
        fun `should throw NoteAlreadyDeletedException when note is soft-deleted`() {
            val note = createNote()
            softDeleteUseCase.softDelete(note.id, authorId, "127.0.0.1")

            shouldThrow<NoteAlreadyDeletedException> {
                useCase.update(
                    note.id, authorId, expectedVersion = 1,
                    title = null, content = "", accessLevel = null, status = null, "127.0.0.1", null,
                )
            }
        }

        @Test
        fun `should throw NoteAccessDeniedException when requester is not the author`() {
            val note = createNote()

            shouldThrow<NoteAccessDeniedException> {
                useCase.update(
                    note.id, otherUserId, expectedVersion = 1,
                    title = null, content = "", accessLevel = null, status = null, "127.0.0.1", null,
                )
            }
        }

        @Test
        fun `should throw StaleNoteVersionException when expected version does not match`() {
            val note = createNote()

            shouldThrow<StaleNoteVersionException> {
                useCase.update(
                    note.id, authorId, expectedVersion = 99,
                    title = null, content = "", accessLevel = null, status = null, "127.0.0.1", null,
                )
            }
        }

        @Test
        fun `should throw DuplicateNoteTitleException when new title is already taken`() {
            createNote(title = "Existing Title")
            val note = createNote(title = "My Note")

            shouldThrow<DuplicateNoteTitleException> {
                useCase.update(
                    note.id, authorId, expectedVersion = 1,
                    title = "Existing Title", content = "", accessLevel = null, status = null, "127.0.0.1", null,
                )
            }
        }

        @Test
        fun `should allow updating with the same title (no-op rename)`() {
            val note = createNote(title = "Same Title")

            val updated = useCase.update(
                note.id, authorId, expectedVersion = 1,
                title = "Same Title", content = "new content",
                accessLevel = null, status = null, "127.0.0.1", null,
            )

            updated.title shouldBe "Same Title"
        }
    }
}
