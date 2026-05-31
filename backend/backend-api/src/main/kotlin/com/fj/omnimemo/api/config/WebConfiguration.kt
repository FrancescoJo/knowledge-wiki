/*
 * WebConfiguration.kt
 *
 * $Since: 2026-05-28T00:00:00Z
 */
package com.fj.omnimemo.api.config

import com.fj.omnimemo.api.config.WebConfiguration.Companion.SUPPORTED_LOCALES
import jakarta.servlet.http.HttpServletRequest
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.servlet.LocaleResolver
import org.springframework.web.servlet.config.annotation.InterceptorRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer
import org.springframework.web.servlet.i18n.CookieLocaleResolver
import org.springframework.web.servlet.i18n.LocaleChangeInterceptor
import java.util.*

/**
 * Configures locale resolution for browser-facing UI.
 *
 * Resolution order:
 * 1. Cookie `omnimemo_lang` set by a previous explicit language selection
 * 2. Best match from the browser's `Accept-Language` header against [SUPPORTED_LOCALES]
 * 3. English as the ultimate fallback
 *
 * REST API responses are not affected; they remain English-only.
 *
 * @author Francesco Jo
 * @since 0.1.1
 */
@Configuration
class WebConfiguration : WebMvcConfigurer {
    @Bean
    fun localeResolver(): LocaleResolver = BrowserAwareLocaleResolver()

    @Bean
    fun localeChangeInterceptor(): LocaleChangeInterceptor =
        LocaleChangeInterceptor().apply { paramName = LANG_PARAM }

    override fun addInterceptors(registry: InterceptorRegistry) {
        registry.addInterceptor(localeChangeInterceptor())
    }

    private inner class BrowserAwareLocaleResolver : CookieLocaleResolver(COOKIE_NAME) {
        @Suppress("OVERRIDE_DEPRECATION")
        override fun determineDefaultLocale(request: HttpServletRequest): Locale {
            val locales = request.getLocales()
            while (locales.hasMoreElements()) {
                val locale = locales.nextElement()
                if (SUPPORTED_LOCALES.any { it.language == locale.language }) {
                    return Locale.forLanguageTag(locale.language)
                }
            }
            return Locale.ENGLISH
        }
    }

    companion object {
        private const val COOKIE_NAME = "omnimemo_lang"
        private const val LANG_PARAM = "lang"
        val SUPPORTED_LOCALES = listOf(Locale.ENGLISH, Locale.KOREAN)
    }
}
