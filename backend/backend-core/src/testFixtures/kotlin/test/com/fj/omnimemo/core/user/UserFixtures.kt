/*
 * UserFixtures.kt
 *
 * $Since: 2026-05-30T00:00:00Z
 */
package test.com.fj.omnimemo.core.user

import com.fj.omnimemo.core.user.model.User
import com.fj.omnimemo.core.user.model.UserId
import com.fj.omnimemo.core.user.security.PasswordHasher
import net.datafaker.Faker

private val faker = Faker()

/**
 * Returns a realistic random e-mail address unique enough for test isolation.
 *
 * @author Francesco Jo
 * @since 0.2.0
 */
fun randomEmail(): String = faker.internet().emailAddress()

/**
 * Builds a new (unpersisted) [User] with a realistic random e-mail.
 *
 * [hasher] defaults to a no-op that prefixes "hashed:" so tests never
 * run a real bcrypt operation unless explicitly required.
 *
 * @author Francesco Jo
 * @since 0.2.0
 */
fun randomUser(
    email: String = randomEmail(),
    rawPassword: String = faker.internet().password(),
    hasher: PasswordHasher = object : PasswordHasher {
        override fun hash(rawPassword: String) = "hashed:$rawPassword"
        override fun matches(rawPassword: String, hash: String) = hash == "hashed:$rawPassword"
    },
): User = User.create(email = email, passwordHash = hasher.hash(rawPassword))

/** Returns a new random [UserId]. */
fun randomUserId(): UserId = UserId.generate()
