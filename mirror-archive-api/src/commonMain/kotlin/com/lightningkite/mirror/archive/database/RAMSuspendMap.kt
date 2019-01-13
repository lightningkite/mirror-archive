package com.lightningkite.mirror.archive.database

import com.lightningkite.mirror.archive.model.Condition
import com.lightningkite.mirror.archive.model.Operation
import com.lightningkite.mirror.archive.model.Sort
import com.lightningkite.mirror.info.Type
import kotlin.random.Random

class RAMSuspendMap<K, V : Any>(val underlying: MutableMap<K, V>, val makeNewKey: () -> K) : SuspendMap<K, V> {

    object Provider : SuspendMapProvider {
        val maps = HashMap<String, RAMSuspendMap<*, *>>()
        @Suppress("UNCHECKED_CAST")
        override fun <K, V : Any> suspendMap(key: Type<K>, value: Type<V>, name: String?): RAMSuspendMap<K, V> {
            return maps.getOrPut(name ?: Random.nextInt().toString(16)) {
                RAMSuspendMap<K, V>(HashMap()) { throw UnsupportedOperationException() }
            } as RAMSuspendMap<K, V>
        }
    }

    override suspend fun getNewKey(): K {
        return makeNewKey()
    }

    override suspend fun get(key: K): V? = underlying[key]

    override suspend fun put(key: K, value: V, conditionIfExists: Condition<V>, create: Boolean): Boolean {
        val current = underlying[key]
        return if (current != null && conditionIfExists.invoke(current) || current == null && create) {
            underlying[key] = value
            true
        } else {
            false
        }
    }

    override suspend fun modify(key: K, operation: Operation<V>, condition: Condition<V>): V? {
        val current = underlying[key]
        return if (current != null && condition.invoke(current)) {
            val newItem = operation.invoke(current)
            underlying[key] = newItem
            newItem
        } else {
            null
        }
    }

    override suspend fun remove(key: K, condition: Condition<V>): Boolean {
        val current = underlying[key]
        return if (current != null && condition.invoke(current)) {
            underlying.remove(key)
            true
        } else {
            false
        }
    }

    override suspend fun query(
            condition: Condition<V>,
            keyCondition: Condition<K>,
            sortedBy: Sort<V>?,
            after: SuspendMap.Entry<K, V>?,
            count: Int
    ): List<SuspendMap.Entry<K, V>> {
        var afterFound = false
        return underlying.entries.asSequence()
                .filter { condition.invoke(it.value) && keyCondition.invoke(it.key) }
                .map { SuspendMap.Entry(it.key, it.value) }
                .let {
                    if (sortedBy == null) {
                        it
                    } else {
                        it.sortedWith(Comparator { a, b ->
                            sortedBy.compare(a.value, b.value)
                        })
                    }
                }
                .let {
                    if (after == null) it else {
                        it.dropWhile {
                            if (it == after) {
                                afterFound = true
                                true
                            } else !afterFound
                        }
                    }
                }
                .take(count)
                .toList()
    }

}