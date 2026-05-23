/*
 * MenuBar.kt
 *
 * $Since: 2026-05-23T00:00:00Z
 */
package com.fj.omnimemo.core.model.menu

/**
 * The top-level navigation container. Entries are sorted by priority in ascending order.
 *
 * @author Francesco Jo
 * @since 0.1.1
 */
class MenuBar(items: List<Menu>) {
    val items: List<Menu> = items.sortedBy { it.priority }
}
