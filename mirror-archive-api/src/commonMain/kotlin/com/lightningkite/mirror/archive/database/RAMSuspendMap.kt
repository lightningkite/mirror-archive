package com.lightningkite.mirror.archive.database

import com.lightningkite.mirror.archive.model.Condition
import com.lightningkite.mirror.archive.model.Operation
import com.lightningkite.mirror.archive.model.Sort

class RAMSuspendMap<K, V: Any>(val underlying: MutableMap<K, V>, val makeNewKey:()->K): SuspendMap<K, V> {
    override suspend fun getNewKey(): K {
        return makeNewKey()
    }

    override suspend fun get(key: K): V? = underlying[key]

    override suspend fun put(key: K, value: V, conditionIfExists: Condition<V>, create: Boolean): Boolean {
        val current = underlying[key]
        return if(current != null && conditionIfExists.invoke(current) || current == null && create){
            underlying[key] = value
            true
        } else {
            false
        }
    }

    override suspend fun modify(key: K, operation: Operation<V>, condition: Condition<V>): V? {
        val current = underlying[key]
        return if(current != null && condition.invoke(current)){
            val newItem = operation.invoke(current)
            underlying[key] = newItem
            newItem
        } else {
            null
        }
    }

    override suspend fun remove(key: K, condition: Condition<V>): Boolean {
        val current = underlying[key]
        return if(current != null && condition.invoke(current)){
            underlying.remove(key)
            true
        } else {
            false
        }
    }

    override suspend fun query(condition: Condition<V>, sortedBy: Sort<V>, after: V?, count: Int): List<V> {
        var afterFound = false
        return underlying.values.asSequence()
                .filter{ condition.invoke(it) }
                .sortedWith(sortedBy)
                .let{
                    if(after == null) it else {
                        it.dropWhile {
                            if(it == after){
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