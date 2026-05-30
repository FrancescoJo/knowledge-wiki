/*
 * BreadcrumbItem.kt
 *
 * $Since: 2026-05-30T00:00:00Z
 */
package com.fj.omnimemo.view

/**
 * Represents a single step in the page breadcrumb trail.
 *
 * A null [href] renders the item as plain text (current page, last item).
 *
 * @author Francesco Jo
 * @since 0.2.0
 */
data class BreadcrumbItem(val label: String, val href: String? = null)
