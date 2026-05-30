/*
 * NoteAccessLevel.kt
 *
 * $Since: 2026-05-30T00:00:00Z
 */
package com.fj.omnimemo.core.note.model

/**
 * Controls who can read a note.
 *
 * PUBLIC     — visible to anyone, including anonymous visitors.
 * RESTRICTED — visible only to authenticated users.
 * PRIVATE    — visible only to the author and explicit contributors.
 *
 * @author Francesco Jo
 * @since 0.2.0
 * @version 0.2.0
 */
enum class NoteAccessLevel { PUBLIC, RESTRICTED, PRIVATE }
