/*
 * AbstractMockRepository.kt
 *
 * $Since: 2026-05-31T00:00:00Z
 */
package test.com.fj.omnimemo.core.repository

/**
 * In-memory backing store for Small-test fake repositories.
 *
 * Subclasses supply a [keyOf] function that extracts the map key from an entity,
 * then delegate standard CRUD operations to [doSave], [doFindByKey], and
 * [doDeleteByKey]. Call [clear] in `@AfterEach` to reset state between tests.
 *
 * @author Francesco Jo
 * @since 0.2.0
 */
open class AbstractMockRepository<K, V>(private val keyOf: (V) -> K) {
    protected val store: MutableMap<K, V> = mutableMapOf()

    protected fun doSave(entity: V): V = entity.also { store[keyOf(entity)] = entity }

    protected fun doFindByKey(key: K): V? = store[key]

    protected fun doDeleteByKey(key: K) {
        store.remove(key)
    }

    fun clear() = store.clear()
}
