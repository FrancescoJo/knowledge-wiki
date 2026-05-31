/*
 * HealthController.kt
 *
 * $Since: 2026-05-26T00:00:00Z
 */
package com.fj.omnimemo.api.endpoint.health

import com.fj.omnimemo.api.endpoint.ApiPathsV1
import com.fj.omnimemo.core.status.model.HealthStatus
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping

/**
 * REST API contract for health-check queries.
 *
 * Implementations live in the `impl` sub-package.
 *
 * @author Francesco Jo
 * @since 0.1.1
 * @version 0.1.1
 */
@RequestMapping(produces = [MediaType.APPLICATION_JSON_VALUE])
@Tag(name = "Health", description = "Application health check")
interface HealthController {
    @Operation(
        summary = "Health check",
        responses = [
            ApiResponse(
                responseCode = "200", description = "Application is healthy",
                content = [Content(schema = Schema(ref = "#/components/schemas/v1.health.HealthStatus"))],
            ),
        ],
    )
    @GetMapping(ApiPathsV1.HEALTH)
    fun health(): HealthStatus
}
