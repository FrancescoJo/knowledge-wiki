/*
 * UserExtensions.kt
 *
 * $Since: 2026-05-23T00:00:00Z
 */
package com.fj.omnimemo.core.model.user

/**
 * Returns a [User.Mutator] pre-populated from this instance.
 *
 * Any assignment to [User.Mutator.email] or [User.Mutator.passwordHash]
 * automatically advances [User.Mutator.updatedAt] to the current instant.
 *
 * @since 0.1.1
 */
fun User.mutate(): User.Mutator = UserMutator(
    id = id,
    isNew = isNew,
    email = email,
    passwordHash = passwordHash,
    createdAt = createdAt,
    updatedAt = updatedAt,
)
