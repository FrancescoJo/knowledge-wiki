/*
 * User.kt
 *
 * $Since: 2026-05-23T00:00:00Z
 */
package com.fj.omnimemo.core.user.model

import com.fj.omnimemo.core.data.model.DateTimeAuditable
import com.fj.omnimemo.core.data.model.Persistable
import com.fj.omnimemo.core.user.model.snapshot.UserData
import java.time.Instant

/**
 * Represents an authenticated user of the system.
 *
 * The domain always operates on plaintext [email]; encryption is the sole
 * responsibility of the infrastructure layer. [passwordHash] holds the
 * bcrypt-hashed password — the raw password never passes through this type.
 *
 * Use [create] to instantiate a new user and [reconstitute] to restore one
 * from a persisted record.
 *
 * @author Francesco Jo
 * @since 0.1.1
 * @version 0.1.1
 */
interface User : Persistable<UserId>, DateTimeAuditable {
    override val id: UserId
    override val isNew: Boolean
    val email: String
    val passwordHash: String

    /**
     * Grants write access to mutable business fields. Obtain via [mutate].
     *
     * Any assignment to [email] or [passwordHash] automatically advances
     * [updatedAt] to the current instant. [isNew] and [createdAt] are
     * intentionally excluded — they are not business-mutable fields.
     *
     * Must not be shared across threads. Treat as a short-lived scratchpad:
     * obtain, apply changes, then pass the result onward as [User].
     *
     * @since 0.1.1
     * @version 0.1.1
     */
    interface Mutator : User, DateTimeAuditable.Mutator {
        override var email: String
        override var passwordHash: String
    }

    companion object {
        /**
         * Creates a new, unpersisted [User]. A UUID v7 identity is generated
         * by the domain at this point; no persistence interaction is required.
         *
         * @since 0.1.1
         */
        fun create(
            email: String,
            passwordHash: String
        ): User {
            val now = Instant.now()
            return UserData(
                id = UserId.generate(),
                isNew = true,
                email = email,
                passwordHash = passwordHash,
                createdAt = now,
                updatedAt = now,
            )
        }

        /**
         * Restores a [User] from a persisted record. [isNew] is false.
         *
         * @since 0.1.1
         */
        fun reconstitute(
            id: UserId,
            email: String,
            passwordHash: String,
            createdAt: Instant,
            updatedAt: Instant,
        ): User = UserData(
            id = id,
            isNew = false,
            email = email,
            passwordHash = passwordHash,
            createdAt = createdAt,
            updatedAt = updatedAt,
        )
    }
}
