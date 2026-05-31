/*
 * NoteRepositoryImplTest.kt
 *
 * $Since: 2026-05-30T00:00:00Z
 */
package testcase.medium.infrastructure.note.persistence

import com.fj.omnimemo.core.note.model.*
import com.fj.omnimemo.core.note.mutate
import com.fj.omnimemo.core.user.model.User
import com.fj.omnimemo.infrastructure.security.AesGcmCipher
import com.fj.omnimemo.infrastructure.security.HmacBlindIndex
import com.fj.omnimemo.infrastructure.note.persistence.NoteRepositoryImpl
import com.fj.omnimemo.infrastructure.user.persistence.UserRepositoryImpl
import io.kotest.assertions.assertSoftly
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import test.com.fj.omnimemo.core.annotation.MediumTest
import testcase.medium.infrastructure.InfrastructureTestDatabase
import test.com.fj.omnimemo.infrastructure.note.persistence.NoteTableFixture
import test.com.fj.omnimemo.infrastructure.user.persistence.UserTableFixture
import java.time.Instant

/**
 * Medium Tests for [NoteRepositoryImpl]: verifies persistence behaviour against
 * a real PostgreSQL instance shared via [InfrastructureTestDatabase].
 *
 * @author Francesco Jo
 * @since 0.2.0
 * @version 0.2.0
 */
@MediumTest
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class NoteRepositoryImplTest {
    private val jdbc = InfrastructureTestDatabase.jdbc
    private val userRepo = UserRepositoryImpl(jdbc, TEST_AES_CIPHER, TEST_HMAC_INDEX)
    private val repo = NoteRepositoryImpl(jdbc)
    private val users = UserTableFixture(jdbc)
    private val notes = NoteTableFixture(jdbc)

    private lateinit var testUser: User

    @BeforeEach
    fun setUp() {
        notes.deleteAll()
        users.deleteAll()
        testUser = User.create("author@example.com", "hash")
        userRepo.save(testUser)
    }

    private fun createNote(title: String = "My Note") = Note.create(
        authorId = testUser.id,
        language = NoteLanguage.EN,
        title = title,
        accessLevel = NoteAccessLevel.PUBLIC,
        status = NoteStatus.PUBLISHED,
    )

    @Nested
    inner class FindById {

        @Test
        fun `should return note when it is saved`() {
            val note = createNote()
            repo.save(note)

            val found = repo.findById(note.id)

            assertSoftly {
                found shouldNotBe null
                found?.id shouldBe note.id
                found?.title shouldBe "My Note"
                found?.authorId shouldBe testUser.id
            }
        }

        @Test
        fun `should return null when note does not exist`() {
            repo.findById(NoteId.generate()) shouldBe null
        }
    }

    @Nested
    inner class FindByTitle {

        @Test
        fun `should return note when title matches`() {
            repo.save(createNote(title = "Kotlin Guide"))

            val found = repo.findByTitle("Kotlin Guide")

            found?.title shouldBe "Kotlin Guide"
        }

        @Test
        fun `should return null when title does not match`() {
            repo.findByTitle("Nonexistent") shouldBe null
        }
    }

    @Nested
    inner class FindAllByLanguage {

        @Test
        fun `should return all notes for the given language`() {
            repo.save(createNote(title = "Apple"))
            repo.save(createNote(title = "Banana"))

            val found = repo.findAllByLanguage(NoteLanguage.EN)

            found.map { it.title } shouldBe listOf("Apple", "Banana")
        }

        @Test
        fun `should return empty list when no notes for the language`() {
            repo.save(createNote(title = "My Note"))

            val found = repo.findAllByLanguage(NoteLanguage.KO)

            found shouldBe emptyList()
        }
    }

    @Nested
    inner class ExistsByTitle {

        @Test
        fun `should return true when title exists`() {
            repo.save(createNote(title = "Existing"))

            repo.existsByTitle("Existing") shouldBe true
        }

        @Test
        fun `should return false when title does not exist`() {
            repo.existsByTitle("Missing") shouldBe false
        }
    }

    @Nested
    inner class Save {

        @Test
        fun `should insert a new note`() {
            val note = createNote()
            repo.save(note)

            notes.countNotes() shouldBe 1
        }

        @Test
        fun `should update an existing note`() {
            val note = createNote()
            repo.save(note)
            val persisted = repo.findById(note.id)!!

            val mutator = persisted.mutate()
            mutator.currentVersion = 1
            repo.save(mutator)

            repo.findById(note.id)?.currentVersion shouldBe 1
        }

        @Test
        fun `should persist soft-delete fields`() {
            val note = createNote()
            repo.save(note)
            val persisted = repo.findById(note.id)!!

            val mutator = persisted.mutate()
            mutator.softDeletedBy = testUser.id
            mutator.softDeletedAt = Instant.now()
            repo.save(mutator)

            val updated = repo.findById(note.id)!!
            assertSoftly {
                updated.softDeletedBy shouldBe testUser.id
                updated.softDeletedAt shouldNotBe null
                updated.isSoftDeleted shouldBe true
            }
        }

        @Test
        fun `should insert a new note with soft-delete fields preset`() {
            val note = Note.create(
                authorId = testUser.id,
                language = NoteLanguage.EN,
                title = "Pre-deleted Note",
                accessLevel = NoteAccessLevel.PUBLIC,
                status = NoteStatus.PUBLISHED,
            )
            val mutator = note.mutate()
            mutator.softDeletedBy = testUser.id
            mutator.softDeletedAt = Instant.now()

            repo.save(mutator)

            val found = repo.findById(note.id)!!
            assertSoftly {
                found.softDeletedBy shouldBe testUser.id
                found.softDeletedAt shouldNotBe null
            }
        }
    }

    companion object {
        private val TEST_AES_CIPHER = AesGcmCipher(ByteArray(32) { it.toByte() })
        private val TEST_HMAC_INDEX = HmacBlindIndex(ByteArray(32) { (it + 32).toByte() })
    }
}
