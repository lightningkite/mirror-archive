package com.lightningkite.mirror.archive.database

import com.lightningkite.mirror.archive.model.Condition
import com.lightningkite.mirror.archive.model.HasId
import com.lightningkite.mirror.archive.model.Uuid

suspend fun <K, V: Any> SuspendMap<K, V>.insert(
        value: V,
        conditionIfExists: Condition<V> = Condition.Always(),
        create: Boolean = true
): K? {
    val newKey = getNewKey()
    return if(put(newKey, value, conditionIfExists, create)) {
        newKey
    } else null
}

suspend fun <V : HasId> SuspendMap<Uuid, V>.insert(
        value: V,
        conditionIfExists: Condition<V> = Condition.Always(),
        create: Boolean = true
) = put(value.id, value, conditionIfExists, create)