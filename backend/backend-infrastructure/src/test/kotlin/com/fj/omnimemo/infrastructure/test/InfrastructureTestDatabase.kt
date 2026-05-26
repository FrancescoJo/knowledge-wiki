/*
 * InfrastructureTestDatabase.kt
 *
 * $Since: 2026-05-26T00:00:00Z
 */
package com.fj.omnimemo.infrastructure.test

import liquibase.Contexts
import liquibase.Liquibase
import liquibase.database.DatabaseFactory
import liquibase.database.jvm.JdbcConnection
import liquibase.resource.DirectoryResourceAccessor
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.datasource.DriverManagerDataSource
import org.testcontainers.containers.PostgreSQLContainer
import java.nio.file.Paths

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
        // ClassLoaderResourceAccessor scans the entire classpath and finds duplicate copies
        // of the changelog file (once in main resources, once in the testFixtures JAR).
        // Resolving the URL directly from the classloader gives a single filesystem path;
        // DirectoryResourceAccessor then restricts Liquibase's search to that directory tree.
        val changelogUrl = checkNotNull(
            InfrastructureTestDatabase::class.java.classLoader.getResource(CHANGELOG)
        ) { "$CHANGELOG not found on classpath" }
        val resourcesRoot = CHANGELOG.split("/")
            .fold(Paths.get(changelogUrl.toURI())) { path, _ -> path.parent }

        ds.connection.use { conn ->
            val database = DatabaseFactory.getInstance()
                .findCorrectDatabaseImplementation(JdbcConnection(conn))
            Liquibase(CHANGELOG, DirectoryResourceAccessor(resourcesRoot), database).use {
                it.update(Contexts())
            }
        }
    }
}
