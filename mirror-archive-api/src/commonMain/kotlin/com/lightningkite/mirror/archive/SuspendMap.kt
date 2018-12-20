package com.lightningkite.mirror.archive

interface SuspendMap<K, V> {
    suspend fun get(key: K):V?
    suspend fun set(key: K, value: V): V?
    suspend fun replace(key: K, old: V?, new: V): Boolean
}