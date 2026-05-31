/*
 * ParsingTest.kt
 *
 * $Since: 2026-05-26T00:00:00Z
 */
package testcase.small.core.util

import com.fj.omnimemo.core.util.parseUuidOrNull
import io.kotest.assertions.assertSoftly
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import org.junit.jupiter.api.Test
import test.com.fj.omnimemo.core.annotation.SmallTest
import java.util.*

@SmallTest
class ParsingTest {
    @Test
    fun `parseUuidOrNull returns UUID for a valid UUID string`() {
        val id = UUID.randomUUID()

        val result = parseUuidOrNull(id.toString())

        result shouldBe id
    }

    @Test
    fun `parseUuidOrNull returns null for an empty string`() {
        parseUuidOrNull("") shouldBe null
    }

    @Test
    fun `parseUuidOrNull returns null for a non-UUID string`() {
        parseUuidOrNull("not-a-uuid") shouldBe null
    }

    @Test
    fun `parseUuidOrNull returns null for a partial UUID string`() {
        parseUuidOrNull("12345678-1234-1234") shouldBe null
    }

    @Test
    fun `parseUuidOrNull returns UUID for all-zero UUID`() {
        val result = parseUuidOrNull("00000000-0000-0000-0000-000000000000")

        assertSoftly {
            result shouldNotBe null
            result shouldBe UUID(0, 0)
        }
    }
}
