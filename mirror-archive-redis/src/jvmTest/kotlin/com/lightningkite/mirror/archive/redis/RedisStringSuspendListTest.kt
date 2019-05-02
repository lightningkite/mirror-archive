package com.lightningkite.mirror.archive.redis

import com.lightningkite.mirror.archive.list.SuspendList
import com.lightningkite.mirror.info.IntMirror
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test

class RedisStringSuspendListTest {

    lateinit var list: SuspendList<Int>

    @BeforeTest
    fun before() {
        val redis = EmbeddedRedis.start()
        list = RedisStringSuspendList<Int>(
                redis = redis.connect(),
                serializer = Json.plain,
                key = "Test",
                mirror = IntMirror
        )
    }

    @AfterTest
    fun after() {
        EmbeddedRedis.stop()
    }

    @Test
    fun pushStart() {
        runBlocking {
            list.pushStart(1)
            assert(list.get(0) == 1)
            list.pushStart(2)
            assert(list.get(0) == 2)
            assert(list.get(1) == 1)
        }
    }

    @Test
    fun pushEnd() {
        runBlocking {
            list.pushEnd(1)
            assert(list.get(0) == 1)
            list.pushEnd(2)
            assert(list.get(0) == 1)
            assert(list.get(1) == 2)
        }
    }

    @Test
    fun popStart() {
        runBlocking {
            list.pushEnd(1)
            list.pushEnd(2)
            assert(list.popStart() == 1)
        }
    }

    @Test
    fun popEnd() {
        runBlocking {
            list.pushEnd(1)
            list.pushEnd(2)
            assert(list.popEnd() == 2)
        }
    }

    @Test
    fun size() {
        runBlocking {
            list.pushEnd(1)
            list.pushEnd(2)
            assert(list.size() == 2)
        }
    }

    @Test
    fun set() {
        runBlocking {
            list.pushEnd(1)
            list.pushEnd(2)
            list.pushEnd(3)
            list.set(1, 42)
            assert(list.get(1) == 42)
        }
    }

    @Test
    fun getRange() {
        runBlocking {
            list.pushEnd(1)
            list.pushEnd(2)
            list.pushEnd(3)
            list.pushEnd(4)
            assert(list.getRange(0 .. 2).toIntArray() contentEquals intArrayOf(1, 2, 3))
        }
    }

    @Test
    fun trim() {
        runBlocking {
            list.pushEnd(1)
            list.pushEnd(2)
            list.pushEnd(3)
            list.pushEnd(4)
            list.trim(1 .. 2)
            assert(list.size() == 2)
            assert(list.get(0) == 2)
            assert(list.get(1) == 3)
        }
    }
}