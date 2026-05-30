/*
 * MockUserProfileCache.kt
 *
 * $Since: 2026-05-30T00:00:00Z
 */
package com.fj.omnimemo.core.user

import com.fj.omnimemo.core.user.model.UserId
import com.fj.omnimemo.core.user.model.UserProfile

/**
 * In-memory fake of [UserProfileCache] for use in Small tests.
 *
 * Tracks each [invalidate] call in [invalidatedIds] so tests can assert that
 * cache evictions occurred in the expected order.
 *
 * @author Francesco Jo
 * @since 0.1.1
 * @version 0.1.1
 */
class MockUserProfileCache : UserProfileCache {

    private val store = mutableMapOf<UserId, UserProfile>()
    val invalidatedIds = mutableListOf<UserId>()

    fun put(profile: UserProfile) {
        store[profile.id] = profile
    }

    fun clear() {
        store.clear()
        invalidatedIds.clear()
    }

    override fun get(id: UserId): UserProfile? = store[id]

    override fun invalidate(id: UserId) {
        store.remove(id)
        invalidatedIds.add(id)
    }
}
