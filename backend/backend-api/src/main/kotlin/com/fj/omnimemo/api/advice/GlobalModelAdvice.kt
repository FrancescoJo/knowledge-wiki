/*
 * GlobalModelAdvice.kt
 *
 * $Since: 2026-05-23T00:00:00Z
 */
package com.fj.omnimemo.api.advice

import com.fj.omnimemo.core.model.menu.MenuBar
import com.fj.omnimemo.core.model.menu.SimpleMenuItem
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.bind.annotation.ModelAttribute

/**
 * Supplies common model attributes to every view render.
 *
 * @author Francesco Jo
 * @since 0.1.1
 */
@ControllerAdvice
class GlobalModelAdvice {

    @ModelAttribute("menuBar")
    fun menuBar(): MenuBar = MenuBar(
        listOf(
            SimpleMenuItem(label = "Home", href = "/", priority = 0)
        )
    )
}
