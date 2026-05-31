/*
 * LruUserProfileCache.kt
 *
 * $Since: 2026-05-30T00:00:00Z
 */
package com.fj.omnimemo.api.user

import com.fj.omnimemo.core.user.UserProfileCache
import com.fj.omnimemo.core.user.model.UserId
import com.fj.omnimemo.core.user.model.UserProfile
import com.fj.omnimemo.core.user.repository.UserRepository
import java.util.*

/**
 * In-memory LRU cache of [UserProfile] entries, bounded to [maxSize] entries.
 *
 * Cache misses are satisfied synchronously by [userRepository]. The read-check-write
 * path is not atomic: two concurrent threads may both observe a miss for the same key
 * and issue duplicate loads. The last write wins and the outcome is always correct,
 * making this race benign for the expected user-base size.
 *
 * @author Francesco Jo
 * @since 0.1.1
 * @version 0.1.1
 */
class LruUserProfileCache(
    private val userRepository: UserRepository,
    private val maxSize: Int = DEFAULT_MAX_SIZE,
) : UserProfileCache {
    private val cache: MutableMap<UserId, UserProfile> = Collections.synchronizedMap(
        object : LinkedHashMap<UserId, UserProfile>(maxSize, 0.75f, true) {
            override fun removeEldestEntry(eldest: Map.Entry<UserId, UserProfile>): Boolean = size > maxSize
        }
    )

    override fun get(id: UserId): UserProfile? =
        cache[id] ?: userRepository.findById(id)
            ?.let { UserProfile(it.id, it.email) }
            ?.also { cache[id] = it }

    override fun invalidate(id: UserId) {
        cache.remove(id)
    }

    companion object {
        const val DEFAULT_MAX_SIZE = 250
    }
}
