/*
 * SecurityConfiguration.kt
 *
 * $Since: 2026-05-25T00:00:00Z
 */
package com.fj.omnimemo.api.config

import com.fj.omnimemo.api.endpoint.ApiPathsV1
import com.fj.omnimemo.api.security.JwtAuthenticationFilter
import com.fj.omnimemo.infrastructure.security.JwtTokenService
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.boot.autoconfigure.security.servlet.PathRequest
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpMethod
import org.springframework.security.authorization.AuthorizationDecision
import org.springframework.security.authorization.AuthorizationManager
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.access.intercept.RequestAuthorizationContext
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter
import org.springframework.security.web.authentication.session.NullAuthenticatedSessionStrategy
import org.springframework.security.web.csrf.CookieCsrfTokenRepository
import org.springframework.security.web.csrf.CsrfToken
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler
import org.springframework.security.web.util.matcher.IpAddressMatcher
import org.springframework.web.filter.OncePerRequestFilter

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
 * @version 0.1.3
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
                .ignoringRequestMatchers(ApiPathsV1.BOOTSTRAP_USERS)
                // In STATELESS mode, SessionManagementFilter treats every JWT-authenticated request
                // as a "new" authentication and calls CsrfAuthenticationStrategy via a
                // CompositeSessionAuthenticationStrategy. This clears the XSRF-TOKEN cookie on
                // every authenticated page load, making the token baked into rendered HTML stale.
                // Replacing the CSRF authentication strategy with a no-op prevents this rotation;
                // token fixation protection is not applicable to the double-submit cookie pattern.
                .sessionAuthenticationStrategy(NullAuthenticatedSessionStrategy())
        }
        .addFilterBefore(
            JwtAuthenticationFilter(jwtTokenService),
            UsernamePasswordAuthenticationFilter::class.java,
        )
        .addFilterAfter(
            CsrfCookieFilter(),
            UsernamePasswordAuthenticationFilter::class.java,
        )
        .authorizeHttpRequests { customiser ->
            // Happens only once during server startup
            @Suppress("SpreadOperator")
            customiser
                .requestMatchers(PathRequest.toStaticResources().atCommonLocations()).permitAll()
                .requestMatchers("/error").permitAll()
                .requestMatchers(
                    HttpMethod.GET,
                    "/", "/health", "/contents", "/notes", "/notes/**",
                    *WEB_STATIC_PATHS.map { "$it/**" }.toTypedArray()
                ).permitAll()
                .requestMatchers(HttpMethod.GET, "/api/v1/health").permitAll()
                .requestMatchers(HttpMethod.GET, "/swagger-ui/**", "/v3/api-docs/**").permitAll()
                .requestMatchers(HttpMethod.GET, ApiPathsV1.NOTES, "${ApiPathsV1.NOTES}/**").permitAll()
                .requestMatchers("${ApiPathsV1.AUTH}/**").permitAll()
                .requestMatchers(HttpMethod.POST, ApiPathsV1.BOOTSTRAP_USERS).access(localhostOnly())
                .anyRequest().authenticated()
        }
        .formLogin { it.disable() }
        .httpBasic { it.disable() }
        .build()

    private class CsrfCookieFilter : OncePerRequestFilter() {
        // Static assets and non-page endpoints never need to regenerate the XSRF-TOKEN cookie.
        // Allowing them through causes the cookie to be replaced on every parallel asset load,
        // making the token baked into the page HTML diverge from the live cookie value.
        override fun shouldNotFilter(request: HttpServletRequest): Boolean {
            val path = request.servletPath

            return WEB_STATIC_PATHS.any { path.startsWith("$it/") } ||
                    ALLOWED_RESOURCE_PATHS.any { path == it }
        }

        override fun doFilterInternal(
            request: HttpServletRequest,
            response: HttpServletResponse,
            filterChain: FilterChain,
        ) {
            // Spring Security 6.x uses deferred CSRF token loading: the XSRF-TOKEN cookie is only
            // written to the response when the token attribute is actually accessed. For SPA/HTMX
            // clients that read the cookie via JavaScript, we must trigger access on every request
            // so the cookie is present before the first form submission.
            val csrfToken = request.getAttribute(CsrfToken::class.java.name) as? CsrfToken
            csrfToken?.token
            filterChain.doFilter(request, response)
        }
    }

    private fun localhostOnly(): AuthorizationManager<RequestAuthorizationContext> {
        val ipv4 = IpAddressMatcher("127.0.0.1")
        val ipv6 = IpAddressMatcher("::1")
        return AuthorizationManager { _, ctx ->
            val addr = ctx.request.remoteAddr
            AuthorizationDecision(
                ipv4.matches(addr) || ipv6.matches(addr),
            )
        }
    }

    companion object {
        private val WEB_STATIC_PATHS = listOf(
            "/flags",
            "/lib",
            "/css",
            "/js",
            "/images",
            "/webjars",
        )
        private val ALLOWED_RESOURCE_PATHS = listOf(
            "/health",
            "/favicon.ico",
        )
    }
}
