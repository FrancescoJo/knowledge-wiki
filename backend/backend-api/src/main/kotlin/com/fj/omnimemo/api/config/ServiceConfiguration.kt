/*
 * ServiceConfiguration.kt
 *
 * $Since: 2026-05-25T00:00:00Z
 */
package com.fj.omnimemo.api.config

import com.fj.omnimemo.core.model.user.PasswordHasher
import com.fj.omnimemo.core.model.user.UserRepository
import com.fj.omnimemo.core.model.user.UserService
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

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
}
