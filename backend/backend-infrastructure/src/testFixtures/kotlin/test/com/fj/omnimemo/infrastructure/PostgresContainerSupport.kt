package test.com.fj.omnimemo.infrastructure

import org.testcontainers.containers.PostgreSQLContainer
import test.com.fj.omnimemo.infrastructure.PostgresContainerSupport.newContainer

/**
 * Factory for Testcontainers PostgreSQL instances used across test modules.
 *
 * Each call to [newContainer] creates an independent container so tests in
 * different modules do not share database state.
 *
 * @author Francesco Jo
 * @since 0.1.1
 * @version 0.1.1
 */
object PostgresContainerSupport {
    const val IMAGE = "postgres:16-alpine"

    @JvmStatic
    fun newContainer(): PostgreSQLContainer<*> = PostgreSQLContainer(IMAGE)
}
