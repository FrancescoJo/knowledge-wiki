/*
 * OmniMemoErrorCodeTest.kt
 *
 * $Since: 2026-05-28T00:00:00Z
 */
package testcase.small.core.exception

import com.fj.omnimemo.core.exception.OmniMemoErrorCode
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import test.com.fj.omnimemo.core.annotation.SmallTest

@SmallTest
class OmniMemoErrorCodeTest {
    @Test
    fun `every OmniMemoErrorCode entry has a unique code value`() {
        val codes = OmniMemoErrorCode.entries.map { it.code }
        val distinctCodes = codes.toSet()

        distinctCodes shouldHaveSize codes.size
    }

    @Test
    fun `code format is an 8-digit uppercase hex string prefixed with 0x`() {
        OmniMemoErrorCode.entries.forEach { entry ->
            entry.code shouldBe Regex("0x[0-9A-F]{8}").find(entry.code)?.value
        }
    }
}
