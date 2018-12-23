package com.lightningkite.mirror.archive.database

import com.lightningkite.mirror.archive.model.Condition

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