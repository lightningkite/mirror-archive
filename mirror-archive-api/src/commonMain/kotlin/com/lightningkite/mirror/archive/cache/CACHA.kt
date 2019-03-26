package com.lightningkite.mirror.archive.cache


interface SuspendList<T> {
    suspend fun pushStart(item: T)
    suspend fun pushEnd(item: T)
    suspend fun popStart(): T
    suspend fun popEnd(): T
    suspend fun get(index: Int): T
    suspend fun set(index: Int, value: T)
    suspend fun size(): Int
    suspend fun trim(range: IntRange)
    suspend fun insertBefore(otherValue: T, value: T)
    suspend fun insertAfter(otherValue: T, value: T)
    suspend fun getRange(range: IntRange): List<T>
}

interface SuspendOrderedSet<T> {
    suspend fun add(value: T)
    suspend fun remove(value: T)
    suspend fun size(): Int

    /**
     * @param rank - Negative ranks start from the end, zero is the first item
     **/
    suspend fun get(rank: Int): T

    /**
     * @param rank - Negative ranks start from the end, zero is the first item
     **/
    suspend fun removeByRank(rank: Int): List<T>

    /**
     * @param low - Negative ranks start from the end, zero is the first item
     * @param high - Negative ranks start from the end, zero is the first item
     **/
    suspend fun removeByRankRange(low: Int, high: Int): List<T>
}