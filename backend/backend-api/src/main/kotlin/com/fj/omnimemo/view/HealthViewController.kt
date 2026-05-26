/*
 * HealthViewController.kt
 *
 * $Since: 2026-05-20T00:00:00Z
 */
package com.fj.omnimemo.view

import com.fj.omnimemo.core.status.model.HealthStatus
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.servlet.ModelAndView
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * View endpoint for health status; returns an HTMX-compatible HTML fragment via a Thymeleaf template.
 *
 * @author Francesco Jo
 * @since 0.0.1
 * @version 0.1.1
 */
@Controller
class HealthViewController {

    private val formatter = DateTimeFormatter
        .ofPattern("yyyy-MM-dd HH:mm:ss")
        .withZone(ZoneId.systemDefault())

    /**
     * Returns an HTML fragment showing the current health status and timestamp.
     *
     * @return ModelAndView resolving to the status fragment in the health template
     * @since 0.0.1
     * @version 0.1.1
     */
    @GetMapping("/health")
    fun health(): ModelAndView {
        val status = HealthStatus()
        val time = formatter.format(Instant.ofEpochMilli(status.timestamp))
        return ModelAndView("health :: status", mapOf("status" to status.status, "time" to time))
    }
}
