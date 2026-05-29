/*
 * GlobalModelAdvice.kt
 *
 * $Since: 2026-05-23T00:00:00Z
 */
package com.fj.omnimemo.api.advice

import com.fj.omnimemo.core.menu.model.MenuBar
import com.fj.omnimemo.core.menu.model.SimpleMenuItem
import com.fj.omnimemo.core.user.UserProfileCache
import com.fj.omnimemo.core.user.model.UserId
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.MessageSource
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.bind.annotation.ModelAttribute
import java.util.Locale
import java.util.UUID

/**
 * Supplies common model attributes to every view render.
 *
 * @author Francesco Jo
 * @since 0.1.1
 */
@ControllerAdvice
class GlobalModelAdvice(
    @Value("\${app.build-phase}") private val buildPhase: String,
    private val messageSource: MessageSource,
    private val userProfileCache: UserProfileCache,
) {

    @ModelAttribute("menuBar")
    fun menuBar(locale: Locale): MenuBar = MenuBar(
        listOf(
            SimpleMenuItem(label = messageSource.getMessage("nav.home", null, locale), href = "/", priority = 0)
        )
    )

    @ModelAttribute("buildPhase")
    fun buildPhase(): String = buildPhase

    @ModelAttribute("currentUserEmail")
    fun currentUserEmail(): String? {
        val auth = SecurityContextHolder.getContext().authentication ?: return null
        if (!auth.isAuthenticated || auth.principal == "anonymousUser") return null
        return try {
            val userId = UserId(UUID.fromString(auth.principal as String))
            userProfileCache.get(userId)?.email
        } catch (_: Exception) {
            null
        }
    }
}
