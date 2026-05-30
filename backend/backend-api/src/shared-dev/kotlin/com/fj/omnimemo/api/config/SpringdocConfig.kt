/*
 * SpringdocConfig.kt
 *
 * $Since: 2026-05-30T00:00:00Z
 */
package com.fj.omnimemo.api.config

import com.fasterxml.jackson.databind.ObjectMapper
import io.swagger.v3.core.util.Json
import io.swagger.v3.oas.models.Components
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.info.Info
import io.swagger.v3.oas.models.media.Schema
import org.slf4j.LoggerFactory
import org.springdoc.core.customizers.OpenApiCustomizer
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.Ordered
import org.springframework.core.annotation.Order
import org.springframework.core.io.support.PathMatchingResourcePatternResolver

/**
 * Loads API schema definitions from JSON files under `docs/api-schema/` on the classpath
 * and registers them as OpenAPI components, replacing all auto-generated schemas.
 *
 * Schema names are derived from the file path relative to `docs/api-schema/`, with `/`
 * replaced by `.` and the `.schema.json` suffix stripped.
 * Example: `v1/user/UserResponse.schema.json` → component name `v1.user.UserResponse`.
 *
 * This class is compiled only for local and alpha builds (via the `shared-dev` source set)
 * and is entirely absent from beta and release binaries.
 *
 * @author Francesco Jo
 * @since 0.1.1
 */
@Configuration
class SpringdocConfig(private val objectMapper: ObjectMapper) {

    private val log = LoggerFactory.getLogger(javaClass)

    private fun attachSchemas(components: Components) {
        val resolver = PathMatchingResourcePatternResolver()
        val resources = resolver.getResources("classpath:/$SCHEMA_DIR/**/*.$SCHEMA_SUFFIX")

        for (resource in resources) {
            val uri = resource.uri.toString()
            val schemaName = uri
                .substringAfter(SCHEMA_DIR)
                .substringBeforeLast(".$SCHEMA_SUFFIX")
                .trimStart('/')
                .replace("/", ".")

            log.trace("Loading API schema: {} from {}", schemaName, uri)

            runCatching {
                val tree = resource.inputStream.use { objectMapper.readTree(it) }
                @Suppress("UNCHECKED_CAST")
                components.addSchemas(schemaName, Json.mapper().convertValue(tree, Schema::class.java))
            }.onFailure { e ->
                log.warn("Failed to load API schema: {}", schemaName, e)
            }
        }
    }

    @Bean
    fun openApi(@Value("\${spring.application.name}") appName: String): OpenAPI =
        OpenAPI().info(Info().title(appName).version("v1"))

    @Bean
    @Order(Ordered.LOWEST_PRECEDENCE)
    fun openApiCustomiser(): OpenApiCustomizer = OpenApiCustomizer { openApi ->
        openApi.components = Components().apply { attachSchemas(this) }
    }

    companion object {
        private const val SCHEMA_DIR = "docs/api-schema"
        private const val SCHEMA_SUFFIX = "schema.json"
    }
}
