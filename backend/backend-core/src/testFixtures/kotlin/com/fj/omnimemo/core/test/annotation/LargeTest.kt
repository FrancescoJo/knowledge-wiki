/*
 * LargeTest.kt
 *
 * $Since: 2026-05-23T00:00:00Z
 */
package com.fj.omnimemo.core.test.annotation

import org.junit.jupiter.api.Tag

/**
 * Marks a test class or method as a Large test (end-to-end or system test with real external dependencies).
 *
 * @author Francesco Jo
 * @since 0.1.1
 * @version 0.1.1
 */
@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
@Tag("large")
annotation class LargeTest
