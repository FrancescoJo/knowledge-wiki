/*
 * UserMutator.kt
 *
 * $Since: 2026-05-23T00:00:00Z
 */
package com.fj.omnimemo.core.user.model.snapshot

import com.fj.omnimemo.core.user.model.User
import com.fj.omnimemo.core.user.model.UserId
import java.time.Instant
import kotlin.properties.Delegates

// Not a data class: Delegates.observable requires a regular class because
// data class does not support backing an interface-overriding property with
// a delegate. equals / hashCode / toString delegate to UserData snapshot.
// See coding guide § Mutator Pattern — Exception: property-change side effects.
/**
 * Mutable implementation of [User]. Obtain via [mutate]; discard after use.
 *
 * Any assignment to [email] or [passwordHash] automatically advances
 * [updatedAt] to the current instant via [Delegates.observable].
 *
 * @author Francesco Jo
 * @since 0.1.1
 * @version 0.1.1
 */
internal class UserMutator(
    override val id: UserId,
    override val isNew: Boolean,
    email: String,
    passwordHash: String,
    override val createdAt: Instant,
    override var updatedAt: Instant,
) : User.Mutator {
    override var email: String by Delegates.observable(email) { _, _, _ ->
        this.updatedAt = Instant.now()
    }

    override var passwordHash: String by Delegates.observable(passwordHash) { _, _, _ ->
        this.updatedAt = Instant.now()
    }

    private fun snapshot() = UserData(id, isNew, email, passwordHash, createdAt, updatedAt)

    override fun equals(other: Any?) = other is UserMutator && snapshot() == other.snapshot()
    override fun hashCode() = snapshot().hashCode()
    override fun toString() = snapshot().toString()
}
