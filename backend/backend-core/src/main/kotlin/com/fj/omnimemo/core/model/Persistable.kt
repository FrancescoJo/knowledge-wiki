/*
 * Persistable.kt
 *
 * $Since: 2026-05-23T00:00:00Z
 */
package com.fj.omnimemo.core.model

/**
 * Marks an entity that can be persisted to a data store.
 *
 * Identity is assigned by the domain at creation time via the entity's
 * [create][companion object's create factory] factory method, not by the
 * persistence layer. This keeps entities complete and fully testable before
 * any infrastructure interaction occurs.
 *
 * [isNew] distinguishes a freshly created entity from one restored from
 * the data store via its [reconstitute] factory. Repositories use this flag
 * to perform an unconditional INSERT for new entities without inspecting the ID.
 *
 * Design rationale: ID generation was deliberately placed in the domain
 * layer (not infrastructure) for the following reasons:
 *   - UUID v7 is purely algorithmic — no network or database access is needed.
 *   - Domain entities are complete at creation; no null-safety workarounds needed.
 *   - Domain events can carry the correct ID before the persistence commit.
 *   - Repository implementations are simplified: new entity ↔ INSERT, always.
 *
 * @author Francesco Jo
 * @since 0.1.1
 * @version 0.1.1
 */
interface Persistable<ID : Any> {
    val id: ID
    val isNew: Boolean
    val isPersisted: Boolean get() = !isNew
}
