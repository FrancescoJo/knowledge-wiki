/*
 * UserControllerSpec.groovy
 *
 * $Since: 2026-05-26T00:00:00Z
 */
package com.fj.omnimemo.api.endpoint.user

import com.fj.omnimemo.api.endpoint.test.ApiFixture
import com.fj.omnimemo.api.endpoint.test.auth.AuthApiClient
import com.fj.omnimemo.api.endpoint.test.user.UserApiClient
import com.fj.omnimemo.core.test.annotation.LargeTest
import com.fj.omnimemo.core.user.usecase.CreateUserUseCase
import com.fj.omnimemo.infrastructure.test.PostgresContainerSupport
import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.context.ApplicationContextInitializer
import org.springframework.context.ConfigurableApplicationContext
import org.springframework.core.env.MapPropertySource
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.test.context.ContextConfiguration
import org.testcontainers.containers.PostgreSQLContainer
import spock.lang.Specification

/**
 * Smoke tests for the user API: verifies that read and write operations work
 * end-to-end with a fully loaded Spring context, a real database, and JWT
 * authentication enforced by the security filter chain.
 *
 * Setup for each test:
 *   1. Database is reset and a fresh test user is created via [ApiFixture].
 *   2. The test user logs in via [AuthApiClient] to obtain an access token.
 *   3. The access token is carried on all subsequent requests via [UserApiClient].
 *
 * @author Francesco Jo
 * @since 0.1.1
 * @version 0.1.1
 */
@LargeTest
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ContextConfiguration(initializers = [ContainerInitializer])
class UserControllerSpec extends Specification {

    static PostgreSQLContainer container = PostgresContainerSupport.newContainer()

    static class ContainerInitializer implements ApplicationContextInitializer<ConfigurableApplicationContext> {
        @Override
        void initialize(ConfigurableApplicationContext ctx) {
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

    @Autowired TestRestTemplate restTemplate
    @Autowired CreateUserUseCase createUserUseCase
    @Autowired JdbcTemplate jdbcTemplate
    @Autowired ObjectMapper objectMapper

    private static final String TEST_EMAIL    = "user-smoke@example.com"
    private static final String TEST_PASSWORD = "UserSmoke1!"

    AuthApiClient authApiClient
    UserApiClient userApiClient
    ApiFixture apiFixture

    String accessToken
    String testUserId

    def setup() {
        authApiClient = new AuthApiClient(restTemplate)
        userApiClient = new UserApiClient(restTemplate, objectMapper)
        apiFixture    = new ApiFixture(createUserUseCase, jdbcTemplate)

        def user  = apiFixture.resetWithUser(TEST_EMAIL, TEST_PASSWORD)
        testUserId  = user.id.value.toString()
        accessToken = authApiClient.loginAndGetAccessToken(TEST_EMAIL, TEST_PASSWORD)
    }

    def "GET users by id returns 200 with the user's details"() {
        when:
        def response = userApiClient.findById(testUserId, accessToken)

        then:
        response.statusCode.value() == 200
        response.body.contains(TEST_EMAIL)
    }

    def "POST users returns 201 and creates a new user"() {
        given:
        def newEmail = "created-smoke@example.com"

        when:
        def response = userApiClient.create(newEmail, "NewSmoke1!", accessToken)

        then:
        response.statusCode.value() == 201
        response.body.contains(newEmail)
    }

    def "DELETE users by id returns 204"() {
        when:
        def response = userApiClient.delete(testUserId, accessToken)

        then:
        response.statusCode.value() == 204
    }
}
