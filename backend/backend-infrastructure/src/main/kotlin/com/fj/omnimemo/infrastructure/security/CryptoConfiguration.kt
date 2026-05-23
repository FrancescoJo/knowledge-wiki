/*
 * CryptoConfiguration.kt
 *
 * $Since: 2026-05-23T00:00:00Z
 */
package com.fj.omnimemo.infrastructure.security

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.util.Base64

/**
 * Spring configuration for cryptographic service beans.
 *
 * Keys are loaded from application configuration properties and decoded from Base64.
 *
 * @author Francesco Jo
 * @since 0.1.1
 * @version 0.1.1
 */
@Configuration
class CryptoConfiguration {

    @Bean
    fun aesGcmCipher(@Value("\${app.security.aes-key}") aesKeyBase64: String): AesGcmCipher =
        AesGcmCipher(Base64.getDecoder().decode(aesKeyBase64))

    @Bean
    fun hmacBlindIndex(@Value("\${app.security.hmac-key}") hmacKeyBase64: String): HmacBlindIndex =
        HmacBlindIndex(Base64.getDecoder().decode(hmacKeyBase64))
}
