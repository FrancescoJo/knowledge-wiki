/*
 * ListNotesUseCaseTest.kt
 *
 * $Since: 2026-05-30T00:00:00Z
 */
package com.fj.omnimemo.core.note.usecase

import com.fj.omnimemo.core.note.model.NoteAccessLevel
import com.fj.omnimemo.core.note.model.NoteLanguage
import com.fj.omnimemo.core.note.model.NoteStatus
import com.fj.omnimemo.core.note.repository.MockNoteAuditRepository
import com.fj.omnimemo.core.note.repository.MockNoteRepository
import com.fj.omnimemo.core.note.repository.MockNoteVersionRepository
import com.fj.omnimemo.core.test.annotation.SmallTest
import com.fj.omnimemo.core.user.model.UserId
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.maps.shouldBeEmpty
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

@SmallTest
class ListNotesUseCaseTest {

    private val noteRepo = MockNoteRepository()
    private val versionRepo = MockNoteVersionRepository()
    private val auditRepo = MockNoteAuditRepository()
    private val createUseCase = CreateNoteUseCase(noteRepo, versionRepo, auditRepo)
    private val softDeleteUseCase = SoftDeleteNoteUseCase(noteRepo, auditRepo)
    private val useCase = ListNotesUseCase(noteRepo)

    private val authorId = UserId.generate()
    private val otherUserId = UserId.generate()

    @BeforeEach
    fun setUp() {
        noteRepo.clear()
        versionRepo.clear()
        auditRepo.clear()
    }

    private fun create(
        title: String,
        accessLevel: NoteAccessLevel = NoteAccessLevel.PUBLIC,
        status: NoteStatus = NoteStatus.PUBLISHED,
        language: NoteLanguage = NoteLanguage.EN,
        author: UserId = authorId,
    ) = createUseCase.create(author, language, title, "", accessLevel, status, "127.0.0.1", null)

    @Nested
    inner class ListByLanguage {

        @Test
        fun `should group notes by titleIndex`() {
            create("Apple")
            create("Banana")
            create("Avocado")

            val result = useCase.listByLanguage(NoteLanguage.EN, requesterId = null)

            result["A"]?.map { it.title } shouldContainExactlyInAnyOrder listOf("Apple", "Avocado")
            result["B"]?.map { it.title } shouldContainExactlyInAnyOrder listOf("Banana")
        }

        @Test
        fun `should exclude soft-deleted notes`() {
            val note = create("Apple")
            softDeleteUseCase.softDelete(note.id, authorId, "127.0.0.1")

            val result = useCase.listByLanguage(NoteLanguage.EN, requesterId = null)

            result.shouldBeEmpty()
        }

        @Test
        fun `should exclude RESTRICTED notes for anonymous requester`() {
            create("Public Note", accessLevel = NoteAccessLevel.PUBLIC)
            create("Members Note", accessLevel = NoteAccessLevel.RESTRICTED)

            val result = useCase.listByLanguage(NoteLanguage.EN, requesterId = null)

            result.values.flatten() shouldHaveSize 1
            result.values.flatten()[0].title shouldBe "Public Note"
        }

        @Test
        fun `should include RESTRICTED notes for authenticated requester`() {
            create("Public Note", accessLevel = NoteAccessLevel.PUBLIC)
            create("Members Note", accessLevel = NoteAccessLevel.RESTRICTED)

            val result = useCase.listByLanguage(NoteLanguage.EN, requesterId = otherUserId)

            result.values.flatten() shouldHaveSize 2
        }

        @Test
        fun `should exclude PRIVATE notes for non-author`() {
            create("Secret Note", accessLevel = NoteAccessLevel.PRIVATE)

            val result = useCase.listByLanguage(NoteLanguage.EN, requesterId = otherUserId)

            result.shouldBeEmpty()
        }

        @Test
        fun `should include PRIVATE notes for author`() {
            create("Secret Note", accessLevel = NoteAccessLevel.PRIVATE)

            val result = useCase.listByLanguage(NoteLanguage.EN, requesterId = authorId)

            result.values.flatten() shouldHaveSize 1
        }

        @Test
        fun `should exclude DRAFT notes for non-author`() {
            create("Draft Note", status = NoteStatus.DRAFT)

            val result = useCase.listByLanguage(NoteLanguage.EN, requesterId = otherUserId)

            result.shouldBeEmpty()
        }

        @Test
        fun `should include DRAFT notes for author`() {
            create("Draft Note", status = NoteStatus.DRAFT)

            val result = useCase.listByLanguage(NoteLanguage.EN, requesterId = authorId)

            result.values.flatten() shouldHaveSize 1
        }

        @Test
        fun `should not include notes from a different language`() {
            create("Kotlin Guide", language = NoteLanguage.EN)
            create("코틀린 가이드", language = NoteLanguage.KO)

            val result = useCase.listByLanguage(NoteLanguage.EN, requesterId = null)

            result.values.flatten() shouldHaveSize 1
            result.values.flatten()[0].title shouldBe "Kotlin Guide"
        }

        @Test
        fun `should return empty map when no notes exist`() {
            useCase.listByLanguage(NoteLanguage.EN, requesterId = null).shouldBeEmpty()
        }
    }
}
