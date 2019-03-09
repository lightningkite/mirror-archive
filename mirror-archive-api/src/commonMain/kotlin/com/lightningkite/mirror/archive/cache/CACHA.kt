package com.lightningkite.mirror.archive.cache


interface SuspendList<T> {
    suspend fun pushLeft(item: T)
    suspend fun pushRight(item: T)
    suspend fun popLeft(): T
    suspend fun popRight(): T
    suspend fun get(index: Int): T
    suspend fun set(index: Int, value: T)
    suspend fun size(): Int
    suspend fun trim(range: IntRange)
    suspend fun insertBefore(otherValue: T, value: T)
    suspend fun insertAfter(otherValue: T, value: T)
    suspend fun getRange(range: IntRange): List<T>
}

interface SuspendSet<T> {
    suspend fun add(score: Int, value: T)
    suspend fun size(): Int
    /**
     * @param rank - Negative ranks start from the end, zero is the first item
     **/
    suspend fun get(rank: Int): T

    suspend fun removeWithRange(range: IntRange): List<T>
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