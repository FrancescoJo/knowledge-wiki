/*
 * ParsingTest.kt
 *
 * $Since: 2026-05-26T00:00:00Z
 */
package com.fj.omnimemo.core.util

import com.fj.omnimemo.core.test.annotation.SmallTest
import org.junit.jupiter.api.Test
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

@SmallTest
class ParsingTest {

    @Test
    fun `parseUuidOrNull returns UUID for a valid UUID string`() {
        val id = UUID.randomUUID()

        val result = parseUuidOrNull(id.toString())

        assertEquals(id, result)
    }

    @Test
    fun `parseUuidOrNull returns null for an empty string`() {
        assertNull(parseUuidOrNull(""))
    }

    @Test
    fun `parseUuidOrNull returns null for a non-UUID string`() {
        assertNull(parseUuidOrNull("not-a-uuid"))
    }

    @Test
    fun `parseUuidOrNull returns null for a partial UUID string`() {
        assertNull(parseUuidOrNull("12345678-1234-1234"))
    }

    @Test
    fun `parseUuidOrNull returns UUID for all-zero UUID`() {
        val result = parseUuidOrNull("00000000-0000-0000-0000-000000000000")

        assertNotNull(result)
        assertEquals(UUID(0, 0), result)
    }
}
