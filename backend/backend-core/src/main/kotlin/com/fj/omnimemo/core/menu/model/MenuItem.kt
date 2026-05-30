/*
 * MenuItem.kt
 *
 * $Since: 2026-05-23T00:00:00Z
 */
package com.fj.omnimemo.core.menu.model

/**
 * A navigation entry that expands to reveal child entries.
 *
 * @author Francesco Jo
 * @since 0.1.1
 */
data class MenuItem(
    override val label: String,
    override val href: String,
    val children: List<Menu>,
    override val priority: Int = 0,
    override val requiredRoles: Set<String> = emptySet(),
    override val visible: Boolean = true,
    override val enabled: Boolean = true,
) : LabelledMenu
