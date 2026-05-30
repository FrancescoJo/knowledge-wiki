/*
 * NoteAuditRepositoryImplTest.kt
 *
 * $Since: 2026-05-30T00:00:00Z
 */
package com.fj.omnimemo.infrastructure.note.persistence

import com.fj.omnimemo.core.note.model.Note
import com.fj.omnimemo.core.note.model.NoteAccessLevel
import com.fj.omnimemo.core.note.model.NoteAction
import com.fj.omnimemo.core.note.model.NoteAudit
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
 * Medium Tests for [NoteAuditRepositoryImpl]: verifies audit persistence
 * against a real PostgreSQL instance shared via [InfrastructureTestDatabase].
 *
 * @author Francesco Jo
 * @since 0.2.0
 * @version 0.2.0
 */
@MediumTest
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class NoteAuditRepositoryImplTest {

    private val jdbc = InfrastructureTestDatabase.jdbc
    private val userRepo = UserRepositoryImpl(jdbc, TEST_AES_CIPHER, TEST_HMAC_INDEX)
    private val noteRepo = NoteRepositoryImpl(jdbc)
    private val repo = NoteAuditRepositoryImpl(jdbc)
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
    inner class Save {

        @Test
        fun `should persist a CREATE audit entry`() {
            val audit = NoteAudit.create(
                noteId = testNote.id,
                version = 1,
                action = NoteAction.CREATE,
                actorId = testUser.id,
                remoteIp = "127.0.0.1",
                summary = null,
            )
            repo.save(audit)

            val found = repo.findAllByNoteId(testNote.id)
            assertSoftly {
                found.size shouldBe 1
                found[0].action shouldBe NoteAction.CREATE
                found[0].version shouldBe 1
                found[0].actorId shouldBe testUser.id
                found[0].remoteIp shouldBe "127.0.0.1"
                found[0].summary shouldBe null
                found[0].createdAt shouldNotBe null
            }
        }

        @Test
        fun `should persist summary when provided`() {
            val audit = NoteAudit.create(
                noteId = testNote.id,
                version = 2,
                action = NoteAction.EDIT,
                actorId = testUser.id,
                remoteIp = "10.0.0.1",
                summary = "fixed typo",
            )
            repo.save(audit)

            val found = repo.findAllByNoteId(testNote.id)
            found[0].summary shouldBe "fixed typo"
        }
    }

    @Nested
    inner class FindAllByNoteId {

        @Test
        fun `should return all audit entries in chronological order`() {
            repo.save(NoteAudit.create(testNote.id, 1, NoteAction.CREATE, testUser.id, "127.0.0.1", null))
            repo.save(NoteAudit.create(testNote.id, 2, NoteAction.EDIT, testUser.id, "127.0.0.1", null))

            val audits = repo.findAllByNoteId(testNote.id)

            assertSoftly {
                audits.size shouldBe 2
                audits[0].action shouldBe NoteAction.CREATE
                audits[1].action shouldBe NoteAction.EDIT
            }
        }

        @Test
        fun `should return empty list when no audits exist for the note`() {
            repo.findAllByNoteId(NoteId.generate()) shouldBe emptyList()
        }
    }

    companion object {
        private val TEST_AES_CIPHER = AesGcmCipher(ByteArray(32) { it.toByte() })
        private val TEST_HMAC_INDEX = HmacBlindIndex(ByteArray(32) { (it + 32).toByte() })
    }
}
