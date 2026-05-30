/*
 * NoteAction.kt
 *
 * $Since: 2026-05-30T00:00:00Z
 */
package com.fj.omnimemo.core.note.model

/**
 * Classifies the actor's intent recorded in a [NoteAudit] entry.
 *
 * @author Francesco Jo
 * @since 0.2.0
 * @version 0.2.0
 */
enum class NoteAction { CREATE, EDIT, ROLLBACK, DELETE, RESTORE }
