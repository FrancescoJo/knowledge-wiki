/*
 * NoteStatus.kt
 *
 * $Since: 2026-05-30T00:00:00Z
 */
package com.fj.omnimemo.core.note.model

/**
 * Publication state of a note.
 *
 * DRAFT     — work-in-progress; visible only to the author.
 * PUBLISHED — live; visibility is further controlled by [NoteAccessLevel].
 *
 * @author Francesco Jo
 * @since 0.2.0
 * @version 0.2.0
 */
enum class NoteStatus { DRAFT, PUBLISHED }
