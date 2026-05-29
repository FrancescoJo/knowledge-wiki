/*
 * ServiceConfiguration.kt
 *
 * $Since: 2026-05-25T00:00:00Z
 */
package com.fj.omnimemo.api.config

import com.fj.omnimemo.api.user.LruUserProfileCache
import com.fj.omnimemo.core.security.TokenIssuer
import com.fj.omnimemo.core.user.UserProfileCache
import com.fj.omnimemo.core.user.repository.RefreshTokenRepository
import com.fj.omnimemo.core.user.repository.UserRepository
import com.fj.omnimemo.core.user.security.PasswordHasher
import com.fj.omnimemo.core.user.usecase.BootstrapUserUseCase
import com.fj.omnimemo.core.user.usecase.CreateUserUseCase
import com.fj.omnimemo.core.user.usecase.DeleteUserUseCase
import com.fj.omnimemo.core.user.usecase.FindUserUseCase
import com.fj.omnimemo.core.user.usecase.LoginUseCase
import com.fj.omnimemo.core.user.usecase.UpdateUserEmailUseCase
import com.fj.omnimemo.core.user.usecase.UpdateUserPasswordUseCase
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
    fun bootstrapUserUseCase(repository: UserRepository, hasher: PasswordHasher): BootstrapUserUseCase =
        BootstrapUserUseCase(repository, hasher)

    @Bean
    fun createUserUseCase(repository: UserRepository, hasher: PasswordHasher): CreateUserUseCase =
        CreateUserUseCase(repository, hasher)

    @Bean
    fun findUserUseCase(repository: UserRepository): FindUserUseCase =
        FindUserUseCase(repository)

    @Bean
    fun userProfileCache(userRepository: UserRepository): UserProfileCache =
        LruUserProfileCache(userRepository)

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
