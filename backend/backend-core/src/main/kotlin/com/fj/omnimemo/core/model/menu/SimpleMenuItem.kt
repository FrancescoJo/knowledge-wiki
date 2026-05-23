/*
 * SimpleMenuItem.kt
 *
 * $Since: 2026-05-23T00:00:00Z
 */
package com.fj.omnimemo.core.model.menu

/**
 * A leaf navigation entry with a label and navigation target.
 *
 * @author Francesco Jo
 * @since 0.1.1
 */
data class SimpleMenuItem(
    override val label: String,
    override val href: String,
    override val priority: Int = 0,
    override val requiredRoles: Set<String> = emptySet(),
    override val enabled: Boolean = true,
) : LabelledMenu
