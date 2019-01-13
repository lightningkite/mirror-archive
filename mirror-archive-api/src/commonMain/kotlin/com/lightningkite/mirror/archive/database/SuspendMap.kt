package com.lightningkite.mirror.archive.database

import com.lightningkite.mirror.archive.model.Condition
import com.lightningkite.mirror.archive.model.Operation
import com.lightningkite.mirror.archive.model.Sort

interface SuspendMap<K, V : Any> {

    data class Entry<K, V: Any>(
            override val key: K,
            override val value: V
    ): Map.Entry<K, V>

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
            condition: Condition<V> = Condition.Always(),
            sortedBy: Sort<V>? = null
    ): SuspendMap.Entry<K, V>? = query(
            condition = condition,
            sortedBy = sortedBy,
            count = 1
    ).firstOrNull()

    suspend fun getMany(keys: List<K>): Map<K, V?> = keys.associate { it to get(it) }
    suspend fun putMany(map: Map<K, V>): Unit = map.entries.forEach { put(it.key, it.value) }
    suspend fun removeMany(keys: List<K>): Unit = keys.forEach { remove(it) }

    suspend fun query(
            condition: Condition<V> = Condition.Always(),
            keyCondition: Condition<K> = Condition.Always(),
            sortedBy: Sort<V>? = null,
            after: SuspendMap.Entry<K, V>? = null,
            count: Int = 100
    ): List<SuspendMap.Entry<K, V>>
}

//Traditional database: SuspendMap<Id, HasId>
//Log: SuspendMap<TimeStamp, Any> OR SuspendMap<Id, HasIdAndTimestamp>
//Redis Cache: SuspendMap<String, Any>