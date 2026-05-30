/*
 * NoteVersionRepositoryImplTest.kt
 *
 * $Since: 2026-05-30T00:00:00Z
 */
package com.fj.omnimemo.infrastructure.note.persistence

import com.fj.omnimemo.core.note.model.Note
import com.fj.omnimemo.core.note.model.NoteAccessLevel
import com.fj.omnimemo.core.note.model.NoteId
import com.fj.omnimemo.core.note.model.NoteLanguage
import com.fj.omnimemo.core.note.model.NoteStatus
import com.fj.omnimemo.core.test.annotation.MediumTest
import com.fj.omnimemo.core.user.model.User
import com.fj.omnimemo.infrastructure.security.AesGcmCipher
import com.fj.omnimemo.infrastructure.security.HmacBlindIndex
import com.fj.omnimemo.infrastructure.test.InfrastructureTestDatabase
import com.fj.omnimemo.infrastructure.user.persistence.UserRepositoryImpl
import com.fj.omnimemo.infrastructure.user.persistence.UserTableFixture
import io.kotest.assertions.assertSoftly
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

/**
 * Medium Tests for [NoteVersionRepositoryImpl]: verifies snapshot/delta
 * persistence and content reconstruction against a real PostgreSQL instance.
 *
 * @author Francesco Jo
 * @since 0.2.0
 * @version 0.2.0
 */
@MediumTest
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class NoteVersionRepositoryImplTest {

    private val jdbc = InfrastructureTestDatabase.jdbc
    private val userRepo = UserRepositoryImpl(jdbc, TEST_AES_CIPHER, TEST_HMAC_INDEX)
    private val noteRepo = NoteRepositoryImpl(jdbc)
    private val repo = NoteVersionRepositoryImpl(jdbc)
    private val users = UserTableFixture(jdbc)
    private val notes = NoteTableFixture(jdbc)

    private lateinit var testUser: User
    private lateinit var testNote: Note

    @BeforeEach
    fun setUp() {
        notes.deleteAll()
        users.deleteAll()
        testUser = User.create("author@example.com", "hash")
        userRepo.save(testUser)
        testNote = Note.create(
            authorId = testUser.id,
            language = NoteLanguage.EN,
            title = "Test Note",
            accessLevel = NoteAccessLevel.PUBLIC,
            status = NoteStatus.PUBLISHED,
        )
        noteRepo.save(testNote)
    }

    @Nested
    inner class SaveContent {

        @Test
        fun `version 1 should be stored as a snapshot`() {
            repo.saveContent(testNote.id, 1, "# Hello")

            notes.isSnapshot(testNote.id, 1) shouldBe true
        }

        @Test
        fun `version 2 should be stored as a delta`() {
            repo.saveContent(testNote.id, 1, "# Hello")
            repo.saveContent(testNote.id, 2, "# Hello\n\nNew line.")

            notes.isSnapshot(testNote.id, 2) shouldBe false
        }

        @Test
        fun `version 10 should be stored as a snapshot`() {
            var content = "# v1"
            repo.saveContent(testNote.id, 1, content)
            for (v in 2..10) {
                content = "# v$v"
                repo.saveContent(testNote.id, v, content)
            }

            notes.isSnapshot(testNote.id, 10) shouldBe true
        }
    }

    @Nested
    inner class FindContent {

        @Test
        fun `should return snapshot content for version 1`() {
            repo.saveContent(testNote.id, 1, "# Hello")

            repo.findContent(testNote.id, 1) shouldBe "# Hello"
        }

        @Test
        fun `should reconstruct content from snapshot and delta`() {
            val v1 = "# Hello\n\nOriginal."
            val v2 = "# Hello\n\nRevised."
            repo.saveContent(testNote.id, 1, v1)
            repo.saveContent(testNote.id, 2, v2)

            repo.findContent(testNote.id, 2) shouldBe v2
        }

        @Test
        fun `should reconstruct content across multiple deltas`() {
            repo.saveContent(testNote.id, 1, "line 1")
            repo.saveContent(testNote.id, 2, "line 1\nline 2")
            repo.saveContent(testNote.id, 3, "line 1\nline 2\nline 3")

            repo.findContent(testNote.id, 3) shouldBe "line 1\nline 2\nline 3"
        }

        @Test
        fun `should return null when version does not exist`() {
            repo.saveContent(testNote.id, 1, "content")

            repo.findContent(testNote.id, 99) shouldBe null
        }

        @Test
        fun `should return null when note has no versions`() {
            repo.findContent(NoteId.generate(), 1) shouldBe null
        }
    }

    @Nested
    inner class FindAllByNoteId {

        @Test
        fun `should return all versions in order`() {
            repo.saveContent(testNote.id, 1, "v1")
            repo.saveContent(testNote.id, 2, "v2")
            repo.saveContent(testNote.id, 3, "v3")

            val versions = repo.findAllByNoteId(testNote.id)

            assertSoftly {
                versions.size shouldBe 3
                versions[0].version shouldBe 1
                versions[1].version shouldBe 2
                versions[2].version shouldBe 3
            }
        }

        @Test
        fun `should return empty list when note has no versions`() {
            repo.findAllByNoteId(testNote.id) shouldBe emptyList()
        }
    }

    companion object {
        private val TEST_AES_CIPHER = AesGcmCipher(ByteArray(32) { it.toByte() })
        private val TEST_HMAC_INDEX = HmacBlindIndex(ByteArray(32) { (it + 32).toByte() })
    }
}
