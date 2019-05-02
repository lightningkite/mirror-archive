package com.lightningkite.mirror.archive.list

interface SuspendList<T> {
    suspend fun pushStart(item: T)
    suspend fun pushEnd(item: T)
    suspend fun popStart(): T?
    suspend fun popEnd(): T?
    suspend fun get(index: Int): T?
    suspend fun set(index: Int, value: T)
    suspend fun size(): Int
    suspend fun trim(range: IntRange)
    suspend fun getRange(range: IntRange): List<T>
}