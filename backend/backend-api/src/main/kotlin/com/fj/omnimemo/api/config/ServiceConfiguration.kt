/*
 * ServiceConfiguration.kt
 *
 * $Since: 2026-05-25T00:00:00Z
 */
package com.fj.omnimemo.api.config

import com.fj.omnimemo.core.model.user.LoginService
import com.fj.omnimemo.core.model.user.PasswordHasher
import com.fj.omnimemo.core.model.user.RefreshTokenRepository
import com.fj.omnimemo.core.model.user.TokenIssuer
import com.fj.omnimemo.core.model.user.UserRepository
import com.fj.omnimemo.core.model.user.UserService
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.time.Duration

/**
 * Registers application-layer service beans.
 *
 * @author Francesco Jo
 * @since 0.1.1
 * @version 0.1.1
 */
@Configuration
class ServiceConfiguration {

    @Bean
    fun userService(repository: UserRepository, hasher: PasswordHasher): UserService =
        UserService(repository, hasher)

    @Bean
    fun loginService(
        repository: UserRepository,
        hasher: PasswordHasher,
        tokenIssuer: TokenIssuer,
        refreshTokenRepository: RefreshTokenRepository,
        @Value("\${app.security.refresh-token-ttl-seconds}") refreshTtlSeconds: Long,
    ): LoginService = LoginService(repository, hasher, tokenIssuer, refreshTokenRepository, Duration.ofSeconds(refreshTtlSeconds))
}
