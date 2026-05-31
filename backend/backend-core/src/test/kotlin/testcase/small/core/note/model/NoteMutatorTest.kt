/*
 * NoteMutatorTest.kt
 *
 * $Since: 2026-05-31T00:00:00Z
 */
package testcase.small.core.note.model

import com.fj.omnimemo.core.note.mutate
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import test.com.fj.omnimemo.core.annotation.SmallTest
import test.com.fj.omnimemo.core.note.randomNote

/**
 * Small tests for the NoteMutator equals/hashCode contract.
 *
 * @author Francesco Jo
 * @since 0.2.0
 */
@SmallTest
class NoteMutatorTest {
    private val note = randomNote(title = "Test Note")

    @Test
    fun `equals returns true for two mutators with identical state`() {
        val a = note.mutate()
        val b = note.mutate()

        (a == b) shouldBe true
    }

    @Test
    fun `equals returns false when compared to a non-mutator type`() {
        val mutator = note.mutate()

        mutator.equals("not a mutator") shouldBe false
    }

    @Test
    fun `equals returns false after mutating title`() {
        val a = note.mutate()
        val b = note.mutate().also { it.title = "Different Title" }

        (a == b) shouldBe false
    }

    @Test
    fun `hashCode is consistent with equals`() {
        val a = note.mutate()
        val b = note.mutate()

        a.hashCode() shouldBe b.hashCode()
    }
}
