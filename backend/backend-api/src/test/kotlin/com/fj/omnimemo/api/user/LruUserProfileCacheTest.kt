/*
 * LruUserProfileCacheTest.kt
 *
 * $Since: 2026-05-30T00:00:00Z
 */
package com.fj.omnimemo.api.user

import com.fj.omnimemo.core.test.annotation.SmallTest
import com.fj.omnimemo.core.user.model.User
import com.fj.omnimemo.core.user.model.UserId
import com.fj.omnimemo.core.user.repository.MockUserRepository
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.util.*

/**
 * Small tests for [LruUserProfileCache].
 *
 * @author Francesco Jo
 * @since 0.1.1
 */
@SmallTest
class LruUserProfileCacheTest {

    private val repo = MockUserRepository()
    private val cache = LruUserProfileCache(repo)

    @BeforeEach
    fun setUp() {
        repo.clear()
    }

    @Nested
    inner class Get {

        @Test
        fun `should load profile from repository on cache miss`() {
            val user = repo.save(User.create("alice@example.com", "hash"))

            val profile = cache.get(user.id)

            profile shouldNotBe null
            profile!!.email shouldBe "alice@example.com"
            profile.id shouldBe user.id
        }

        @Test
        fun `should return cached profile without hitting repository on cache hit`() {
            val user = repo.save(User.create("alice@example.com", "hash"))
            cache.get(user.id)
            repo.clear()

            val profile = cache.get(user.id)

            profile shouldNotBe null
            profile!!.email shouldBe "alice@example.com"
        }

        @Test
        fun `should return null when user does not exist`() {
            cache.get(UserId(UUID.randomUUID())) shouldBe null
        }

        @Test
        fun `should return null after cache is invalidated`() {
            val user = repo.save(User.create("alice@example.com", "hash"))
            cache.get(user.id)
            cache.invalidate(user.id)
            repo.clear()

            cache.get(user.id) shouldBe null
        }
    }

    @Nested
    inner class LruEviction {

        @Test
        fun `should evict least recently used entry when capacity is exceeded`() {
            val smallCache = LruUserProfileCache(repo, maxSize = 2)
            val userA = repo.save(User.create("a@example.com", "hash"))
            val userB = repo.save(User.create("b@example.com", "hash"))
            val userC = repo.save(User.create("c@example.com", "hash"))

            smallCache.get(userA.id)
            smallCache.get(userB.id)
            smallCache.get(userA.id)
            smallCache.get(userC.id)

            // B was the least recently used at the point C was added; clear repo to distinguish
            // cache hits from live loads.
            repo.clear()

            smallCache.get(userA.id) shouldNotBe null
            smallCache.get(userC.id) shouldNotBe null
            smallCache.get(userB.id) shouldBe null
        }
    }

    @Nested
    inner class Invalidate {

        @Test
        fun `should remove entry from cache`() {
            val user = repo.save(User.create("alice@example.com", "hash"))
            cache.get(user.id)

            cache.invalidate(user.id)

            repo.delete(user.id)
            cache.get(user.id) shouldBe null
        }

        @Test
        fun `should be a no-op for an id that is not cached`() {
            cache.invalidate(UserId(UUID.randomUUID()))
        }
    }
}
