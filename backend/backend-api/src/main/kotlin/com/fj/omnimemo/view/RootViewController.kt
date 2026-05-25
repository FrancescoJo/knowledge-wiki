/*
 * RootViewController.kt
 *
 * $Since: 2026-05-22T00:00:00Z
 */
package com.fj.omnimemo.view

import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.servlet.ModelAndView

/**
 * View endpoint for the application root.
 *
 * @author Francesco Jo
 * @since 0.1.1
 */
@Controller
class RootViewController {

    /**
     * Serves the main index page.
     *
     * @return ModelAndView resolving to the index template
     * @since 0.1.1
     */
    @GetMapping("/")
    fun index(): ModelAndView = ModelAndView("index")
}
