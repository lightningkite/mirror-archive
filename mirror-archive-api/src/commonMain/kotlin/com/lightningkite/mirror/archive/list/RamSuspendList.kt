package com.lightningkite.mirror.archive.list

class RamSuspendList<T>: SuspendList<T> {
    val underlying = ArrayList<T>()

    override suspend fun pushStart(item: T) {
        underlying.add(0, item)
    }
    override suspend fun pushEnd(item: T) {
        underlying.add(item)
    }
    override suspend fun popStart(): T? {
        if(underlying.isEmpty()) return null
        return underlying.removeAt(0)
    }
    override suspend fun popEnd(): T? {
        if(underlying.isEmpty()) return null
        return underlying.removeAt(underlying.lastIndex)
    }
    override suspend fun get(index: Int): T? {
        return underlying.getOrNull(index)
    }
    override suspend fun set(index: Int, value: T) {
        if(index in underlying.indices){
            underlying[index] = value
        }
    }
    override suspend fun size(): Int = underlying.size
    override suspend fun trim(range: IntRange) {
        repeat(underlying.size - range.endInclusive - 1) {
            underlying.removeAt(underlying.lastIndex)
        }
        repeat(range.start) {
            underlying.removeAt(0)
        }
    }
    override suspend fun getRange(range: IntRange): List<T> = underlying.subList(range.start, range.endInclusive + 1)
}