/*
 * MenuDivider.kt
 *
 * $Since: 2026-05-23T00:00:00Z
 */
package com.fj.omnimemo.core.menu.model

/**
 * A visual separator between groups of navigation entries.
 *
 * @author Francesco Jo
 * @since 0.1.1
 */
data class MenuDivider(override val priority: Int = 0) : Menu
