package com.lightningkite.mirror.archive

import com.lightningkite.mirror.archive.model.Condition
import com.lightningkite.mirror.archive.model.Operation
import com.lightningkite.mirror.archive.model.Sort
import com.lightningkite.mirror.archive.property.FileSuspendProperty
import com.lightningkite.mirror.archive.property.RamSuspendProperty
import com.lightningkite.mirror.archive.property.SuspendProperty
import com.lightningkite.mirror.info.IntMirror
import com.lightningkite.mirror.test.IntData
import com.lightningkite.mirror.test.IntDataMirror
import com.lightningkite.mirror.test.Zoo
import com.lightningkite.mirror.test.ZooMirror
import kotlinx.coroutines.runBlocking
import java.io.File
import java.util.concurrent.atomic.AtomicReference
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

class FileSuspendPropertyTest {

    lateinit var property: SuspendProperty<Int>

    @BeforeTest
    fun before() {
        val file = File("./build/run/fileProperties")
        if(file.exists()){
            file.delete()
        }
        property = FileSuspendProperty<Int>(file, IntMirror, 0)
    }

    @AfterTest
    fun after() {
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
            println("setSucceeding")
            //Try a set
            property.set(42)

            println("It's currently ${property.get()}, should be 42")

            //Do a succeeding change
            val result = property.compareAndSet(42, 9)

            println("Result: It's currently ${property.get()}, should be 9 now")

            //Check that the change did take
            assert(property.get() == 9)
            assert(result)
        }
    }
}