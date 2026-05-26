package com.fj.omnimemo.core.status.model

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
