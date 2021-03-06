package com.lightningkite.mirror.archive.database

import com.lightningkite.mirror.info.Type

interface SuspendMapProvider {
    fun <K, V: Any> suspendMap(key: Type<K>, value: Type<V>, name: String? = null): SuspendMap<K, V>
}