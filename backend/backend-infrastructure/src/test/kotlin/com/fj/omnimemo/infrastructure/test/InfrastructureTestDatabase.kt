/*
 * InfrastructureTestDatabase.kt
 *
 * $Since: 2026-05-26T00:00:00Z
 */
package com.fj.omnimemo.infrastructure.test

import com.fj.omnimemo.infrastructure.test.InfrastructureTestDatabase.CHANGELOG
import liquibase.Contexts
import liquibase.GlobalConfiguration
import liquibase.Liquibase
import liquibase.Scope
import liquibase.database.DatabaseFactory
import liquibase.database.jvm.JdbcConnection
import liquibase.resource.ClassLoaderResourceAccessor
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.datasource.DriverManagerDataSource
import org.testcontainers.containers.PostgreSQLContainer

/**
 * Shared PostgreSQL container for medium tests in the backend-infrastructure module.
 *
 * The container is started once on first access and terminated by Testcontainers'
 * Ryuk sidecar when the JVM exits. The schema is applied via Liquibase using
 * [CHANGELOG], so new migrations are picked up automatically without touching
 * this class. Each test class must delete its own rows in `@BeforeEach` to
 * maintain isolation between tests.
 *
 * @author Francesco Jo
 * @since 0.1.1
 * @version 0.1.1
 */
object InfrastructureTestDatabase {

    private const val CHANGELOG = "db/changelog/db.changelog-master.yaml"

    val container: PostgreSQLContainer<*> = PostgresContainerSupport.newContainer().also { it.start() }

    val jdbc: JdbcTemplate

    init {
        val ds = DriverManagerDataSource(container.jdbcUrl, container.username, container.password)
        jdbc = JdbcTemplate(ds)
        applySchema(ds)
    }

    private fun applySchema(ds: DriverManagerDataSource) {
        // The Gradle test classpath exposes the changelog both as a directory entry (build/resources/main)
        // and inside the testFixtures JAR, so ClassLoaderResourceAccessor finds two copies.
        // WARN mode makes Liquibase pick the first one and continue rather than throwing.
        val scopeAttrs = mapOf(
            GlobalConfiguration.DUPLICATE_FILE_MODE.key to GlobalConfiguration.DuplicateFileMode.WARN,
        )
        try {
            Scope.child(scopeAttrs) {
                ds.connection.use { conn ->
                    val database = DatabaseFactory.getInstance()
                        .findCorrectDatabaseImplementation(JdbcConnection(conn))
                    Liquibase(CHANGELOG, ClassLoaderResourceAccessor(), database).use { it.update(Contexts()) }
                }
            }
        } catch (e: Exception) {
            throw IllegalStateException("Failed to apply schema via $CHANGELOG", e)
        }
    }
}
