/*
 * SecurityConfiguration.kt
 *
 * $Since: 2026-05-25T00:00:00Z
 */
package com.fj.omnimemo.api.config

import com.fj.omnimemo.api.security.JwtAuthenticationFilter
import com.fj.omnimemo.infrastructure.security.JwtTokenService
import org.springframework.boot.autoconfigure.security.servlet.PathRequest
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpMethod
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter
import org.springframework.security.web.csrf.CookieCsrfTokenRepository
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler

/**
 * Configures the Spring Security filter chain for stateless JWT-based authentication.
 *
 * CSRF uses the double-submit cookie pattern via [CookieCsrfTokenRepository] with a
 * non-httpOnly cookie. [CsrfTokenRequestAttributeHandler] is used instead of the default
 * XOR-masking handler so that HTMX can read the raw `XSRF-TOKEN` cookie value and submit
 * it unchanged as the `X-XSRF-TOKEN` request header.
 *
 * @author Francesco Jo
 * @since 0.1.1
 * @version 0.1.1
 */
@Configuration
@EnableWebSecurity
class SecurityConfiguration(private val jwtTokenService: JwtTokenService) {

    @Bean
    fun securityFilterChain(http: HttpSecurity): SecurityFilterChain = http
        .sessionManagement { it.sessionCreationPolicy(SessionCreationPolicy.STATELESS) }
        .csrf {
            it.csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
                .csrfTokenRequestHandler(CsrfTokenRequestAttributeHandler())
        }
        .addFilterBefore(
            JwtAuthenticationFilter(jwtTokenService),
            UsernamePasswordAuthenticationFilter::class.java,
        )
        .authorizeHttpRequests {
            it
                .requestMatchers(PathRequest.toStaticResources().atCommonLocations()).permitAll()
                .requestMatchers("/error").permitAll()
                .requestMatchers(HttpMethod.GET, "/", "/lib/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/v1/health").permitAll()
                .requestMatchers(HttpMethod.GET, "/swagger-ui/**", "/v3/api-docs/**").permitAll()
                .requestMatchers("/api/v1/auth/**").permitAll()
                .anyRequest().authenticated()
        }
        .formLogin { it.disable() }
        .httpBasic { it.disable() }
        .build()
}
