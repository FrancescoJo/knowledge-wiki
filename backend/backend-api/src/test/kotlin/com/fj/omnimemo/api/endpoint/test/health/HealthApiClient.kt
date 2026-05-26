/*
 * HealthApiClient.kt
 *
 * $Since: 2026-05-26T00:00:00Z
 */
package com.fj.omnimemo.api.endpoint.test.health

import com.fj.omnimemo.api.endpoint.ApiPathsV1
import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.http.ResponseEntity

/**
 * Large Test helper that wraps [TestRestTemplate] calls for the health endpoint.
 *
 * @author Francesco Jo
 * @since 0.1.1
 * @version 0.1.1
 */
class HealthApiClient(private val restTemplate: TestRestTemplate) {

    fun health(): ResponseEntity<String> =
        restTemplate.getForEntity(ApiPathsV1.HEALTH, String::class.java)
}
