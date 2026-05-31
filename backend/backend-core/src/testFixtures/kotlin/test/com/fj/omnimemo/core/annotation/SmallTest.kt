/*
 * SmallTest.kt
 *
 * $Since: 2026-05-23T00:00:00Z
 */
package test.com.fj.omnimemo.core.annotation

import org.junit.jupiter.api.Tag

/**
 * Marks a test class or method as a Small test (unit test with no I/O or external dependencies).
 *
 * @author Francesco Jo
 * @since 0.1.1
 * @version 0.1.1
 */
@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
@Tag("small")
annotation class SmallTest
