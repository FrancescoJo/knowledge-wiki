/*
 * HealthApiController.kt
 *
 * $Since: 2026-05-20T00:00:00Z
 */
package com.fj.omnimemo.api.endpoint.api

import com.fj.omnimemo.core.model.HealthStatus
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/**
 * REST endpoint for health-check queries.
 *
 * @author Francesco Jo
 * @since 0.0.1
 * @version 0.1.1
 */
@RestController
@RequestMapping("/api")
class HealthApiController {

    /**
     * Returns the current health status of the application.
     *
     * @return current [HealthStatus]
     * @since 0.0.1
     * @version 0.1.1
     */
    @GetMapping("/health")
    fun health(): HealthStatus {
        return HealthStatus()
    }
}
