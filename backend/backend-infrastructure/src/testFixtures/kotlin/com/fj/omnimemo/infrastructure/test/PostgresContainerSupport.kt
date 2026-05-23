/*
 * PostgresContainerSupport.kt
 *
 * $Since: 2026-05-23T00:00:00Z
 */
package com.fj.omnimemo.infrastructure.test

import org.testcontainers.containers.PostgreSQLContainer

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
