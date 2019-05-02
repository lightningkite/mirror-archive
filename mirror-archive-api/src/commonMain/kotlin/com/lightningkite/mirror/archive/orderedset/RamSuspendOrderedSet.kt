package com.lightningkite.mirror.archive.orderedset

import com.lightningkite.kommon.collection.SortedBag


class RamSuspendOrderedSet<T: Any>(val score: (T)->Double): SuspendOrderedSet<T> {
    val underlying = SortedBag<T>(Comparator { a: T, b: T ->
        a.let(score).compareTo(b.let(score))
    })

    override suspend fun add(value: T) {
        underlying.add(value)
    }

    override suspend fun remove(value: T): Boolean {
        return underlying.remove(value)
    }

    override suspend fun size(): Int {
        return underlying.size
    }

    override suspend fun popMax(): T? {
        return underlying.popLast()
    }

    override suspend fun popMin(): T? {
        return underlying.popFirst()
    }

    override suspend fun popMax(count: Int): List<T> = (0 until count).asSequence().mapNotNull { underlying.popLast() }.toList()

    override suspend fun popMin(count: Int): List<T> = (0 until count).asSequence().mapNotNull { underlying.popFirst() }.toList()
}