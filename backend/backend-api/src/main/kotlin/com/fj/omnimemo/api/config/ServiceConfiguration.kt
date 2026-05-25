/*
 * ServiceConfiguration.kt
 *
 * $Since: 2026-05-25T00:00:00Z
 */
package com.fj.omnimemo.api.config

import com.fj.omnimemo.core.model.user.PasswordHasher
import com.fj.omnimemo.core.model.user.RefreshTokenRepository
import com.fj.omnimemo.core.model.user.TokenIssuer
import com.fj.omnimemo.core.model.user.UserRepository
import com.fj.omnimemo.core.usecase.user.CreateUserUseCase
import com.fj.omnimemo.core.usecase.user.DeleteUserUseCase
import com.fj.omnimemo.core.usecase.user.FindUserUseCase
import com.fj.omnimemo.core.usecase.user.LoginUseCase
import com.fj.omnimemo.core.usecase.user.UpdateUserEmailUseCase
import com.fj.omnimemo.core.usecase.user.UpdateUserPasswordUseCase
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.time.Duration

/**
 * Registers application-layer use case beans.
 *
 * @author Francesco Jo
 * @since 0.1.1
 * @version 0.1.1
 */
@Configuration
class ServiceConfiguration {

    @Bean
    fun createUserUseCase(repository: UserRepository, hasher: PasswordHasher): CreateUserUseCase =
        CreateUserUseCase(repository, hasher)

    @Bean
    fun findUserUseCase(repository: UserRepository): FindUserUseCase =
        FindUserUseCase(repository)

    @Bean
    fun updateUserEmailUseCase(repository: UserRepository): UpdateUserEmailUseCase =
        UpdateUserEmailUseCase(repository)

    @Bean
    fun updateUserPasswordUseCase(repository: UserRepository, hasher: PasswordHasher): UpdateUserPasswordUseCase =
        UpdateUserPasswordUseCase(repository, hasher)

    @Bean
    fun deleteUserUseCase(repository: UserRepository): DeleteUserUseCase =
        DeleteUserUseCase(repository)

    @Bean
    fun loginUseCase(
        repository: UserRepository,
        hasher: PasswordHasher,
        tokenIssuer: TokenIssuer,
        refreshTokenRepository: RefreshTokenRepository,
        @Value("\${app.security.refresh-token-ttl-seconds}") refreshTtlSeconds: Long,
    ): LoginUseCase =
        LoginUseCase(repository, hasher, tokenIssuer, refreshTokenRepository, Duration.ofSeconds(refreshTtlSeconds))
}
