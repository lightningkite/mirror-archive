package com.lightningkite.mirror.archive.redis

import com.lightningkite.mirror.archive.orderedset.SuspendOrderedSet
import com.lightningkite.mirror.info.IntMirror
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.cbor.Cbor
import kotlinx.serialization.json.Json
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test

class RedisBinarySuspendOrderedSetTest {

    lateinit var set: SuspendOrderedSet<Int>

    @BeforeTest
    fun before() {
        val redis = EmbeddedRedis.start()
        set = RedisBinarySuspendOrderedSet<Int>(
                redis = redis.connect(StringByteArrayCodec),
                serializer = Cbor.plain,
                key = "Test",
                mirror = IntMirror,
                getZ = { it.toDouble() }
        )
    }

    @AfterTest
    fun after() {
        EmbeddedRedis.stop()
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