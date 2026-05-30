/*
 * LabelledMenu.kt
 *
 * $Since: 2026-05-23T00:00:00Z
 */
package com.fj.omnimemo.core.menu.model

/**
 * A navigation entry that carries a visible label and a navigation target.
 *
 * @author Francesco Jo
 * @since 0.1.1
 */
sealed interface LabelledMenu : Menu {
    val label: String
    val href: String
    val requiredRoles: Set<String>
    val visible: Boolean
    val enabled: Boolean
}
