package com.lightningkite.mirror.archive

import com.lightningkite.mirror.archive.orderedset.RamSuspendOrderedSet
import com.lightningkite.mirror.archive.orderedset.SuspendOrderedSet
import kotlinx.coroutines.runBlocking
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test

class RamSuspendOrderedSetTest {

    lateinit var set: SuspendOrderedSet<Int>

    @BeforeTest
    fun before() {
        set = RamSuspendOrderedSet<Int> { it.toDouble() }
    }

    @AfterTest
    fun after() {
    }

    @Test
    fun add() {
        runBlocking {
            set.add(1)
            set.add(2)
            set.add(3)
        }
    }

    @Test
    fun popMax() {
        runBlocking {
            set.add(1)
            set.add(2)
            set.add(3)
            assert(set.popMax() == 3)
            assert(set.popMax() == 2)
            assert(set.popMax() == 1)
            assert(set.popMax() == null)
        }
    }

    @Test
    fun popMin() {
        runBlocking {
            set.add(1)
            set.add(2)
            set.add(3)
            assert(set.popMin() == 1)
            assert(set.popMin() == 2)
            assert(set.popMin() == 3)
            assert(set.popMin() == null)
        }
    }

    @Test
    fun popMaxMulti() {
        runBlocking {
            set.add(1)
            set.add(2)
            set.add(3)
            assert(set.popMax(2).toIntArray() contentEquals intArrayOf(3, 2))
            assert(set.popMax(2).toIntArray() contentEquals intArrayOf(1))
        }
    }

    @Test
    fun popMinMulti() {
        runBlocking {
            set.add(1)
            set.add(2)
            set.add(3)
            assert(set.popMin(2).toIntArray() contentEquals intArrayOf(1, 2))
            assert(set.popMin(2).toIntArray() contentEquals intArrayOf(3))
        }
    }

    @Test
    fun size() {
        runBlocking {
            set.add(1)
            assert(set.size() == 1)
            set.add(2)
            assert(set.size() == 2)
            set.add(3)
            assert(set.size() == 3)
        }
    }

}