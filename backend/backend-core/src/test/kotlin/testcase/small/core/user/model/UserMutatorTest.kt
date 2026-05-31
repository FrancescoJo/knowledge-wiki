/*
 * UserMutatorTest.kt
 *
 * $Since: 2026-05-31T00:00:00Z
 */
package testcase.small.core.user.model

import com.fj.omnimemo.core.user.model.User
import com.fj.omnimemo.core.user.mutate
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import test.com.fj.omnimemo.core.annotation.SmallTest

/**
 * Small tests for the UserMutator equals/hashCode contract.
 *
 * @author Francesco Jo
 * @since 0.2.0
 */
@SmallTest
class UserMutatorTest {
    private val user = User.create("alice@example.com", "hash")

    @Test
    fun `equals returns true for two mutators with identical state`() {
        val a = user.mutate()
        val b = user.mutate()

        (a == b) shouldBe true
    }

    @Test
    fun `equals returns false when compared to a non-mutator type`() {
        val mutator = user.mutate()

        mutator.equals("not a mutator") shouldBe false
    }

    @Test
    fun `equals returns false after mutating email`() {
        val a = user.mutate()
        val b = user.mutate().also { it.email = "other@example.com" }

        (a == b) shouldBe false
    }

    @Test
    fun `hashCode is consistent with equals`() {
        val a = user.mutate()
        val b = user.mutate()

        a.hashCode() shouldBe b.hashCode()
    }
}
