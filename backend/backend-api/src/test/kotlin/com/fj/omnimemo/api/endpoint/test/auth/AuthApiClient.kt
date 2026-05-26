/*
 * AuthApiClient.kt
 *
 * $Since: 2026-05-26T00:00:00Z
 */
package com.fj.omnimemo.api.endpoint.test.auth

import com.fasterxml.jackson.databind.ObjectMapper
import com.fj.omnimemo.api.endpoint.ApiPathsV1
import com.fj.omnimemo.api.endpoint.auth.dto.request.LoginRequest
import com.fj.omnimemo.api.endpoint.test.CookieSupport
import com.fj.omnimemo.api.endpoint.test.CsrfSupport
import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.http.HttpEntity
import org.springframework.http.ResponseEntity

/**
 * Large Test helper that wraps [TestRestTemplate] calls for the auth endpoints.
 *
 * @author Francesco Jo
 * @since 0.1.1
 * @version 0.1.1
 */
class AuthApiClient(
    private val restTemplate: TestRestTemplate,
    private val objectMapper: ObjectMapper,
) {

    fun login(email: String, password: String): ResponseEntity<Void> =
        restTemplate.postForEntity(
            "${ApiPathsV1.AUTH}/login",
            HttpEntity(
                objectMapper.writeValueAsString(LoginRequest(email, password)),
                CsrfSupport.buildHeaders(withJson = true),
            ),
            Void::class.java,
        )

    fun logout(accessToken: String, refreshToken: String): ResponseEntity<Void> =
        restTemplate.postForEntity(
            "${ApiPathsV1.AUTH}/logout",
            HttpEntity<Any>(
                CsrfSupport.buildHeaders(
                    withJson = false,
                    cookies = mapOf("access_token" to accessToken, "refresh_token" to refreshToken),
                ),
            ),
            Void::class.java,
        )

    fun refresh(refreshToken: String): ResponseEntity<Void> =
        restTemplate.postForEntity(
            "${ApiPathsV1.AUTH}/refresh",
            HttpEntity<Any>(
                CsrfSupport.buildHeaders(
                    withJson = false,
                    cookies = mapOf("refresh_token" to refreshToken),
                ),
            ),
            Void::class.java,
        )

    fun loginAndGetAccessToken(email: String, password: String): String? =
        CookieSupport.cookieValue(login(email, password), "access_token")
}
