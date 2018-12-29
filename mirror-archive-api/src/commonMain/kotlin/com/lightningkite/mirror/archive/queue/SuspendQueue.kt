package com.lightningkite.mirror.archive.queue

import com.lightningkite.mirror.archive.database.SuspendMap
import com.lightningkite.mirror.archive.database.SuspendMapProvider
import com.lightningkite.mirror.archive.model.Condition
import com.lightningkite.mirror.archive.model.Operation
import com.lightningkite.mirror.info.Type
import com.lightningkite.mirror.info.type

class SuspendQueue<V: Any>(
        val name: String,
        val cursors: SuspendMap<String, Int>,
        val items: SuspendMap<Int, V>
) {
    constructor(provider: SuspendMapProvider, itemType: Type<V>, name: String):this(
            name = name,
            cursors = provider.suspendMap(String::class.type, Int::class.type),
            items = provider.suspendMap(Int::class.type, itemType)
    )

    val enqueueCursor = "$name enqueue cursor"
    val dequeueCursor = "$name dequeue cursor"

    var isSetUp: Boolean = false
    suspend fun setup(){
        if(isSetUp) return
        cursors.put(enqueueCursor, 0, Condition.Never())
        cursors.put(dequeueCursor, 0, Condition.Never())
        isSetUp = true
    }

    suspend fun size(): Int {
        return (cursors.get(enqueueCursor) ?: 0) - (cursors.get(dequeueCursor) ?: 0)
    }

    suspend fun enqueue(item: V): Int {
        val newKey = cursors.modify(enqueueCursor, Operation.AddInt(1))!!
        items.put(newKey, item)
        return newKey
    }

    suspend fun dequeue(): Pair<Int, V>? {
        val mostRecentKey = cursors.get(enqueueCursor) ?: 0
        val retrieveKey = cursors.modify(dequeueCursor, Operation.AddInt(1), Condition.LessThanOrEqual(mostRecentKey)) ?: return null
        val result = items.get(retrieveKey)?.let{ retrieveKey to it } ?: return null
        items.remove(retrieveKey)
        return result
    }
}