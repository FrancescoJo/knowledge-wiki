/*
 * Menu.kt
 *
 * $Since: 2026-05-23T00:00:00Z
 */
package com.fj.omnimemo.core.model.menu

/**
 * Base contract for all navigation entries. Subtypes form a closed hierarchy.
 *
 * @author Francesco Jo
 * @since 0.1.1
 */
sealed interface Menu {
    val priority: Int
}
