/*
 * HealthControllerImpl.kt
 *
 * $Since: 2026-05-26T00:00:00Z
 */
package com.fj.omnimemo.api.endpoint.health.impl

import com.fj.omnimemo.api.endpoint.health.HealthController
import com.fj.omnimemo.core.status.model.HealthStatus
import org.springframework.web.bind.annotation.RestController

/**
 * @author Francesco Jo
 * @since 0.1.1
 * @version 0.1.1
 */
@RestController
internal class HealthControllerImpl : HealthController {
    override fun health(): HealthStatus = HealthStatus()
}
