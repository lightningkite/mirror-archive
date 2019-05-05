package com.lightningkite.mirror.serialization.json

import com.lightningkite.kommon.bytes.toStringHex
import com.lightningkite.lokalize.time.TimeStamp
import com.lightningkite.lokalize.time.TimeStampMirror
import com.lightningkite.mirror.archive.flatarray.BinaryFlatArrayDecoder
import com.lightningkite.mirror.archive.flatarray.BinaryFlatArrayFormat
import com.lightningkite.mirror.archive.flatarray.IndexPath
import com.lightningkite.mirror.info.*
import com.lightningkite.mirror.registerTest
import com.lightningkite.mirror.test.*
import com.lightningkite.recktangle.Point
import com.lightningkite.recktangle.PointMirror
import kotlin.test.Test
import kotlin.test.assertEquals

class BinaryFlatArrayTest {

    init {
        registerTest()
    }

    fun <T> test(value: T, type: MirrorType<T>): T {
        val result = BinaryFlatArrayFormat.toArray(type, value)
        val schema = BinaryFlatArrayFormat.columns(type)
        schema.indices
                .asSequence()
                .map { schema.getOrNull(it) to result.getOrNull(it) }
                .forEachIndexed { index, (col, it) ->
                    println("    $index: ${col?.name} = ${if (it is ByteArray) {
                        it.toStringHex()
                    } else it.toString()}")
                }
        val decoder = BinaryFlatArrayDecoder(BinaryFlatArrayFormat.context, BinaryFlatArrayFormat.binaryFormat, result){false}
        val back = decoder.decodeSerializableValue(type)
        assertEquals(schema.size, result.size)
        assertEquals(decoder.currentIndex, result.size)
        assertEquals(value, back)
        return back
    }

    @Test
    fun listString() {
        println("TEST - listString")
        val map = BinaryFlatArrayFormat.toArray(IntMirror.list, listOf(1, 2, 3, 4))
        println((map[0] as ByteArray).toStringHex())
        val back = BinaryFlatArrayFormat.fromArray(IntMirror.list, map)
        println(back)
    }

    @Test
    fun basicsTest() {
        println("TEST - basicsTest")
        test(listOf(1, 2, 3, 4), IntMirror.list)
        test(mapOf(
                "a" to 1,
                "b" to 2,
                "c" to 3
        ), StringMirror mapTo IntMirror)
        test(mapOf(
                "a" to listOf(1, 8),
                "b" to listOf(2, 8),
                "c" to listOf(3, 8)
        ), StringMirror mapTo IntMirror.list)
    }

    @Test
    fun nullables() {
        println("TEST - nullables")
        test<String?>(null, StringMirror.nullable)
        test<String?>("value", StringMirror.nullable)
        test(listOf(null, "Has String", null, "another"), StringMirror.nullable.list)
    }

    @Test
    fun reflective() {
        println("TEST - reflective")
        test(Post(0, 42, "hello"), PostMirror)
        test(Zoo.instance(), ZooMirror)
        test(Zoo.zero(), ZooMirror)
    }

    @Test
    fun reflectiveWithSkip() {
        println("TEST - reflective")
        val value = Zoo.zero()
        val type = ZooMirror
        val schema = BinaryFlatArrayFormat.columns(type)
        val result = BinaryFlatArrayFormat.toArray(type, value)

        println("First: ${result.joinToString()}")
        val resultWithNulls = result.mapIndexed { index, any ->
            val col = schema[index]
            val otherName = col.name + "_null"
            val otherIndex = schema.indexOfFirst { it.name == otherName }
            if(otherIndex != -1 && result[otherIndex] == true) {
                null
            } else any
        }
        println("With nulls: ${resultWithNulls.joinToString()}")

        val decoder = BinaryFlatArrayDecoder(BinaryFlatArrayFormat.context, BinaryFlatArrayFormat.binaryFormat, resultWithNulls){false}
        val back = decoder.decodeSerializableValue(type)
        assertEquals(decoder.currentIndex, resultWithNulls.size)
        assertEquals(value, back)
    }

    @Test
    fun reflectivePartialShallow() {
        println("TEST - reflectivePartialShallow")
        val default = Zoo.instance()
        val path = IndexPath(intArrayOf(ZooMirror.fieldInt.index))
        val encoded = BinaryFlatArrayFormat.toArrayPartial(ZooMirror, default, 333, path)
        println("Encoded int to $encoded")
        assertEquals(333, encoded[0])
    }

    @Test
    fun reflectivePartialMultivalue() {
        println("TEST - reflectivePartialMultivalue")
        val default = Zoo.instance()
        val path = IndexPath(intArrayOf(ZooMirror.fieldIntData.index))
        val encoded = BinaryFlatArrayFormat.toArrayPartial(ZooMirror, default, IntData(333), path)
        println("Encoded intData to $encoded")
        assertEquals(333, encoded[0])
    }

    @Test
    fun reflectivePartialDeep() {
        println("TEST - reflectivePartialDeep")
        val default = Zoo.instance()
        val path = IndexPath(intArrayOf(ZooMirror.fieldIntData.index, IntDataMirror.fieldIntV.index))
        val encoded = BinaryFlatArrayFormat.toArrayPartial(ZooMirror, default, 333, path)
        println("Encoded intData.intV to $encoded")
        assertEquals(333, encoded[0])
    }

    @Test
    fun polymorphic() {
        println("TEST - polymorphic")
        test(Post(0, 42, "hello"), AnyMirror)
        test(listOf(Post(0, 42, "hello")), AnyMirror)
        test(8, AnyMirror)
        test("hello", AnyMirror)
    }

    @Test
    fun types() {
        println("TEST - types")
        test(listOf<Any>(
                Unit,
                true,
                false,
                'c',
                "string",
                1.toByte(),
                1.toShort(),
                1,
                1L,
                1f,
                1.0
        ), AnyMirror)
    }

    @Test
    fun enumTest() {
        println("TEST - enumTest")
        test(TestEnum.ValueA, TestEnumMirror)
        test(TestEnum.ValueB, TestEnumMirror)
        test(TestEnum.ValueC, TestEnumMirror)
    }

    @Test
    fun reflectiveData() {
        println("TEST - reflectiveData")
        test(PostMirror.fieldId, MirrorClassFieldMirror(PostMirror, LongMirror.nullable))
        test(TestEnumMirror, MirrorClassMirror(TestEnumMirror))
    }

    @Test
    fun externalClass() {
        println("TEST - externalClass")
        test(Point(1f, 2f), PointMirror)
    }

    @Test
    fun inlinedClass() {
        println("TEST - inlinedClass")
        test(TimeStamp(41782934718L), TimeStampMirror)
    }
}
