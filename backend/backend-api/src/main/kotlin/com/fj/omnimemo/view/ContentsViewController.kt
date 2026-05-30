/*
 * ContentsViewController.kt
 *
 * $Since: 2026-05-30T00:00:00Z
 */
package com.fj.omnimemo.view

import org.springframework.context.MessageSource
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.web.bind.annotation.GetMapping
import java.util.Locale

/**
 * View endpoint for the contents index page.
 *
 * @author Francesco Jo
 * @since 0.2.0
 */
@Controller
class ContentsViewController(private val messageSource: MessageSource) {

    @GetMapping("/contents")
    fun contents(locale: Locale, model: Model): String {
        model.addAttribute(
            "breadcrumbs", listOf(
                BreadcrumbItem(messageSource.getMessage("nav.home", null, locale), "/"),
                BreadcrumbItem(messageSource.getMessage("nav.contents", null, locale)),
            )
        )
        return "contents"
    }
}
