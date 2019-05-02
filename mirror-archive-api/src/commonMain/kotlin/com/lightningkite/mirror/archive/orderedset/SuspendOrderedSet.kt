package com.lightningkite.mirror.archive.orderedset


interface SuspendOrderedSet<T: Any> {
    suspend fun add(value: T)
    suspend fun remove(value: T): Boolean
    suspend fun size(): Int

    suspend fun popMax(): T?
    suspend fun popMin(): T?
    suspend fun popMax(count: Int): List<T>
    suspend fun popMin(count: Int): List<T>
}