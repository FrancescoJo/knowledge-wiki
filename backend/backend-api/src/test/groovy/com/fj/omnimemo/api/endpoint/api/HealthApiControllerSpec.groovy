/*
 * HealthControllerSpec.groovy
 *
 * $Since: 2026-05-20T00:00:00Z
 */
package com.fj.omnimemo.api.endpoint.api

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.client.TestRestTemplate
import spock.lang.Specification

/**
 * Verifies that the health endpoint operates correctly with a fully loaded Spring context.
 *
 * @author Francesco Jo
 * @since 0.1.1
 * @version 0.1.1
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class HealthApiControllerSpec extends Specification {

    @Autowired
    TestRestTemplate restTemplate

    /**
     * Asserts that the health endpoint is reachable and returns a successful HTTP response.
     *
     * @since 0.1.1
     * @version 0.1.1
     */
    def "GET /api/health should return HTTP 200 when the application is running"() {
        when:
        def response = restTemplate.getForEntity("/api/health", String)

        then:
        response.statusCode.value() == 200
    }
}
