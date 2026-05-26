/*
 * OmnimemoApplication.kt
 *
 * $Since: 2026-05-20T00:00:00Z
 */
package com.fj.omnimemo.api

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

/**
 * Spring Boot application entry point for the Omnimemo service.
 *
 * @author Francesco Jo
 * @since 0.0.1
 * @version 0.1.1
 */
@SpringBootApplication(scanBasePackages = ["com.fj.omnimemo"])
class OmnimemoApplication

/**
 * Boots the Spring application context.
 *
 * @param args command-line arguments passed to the application
 * @since 0.0.1
 * @version 0.1.1
 */
fun main(args: Array<String>) {
    // Spring Boot's runApplication<T>(*args) is idiomatic and unavoidable; the one-time
    // startup copy is not a meaningful performance concern.
    @Suppress("SpreadOperator")
    runApplication<OmnimemoApplication>(*args)
}
