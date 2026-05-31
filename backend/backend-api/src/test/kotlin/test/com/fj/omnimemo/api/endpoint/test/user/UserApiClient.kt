/*
 * UserApiClient.kt
 *
 * $Since: 2026-05-26T00:00:00Z
 */
package test.com.fj.omnimemo.api.endpoint.test.user

import com.fasterxml.jackson.databind.ObjectMapper
import com.fj.omnimemo.api.endpoint.ApiPathsV1
import com.fj.omnimemo.api.endpoint.user.dto.request.CreateUserRequest
import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.ResponseEntity
import test.com.fj.omnimemo.api.endpoint.test.CsrfSupport

/**
 * Large Test helper that wraps [TestRestTemplate] calls for the user endpoints.
 *
 * @author Francesco Jo
 * @since 0.1.1
 * @version 0.1.1
 */
class UserApiClient(
    private val restTemplate: TestRestTemplate,
    private val objectMapper: ObjectMapper,
) {
    fun findById(id: String, accessToken: String): ResponseEntity<String> =
        restTemplate.exchange(
            "${ApiPathsV1.USERS}/$id",
            HttpMethod.GET,
            HttpEntity<Any>(HttpHeaders().apply { add(HttpHeaders.COOKIE, "access_token=$accessToken") }),
            String::class.java,
        )

    fun create(email: String, password: String, accessToken: String): ResponseEntity<String> =
        restTemplate.exchange(
            ApiPathsV1.USERS,
            HttpMethod.POST,
            HttpEntity(
                objectMapper.writeValueAsString(CreateUserRequest(email, password)),
                CsrfSupport.buildHeaders(withJson = true, cookies = mapOf("access_token" to accessToken)),
            ),
            String::class.java,
        )

    @Suppress("ForbiddenVoid")
    fun delete(id: String, accessToken: String): ResponseEntity<Void> =
        restTemplate.exchange(
            "${ApiPathsV1.USERS}/$id",
            HttpMethod.DELETE,
            HttpEntity<Any>(
                CsrfSupport.buildHeaders(withJson = false, cookies = mapOf("access_token" to accessToken)),
            ),
            Void::class.java,
        )
}
