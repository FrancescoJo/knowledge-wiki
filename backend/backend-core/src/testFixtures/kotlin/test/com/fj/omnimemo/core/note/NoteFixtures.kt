/*
 * NoteFixtures.kt
 *
 * $Since: 2026-05-30T00:00:00Z
 */
package test.com.fj.omnimemo.core.note

import com.fj.omnimemo.core.note.model.Note
import com.fj.omnimemo.core.note.model.NoteAccessLevel
import com.fj.omnimemo.core.note.model.NoteLanguage
import com.fj.omnimemo.core.note.model.NoteStatus
import com.fj.omnimemo.core.user.model.UserId
import net.datafaker.Faker

private val faker = Faker()

/**
 * Builds a new (unpersisted) [Note] using realistic random field values.
 *
 * All parameters are optional; defaults are chosen to produce a valid,
 * publicly-visible, published English note.
 *
 * @author Francesco Jo
 * @since 0.2.0
 */
fun randomNote(
    authorId: UserId = UserId.generate(),
    language: NoteLanguage = NoteLanguage.EN,
    title: String = faker.book().title(),
    accessLevel: NoteAccessLevel = NoteAccessLevel.PUBLIC,
    status: NoteStatus = NoteStatus.PUBLISHED,
): Note = Note.create(
    authorId = authorId,
    language = language,
    title = title,
    accessLevel = accessLevel,
    status = status,
)

/** Returns a realistic random sentence suitable for use as note content. */
fun randomNoteContent(): String = faker.lorem().sentence(10)
