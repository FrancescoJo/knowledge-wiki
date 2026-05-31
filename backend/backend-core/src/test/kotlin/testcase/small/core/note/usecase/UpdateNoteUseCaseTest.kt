/*
 * UpdateNoteUseCaseTest.kt
 *
 * $Since: 2026-05-30T00:00:00Z
 */
package testcase.small.core.note.usecase

import com.fj.omnimemo.core.note.exception.*
import com.fj.omnimemo.core.note.model.*
import com.fj.omnimemo.core.note.usecase.CreateNoteUseCase
import com.fj.omnimemo.core.note.usecase.SoftDeleteNoteUseCase
import com.fj.omnimemo.core.note.usecase.UpdateNoteUseCase
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
class UpdateNoteUseCaseTest {
    private lateinit var noteRepo: MockNoteRepository
    private lateinit var versionRepo: MockNoteVersionRepository
    private lateinit var auditRepo: MockNoteAuditRepository
    private lateinit var createUseCase: CreateNoteUseCase
    private lateinit var softDeleteUseCase: SoftDeleteNoteUseCase
    private lateinit var sut: UpdateNoteUseCase

    private val authorId = UserId.generate()
    private val otherUserId = UserId.generate()

    @BeforeEach
    fun setUp() {
        noteRepo = MockNoteRepository()
        versionRepo = MockNoteVersionRepository()
        auditRepo = MockNoteAuditRepository()
        createUseCase = CreateNoteUseCase(noteRepo, versionRepo, auditRepo)
        softDeleteUseCase = SoftDeleteNoteUseCase(noteRepo, auditRepo)
        sut = UpdateNoteUseCase(noteRepo, versionRepo, auditRepo)
    }

    @AfterEach
    fun tearDown() {
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

            val updated = sut.update(
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

            val updated = sut.update(
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

            sut.update(
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
                sut.update(
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
                sut.update(
                    note.id, authorId, expectedVersion = 1,
                    title = null, content = "", accessLevel = null, status = null, "127.0.0.1", null,
                )
            }
        }

        @Test
        fun `should throw NoteAccessDeniedException when requester is not the author`() {
            val note = createNote()

            shouldThrow<NoteAccessDeniedException> {
                sut.update(
                    note.id, otherUserId, expectedVersion = 1,
                    title = null, content = "", accessLevel = null, status = null, "127.0.0.1", null,
                )
            }
        }

        @Test
        fun `should throw StaleNoteVersionException when expected version does not match`() {
            val note = createNote()

            shouldThrow<StaleNoteVersionException> {
                sut.update(
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
                sut.update(
                    note.id, authorId, expectedVersion = 1,
                    title = "Existing Title", content = "", accessLevel = null, status = null, "127.0.0.1", null,
                )
            }
        }

        @Test
        fun `should allow updating with the same title (no-op rename)`() {
            val note = createNote(title = "Same Title")

            val updated = sut.update(
                note.id, authorId, expectedVersion = 1,
                title = "Same Title", content = "new content",
                accessLevel = null, status = null, "127.0.0.1", null,
            )

            updated.title shouldBe "Same Title"
        }

        @Test
        fun `should update access level when provided`() {
            val note = createNote()

            val updated = sut.update(
                note.id, authorId, expectedVersion = 1,
                title = null, content = "content",
                accessLevel = NoteAccessLevel.PRIVATE, status = null, "127.0.0.1", null,
            )

            updated.accessLevel shouldBe NoteAccessLevel.PRIVATE
        }

        @Test
        fun `should update status when provided`() {
            val note = createNote()

            val updated = sut.update(
                note.id, authorId, expectedVersion = 1,
                title = null, content = "content",
                accessLevel = null, status = NoteStatus.DRAFT, "127.0.0.1", null,
            )

            updated.status shouldBe NoteStatus.DRAFT
        }
    }
}
