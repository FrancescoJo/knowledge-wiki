/*
 * HealthStatus.kt
 *
 * $Since: 2026-05-20T00:00:00Z
 */
package com.fj.omnimemo.core.model

/**
 * Represents the current health state of the application at a point in time.
 *
 * @author Francesco Jo
 * @since 0.0.1
 * @version 0.1.1
 */
data class HealthStatus(
    val status: String = "UP",
    val timestamp: Long = System.currentTimeMillis()
)
