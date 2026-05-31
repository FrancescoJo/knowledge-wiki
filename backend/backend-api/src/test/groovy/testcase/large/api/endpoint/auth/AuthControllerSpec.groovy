/*
 * AuthControllerSpec.groovy
 *
 * $Since: 2026-05-26T00:00:00Z
 */
package testcase.large.api.endpoint.auth

import com.fasterxml.jackson.databind.ObjectMapper
import com.fj.omnimemo.core.user.usecase.CreateUserUseCase
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
import test.com.fj.omnimemo.api.endpoint.test.ApiFixture
import test.com.fj.omnimemo.api.endpoint.test.CookieSupport
import test.com.fj.omnimemo.api.endpoint.test.auth.AuthApiClient
import test.com.fj.omnimemo.core.annotation.LargeTest
import test.com.fj.omnimemo.infrastructure.PostgresContainerSupport

/**
 * Smoke tests for the auth API: verifies that login, logout, and token refresh
 * work end-to-end with a fully loaded Spring context and a real database.
 *
 * Each test creates a fresh user via [ApiFixture] and uses [AuthApiClient] to
 * exercise the endpoints. CSRF tokens are managed transparently by [AuthApiClient].
 *
 * @author Francesco Jo
 * @since 0.1.1
 * @version 0.1.1
 */
@LargeTest
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ContextConfiguration(initializers = [ContainerInitializer])
class AuthControllerSpec extends Specification {
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

    @Autowired
    TestRestTemplate restTemplate
    @Autowired
    CreateUserUseCase createUserUseCase
    @Autowired
    JdbcTemplate jdbcTemplate
    @Autowired
    ObjectMapper objectMapper

    private static final String TEST_EMAIL = "auth-smoke@example.com"
    private static final String TEST_PASSWORD = "AuthSmoke1!"

    AuthApiClient authApiClient
    ApiFixture apiFixture

    def setup() {
        authApiClient = new AuthApiClient(restTemplate)
        apiFixture = new ApiFixture(createUserUseCase, jdbcTemplate)
        apiFixture.resetWithUser(TEST_EMAIL, TEST_PASSWORD)
    }

    def "POST auth/login returns 200 and sets access and refresh token cookies"() {
        when:
        def response = authApiClient.login(TEST_EMAIL, TEST_PASSWORD)
        def envelope = objectMapper.readValue(response.body, Map)

        then:
        response.statusCode.value() == 200
        CookieSupport.cookieValue(response, "access_token") != null
        CookieSupport.cookieValue(response, "refresh_token") != null
        envelope.body.accessToken != null
        envelope.body.refreshToken != null
    }

    def "POST auth/logout returns 200 and expires both token cookies"() {
        given:
        def loginResponse = authApiClient.login(TEST_EMAIL, TEST_PASSWORD)
        def accessToken = CookieSupport.cookieValue(loginResponse, "access_token")
        def refreshToken = CookieSupport.cookieValue(loginResponse, "refresh_token")

        when:
        def response = authApiClient.logout(accessToken, refreshToken)

        then:
        response.statusCode.value() == 200
        CookieSupport.cookieMaxAge(response, "access_token") == 0
        CookieSupport.cookieMaxAge(response, "refresh_token") == 0
    }

    def "POST auth/refresh returns 200 and rotates access and refresh token cookies"() {
        given:
        def loginResponse = authApiClient.login(TEST_EMAIL, TEST_PASSWORD)
        def refreshToken = CookieSupport.cookieValue(loginResponse, "refresh_token")

        when:
        def response = authApiClient.refresh(refreshToken)
        def envelope = objectMapper.readValue(response.body, Map)

        then:
        response.statusCode.value() == 200
        CookieSupport.cookieValue(response, "access_token") != null
        CookieSupport.cookieValue(response, "refresh_token") != null
        envelope.body.accessToken != null
        envelope.body.refreshToken != null
    }
}
