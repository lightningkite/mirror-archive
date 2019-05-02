package com.lightningkite.mirror.archive.redis

import com.lightningkite.mirror.archive.property.RamSuspendProperty
import com.lightningkite.mirror.archive.property.SuspendProperty
import com.lightningkite.mirror.info.IntMirror
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import org.junit.Test
import kotlin.test.assert
import kotlin.test.AfterTest
import kotlin.test.BeforeTest

class RedisStringSuspendPropertyTest {

    lateinit var property: SuspendProperty<Int>

    @BeforeTest
    fun before() {
        val redis = EmbeddedRedis.start()
        property = RedisStringSuspendProperty(
                redis = redis.connect(),
                serializer = Json.plain,
                key = "Test",
                mirror = IntMirror,
                default = 0
        )
    }

    @AfterTest
    fun after() {
        EmbeddedRedis.stop()
    }

    @Test
    fun defaultGet() {
        runBlocking {
            //Check default
            assert(property.get() == 0)
        }
    }

    @Test
    fun set() {
        runBlocking {
            //Try a set
            property.set(42)

            //Check that it worked
            assert(property.get() == 42)
        }
    }

    @Test
    fun setFailing() {
        runBlocking {
            //Try a set
            property.set(42)

            //Do a failing change
            assert(!property.compareAndSet(0, 9))

            //Check that the change didn't take
            assert(property.get() == 42)
        }
    }

    @Test
    fun setSucceeding() {
        runBlocking {
            //Try a set
            property.set(42)

            //Do a succeeding change
            assert(property.compareAndSet(42, 9))

            //Check that the change did take
            assert(property.get() == 9)
        }
    }
}
