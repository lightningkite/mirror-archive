package com.lightningkite.mirror.archive.database

import com.lightningkite.mirror.archive.model.*

interface SuspendMap<K, V : Any> {
    suspend fun getNewKey(): K

    suspend fun get(key: K): V?
    suspend fun put(
            key: K,
            value: V,
            conditionIfExists: Condition<V> = Condition.Always(),
            create: Boolean = true
    ): Boolean

    suspend fun modify(
            key: K,
            operation: Operation<V>,
            condition: Condition<V> = Condition.Always()
    ): V? {
        val current = get(key) ?: return null
        val modified = operation.invoke(current)
        return if(put(key, modified, Condition.Equal(current), false))
            modified
        else
            null
    }

    suspend fun remove(
            key: K,
            condition: Condition<V> = Condition.Always()
    ): Boolean

    suspend fun find(
            condition: Condition<V>,
            sortedBy: Sort<V> = Sort.DontCare()
    ): V? = query(
            condition = condition,
            sortedBy = sortedBy,
            count = 1
    ).firstOrNull()

    suspend fun getMany(keys: Iterable<K>): Map<K, V?> = keys.associate { it to get(it) }
    suspend fun putMany(map: Map<K, V>) = map.entries.forEach { put(it.key, it.value) }
    suspend fun removeMany(keys: Iterable<K>) = keys.forEach { remove(it) }
    suspend fun query(
            condition: Condition<V> = Condition.Always(),
            sortedBy: Sort<V> = Sort.DontCare(),
            after: V? = null,
            count: Int = 100
    ): List<V>
}

//Traditional database: SuspendMap<Id, HasId>
//Log: SuspendMap<TimeStamp, Any> OR SuspendMap<Id, HasIdAndTimestamp>
//Redis Cache: SuspendMap<String, Any>