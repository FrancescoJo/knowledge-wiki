/*
 * GlobalModelAdvice.kt
 *
 * $Since: 2026-05-23T00:00:00Z
 */
package com.fj.omnimemo.api.advice

import com.fj.omnimemo.core.menu.model.MenuBar
import com.fj.omnimemo.core.menu.model.SimpleMenuItem
import org.springframework.beans.factory.annotation.Value
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.bind.annotation.ModelAttribute

/**
 * Supplies common model attributes to every view render.
 *
 * @author Francesco Jo
 * @since 0.1.1
 */
@ControllerAdvice
class GlobalModelAdvice(@Value("\${app.build-phase}") private val buildPhase: String) {

    @ModelAttribute("menuBar")
    fun menuBar(): MenuBar = MenuBar(
        listOf(
            SimpleMenuItem(label = "Home", href = "/", priority = 0)
        )
    )

    @ModelAttribute("buildPhase")
    fun buildPhase(): String = buildPhase
}
