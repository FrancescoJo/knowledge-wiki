/*
 * UserProfileCache.kt
 *
 * $Since: 2026-05-30T00:00:00Z
 */
package com.fj.omnimemo.core.user

import com.fj.omnimemo.core.user.model.UserId
import com.fj.omnimemo.core.user.model.UserProfile

/**
 * Read-through cache for user display profiles.
 *
 * Implementations are responsible for loading missing entries from the backing
 * store on a cache miss. Callers must call [invalidate] whenever a user's
 * display-relevant fields are mutated or the account is deleted.
 *
 * The interface is intentionally minimal so that the backing store can be
 * swapped (e.g., in-memory to Redis) without touching call sites.
 *
 * @author Francesco Jo
 * @since 0.1.1
 * @version 0.1.1
 */
interface UserProfileCache {

    /**
     * Returns the [UserProfile] for [id], loading from the backing store on a cache miss.
     * Returns `null` when no user exists for [id].
     */
    fun get(id: UserId): UserProfile?

    /**
     * Evicts the entry for [id] if present.
     */
    fun invalidate(id: UserId)
}
