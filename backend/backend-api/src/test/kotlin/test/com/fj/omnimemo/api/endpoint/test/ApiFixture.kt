/*
 * ApiFixture.kt
 *
 * $Since: 2026-05-26T00:00:00Z
 */
package test.com.fj.omnimemo.api.endpoint.test

import com.fj.omnimemo.core.user.model.User
import com.fj.omnimemo.core.user.usecase.CreateUserUseCase
import org.springframework.jdbc.core.JdbcTemplate
import test.com.fj.omnimemo.infrastructure.user.persistence.UserTableFixture

/**
 * Large Test fixture that resets the database to a known state and creates a test user.
 *
 * Combines [UserTableFixture.deleteAll] with [CreateUserUseCase.create] so that each
 * spec's `setup` block can delegate its entire database preparation to a single call.
 *
 * @author Francesco Jo
 * @since 0.1.1
 * @version 0.1.1
 */
class ApiFixture(
    private val createUserUseCase: CreateUserUseCase,
    jdbc: JdbcTemplate,
) {
    private val userTableFixture = UserTableFixture(jdbc)

    fun resetWithUser(email: String, password: String): User {
        userTableFixture.deleteAll()
        return createUserUseCase.create(email, password)
    }
}
