/*
 * MockNoteRepository.kt
 *
 * $Since: 2026-05-30T00:00:00Z
 */
package test.com.fj.omnimemo.core.note.repository

import com.fj.omnimemo.core.note.model.Note
import com.fj.omnimemo.core.note.model.NoteId
import com.fj.omnimemo.core.note.model.NoteLanguage
import com.fj.omnimemo.core.note.repository.NoteRepository
import test.com.fj.omnimemo.core.repository.AbstractMockRepository

/**
 * In-memory fake of [NoteRepository] for use in Small tests.
 *
 * @author Francesco Jo
 * @since 0.2.0
 * @version 0.2.0
 */
class MockNoteRepository : AbstractMockRepository<NoteId, Note>({ it.id }), NoteRepository {
    override fun findById(id: NoteId): Note? = doFindByKey(id)

    override fun findByTitle(title: String): Note? = store.values.find { it.title == title }

    override fun findAllByLanguage(language: NoteLanguage): List<Note> =
        store.values.filter { it.language == language }

    override fun existsByTitle(title: String): Boolean = store.values.any { it.title == title }

    override fun save(note: Note): Note = doSave(note)
}
