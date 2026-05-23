/*
 * HealthApiControllerSpec.groovy
 *
 * $Since: 2026-05-20T00:00:00Z
 */
package com.fj.omnimemo.api.endpoint.api

import com.fj.omnimemo.infrastructure.test.PostgresContainerSupport
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.context.ApplicationContextInitializer
import org.springframework.context.ConfigurableApplicationContext
import org.springframework.core.env.MapPropertySource
import org.springframework.test.context.ContextConfiguration
import org.testcontainers.containers.PostgreSQLContainer
import spock.lang.Specification

/**
 * Verifies that the health endpoint operates correctly with a fully loaded Spring context.
 *
 * A Testcontainers PostgreSQL instance is started via [ContainerInitializer] before the
 * Spring context loads so that Liquibase migrations succeed against the container database.
 *
 * @author Francesco Jo
 * @since 0.1.1
 * @version 0.1.1
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ContextConfiguration(initializers = [HealthApiControllerSpec.ContainerInitializer])
class HealthApiControllerSpec extends Specification {

    static PostgreSQLContainer container = PostgresContainerSupport.newContainer()

    static class ContainerInitializer implements ApplicationContextInitializer<ConfigurableApplicationContext> {
        @Override
        void initialize(ConfigurableApplicationContext ctx) {
            // testFixtures pulls in backend-infrastructure's main JAR alongside its
            // classes+resources directory, causing Liquibase to find the changelog in
            // two locations. WARN mode picks the first occurrence (both are identical).
            System.setProperty("liquibase.duplicateFileMode", "WARN")
            container.start()
            def source = new MapPropertySource("testcontainers", [
                "spring.datasource.url"     : container.jdbcUrl,
                "spring.datasource.username": container.username,
                "spring.datasource.password": container.password,
            ])
            ctx.environment.propertySources.addFirst(source)
        }
    }

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
