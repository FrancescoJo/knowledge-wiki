/*
 * CreateNoteUseCaseTest.kt
 *
 * $Since: 2026-05-30T00:00:00Z
 */
package com.fj.omnimemo.core.note.usecase

import com.fj.omnimemo.core.note.exception.DuplicateNoteTitleException
import com.fj.omnimemo.core.note.model.NoteAccessLevel
import com.fj.omnimemo.core.note.model.NoteAction
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
class CreateNoteUseCaseTest {

    private val noteRepo = MockNoteRepository()
    private val versionRepo = MockNoteVersionRepository()
    private val auditRepo = MockNoteAuditRepository()
    private val useCase = CreateNoteUseCase(noteRepo, versionRepo, auditRepo)

    private val authorId = UserId.generate()

    @BeforeEach
    fun setUp() {
        noteRepo.clear()
        versionRepo.clear()
        auditRepo.clear()
    }

    @Nested
    inner class Create {

        @Test
        fun `should persist note with currentVersion 1 after creation`() {
            val note = useCase.create(
                authorId, NoteLanguage.EN, "Getting Started", "# Hello",
                NoteAccessLevel.PUBLIC, NoteStatus.PUBLISHED, "127.0.0.1", null,
            )

            assertSoftly {
                noteRepo.findById(note.id) shouldNotBe null
                note.currentVersion shouldBe 1
                note.authorId shouldBe authorId
            }
        }

        @Test
        fun `should store first version content`() {
            val note = useCase.create(
                authorId, NoteLanguage.EN, "Getting Started", "# Hello",
                NoteAccessLevel.PUBLIC, NoteStatus.PUBLISHED, "127.0.0.1", null,
            )

            versionRepo.findContent(note.id, 1) shouldBe "# Hello"
        }

        @Test
        fun `should record a CREATE audit entry`() {
            val note = useCase.create(
                authorId, NoteLanguage.EN, "Getting Started", "# Hello",
                NoteAccessLevel.PUBLIC, NoteStatus.PUBLISHED, "127.0.0.1", null,
            )

            val audits = auditRepo.findAllByNoteId(note.id)
            assertSoftly {
                audits.size shouldBe 1
                audits[0].action shouldBe NoteAction.CREATE
                audits[0].actorId shouldBe authorId
                audits[0].version shouldBe 1
            }
        }

        @Test
        fun `should set titleIndex from language extraction`() {
            val note = useCase.create(
                authorId, NoteLanguage.EN, "Getting Started", "",
                NoteAccessLevel.PUBLIC, NoteStatus.PUBLISHED, "127.0.0.1", null,
            )

            note.titleIndex shouldBe "G"
        }

        @Test
        fun `should throw DuplicateNoteTitleException when title already exists`() {
            useCase.create(
                authorId, NoteLanguage.EN, "Getting Started", "",
                NoteAccessLevel.PUBLIC, NoteStatus.PUBLISHED, "127.0.0.1", null,
            )

            shouldThrow<DuplicateNoteTitleException> {
                useCase.create(
                    authorId, NoteLanguage.EN, "Getting Started", "",
                    NoteAccessLevel.PUBLIC, NoteStatus.PUBLISHED, "127.0.0.1", null,
                )
            }
        }
    }
}
