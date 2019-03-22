package com.lightningkite.mirror.archive.flatarray

import com.lightningkite.kommon.bytes.toStringHex
import kotlinx.io.ByteArrayInputStream
import kotlinx.io.ByteArrayOutputStream
import kotlinx.serialization.*
import kotlinx.serialization.cbor.Cbor
import kotlinx.serialization.context.SerialContext
import kotlinx.serialization.internal.EnumDescriptor

class FlatArrayDecoder(
        override val context: SerialContext,
        val input: List<Any?>,
        var currentIndex: Int = 0
) : Decoder, CompositeDecoder {
    override val updateMode: UpdateMode get() = UpdateMode.UPDATE

    val seen = ArrayList<String>()

    override fun decodeBoolean(): Boolean = input[currentIndex++] as Boolean
    override fun decodeByte(): Byte = input[currentIndex++] as Byte
    override fun decodeChar(): Char = input[currentIndex++] as Char
    override fun decodeFloat(): Float = input[currentIndex++] as Float
    override fun decodeInt(): Int = input[currentIndex++] as Int
    override fun decodeLong(): Long = input[currentIndex++] as Long
    override fun decodeDouble(): Double = input[currentIndex++] as Double
    override fun decodeShort(): Short = input[currentIndex++] as Short
    override fun decodeString(): String = input[currentIndex++] as String
    override fun decodeUnit() {}
    override fun decodeNull(): Nothing? {
        currentIndex++
        return null
    }

    override fun decodeEnum(enumDescription: EnumDescriptor): Int {
        return enumDescription.getElementIndex(decodeString())
    }

    override fun decodeNotNullMark(): Boolean = !(input[currentIndex++] as Boolean)

    override fun decodeBooleanElement(desc: SerialDescriptor, index: Int): Boolean = input[currentIndex++] as Boolean
    override fun decodeByteElement(desc: SerialDescriptor, index: Int): Byte = input[currentIndex++] as Byte
    override fun decodeCharElement(desc: SerialDescriptor, index: Int): Char = input[currentIndex++] as Char
    override fun decodeDoubleElement(desc: SerialDescriptor, index: Int): Double = input[currentIndex++] as Double
    override fun decodeFloatElement(desc: SerialDescriptor, index: Int): Float = input[currentIndex++] as Float
    override fun decodeIntElement(desc: SerialDescriptor, index: Int): Int = input[currentIndex++] as Int
    override fun decodeLongElement(desc: SerialDescriptor, index: Int): Long = input[currentIndex++] as Long
    override fun decodeShortElement(desc: SerialDescriptor, index: Int): Short = input[currentIndex++] as Short
    override fun decodeStringElement(desc: SerialDescriptor, index: Int): String = input[currentIndex++] as String
    override fun decodeUnitElement(desc: SerialDescriptor, index: Int) {}

    fun skipSerializableValue(desc: SerialDescriptor) {
        when (desc.kind) {
            PrimitiveKind.UNIT -> {
            }
            is PrimitiveKind, UnionKind.ENUM_KIND -> currentIndex++
            StructureKind.CLASS -> {
                if (desc.name in seen) {
                    currentIndex++
                } else {
                    seen.add(desc.name)
                    for (index in 0 until desc.elementsCount) {
                        skipSerializableValue(desc.getElementDescriptor(index))
                    }
                    seen.removeAt(seen.lastIndex)
                }
            }
            else -> currentIndex++
        }
    }

    override fun <T : Any> decodeNullableSerializableValue(deserializer: DeserializationStrategy<T?>): T? {
        val isNull = input[currentIndex++] as Boolean
        return if (isNull) {
            skipSerializableValue(deserializer.descriptor)
            null
        } else {
            decodeSerializableValue(deserializer)
        }
    }

    override fun <T> decodeSerializableValue(deserializer: DeserializationStrategy<T>): T {
        return when (deserializer.descriptor.kind) {
            PrimitiveKind.UNIT,
            PrimitiveKind.INT,
            PrimitiveKind.BOOLEAN,
            PrimitiveKind.BYTE,
            PrimitiveKind.SHORT,
            PrimitiveKind.LONG,
            PrimitiveKind.FLOAT,
            PrimitiveKind.DOUBLE,
            PrimitiveKind.CHAR,
            PrimitiveKind.STRING,
            UnionKind.ENUM_KIND -> {
                deserializer.deserialize(this)
            }
            StructureKind.CLASS -> {
                val desc = deserializer.descriptor
                if (desc.isNullable) {
                    deserializer.deserialize(this)
                } else if (desc.name in seen) {
                    val binary = input[currentIndex++] as ByteArray
                    Cbor.load(deserializer, binary)
                } else {
                    seen.add(desc.name)
                    val result = deserializer.deserialize(this)
                    seen.removeAt(seen.lastIndex)
                    result
                }
            }
            else -> {
                val binary = input[currentIndex++] as ByteArray
                Cbor.load(deserializer, binary)
            }
        }
    }

    val beginStructureIndexStack = IntArray(1024)
    var beginStructureIndexStackIndex = -1
    override fun beginStructure(desc: SerialDescriptor, vararg typeParams: KSerializer<*>): CompositeDecoder {
        return when (desc.kind) {
            StructureKind.CLASS -> {
                beginStructureIndexStackIndex++
                beginStructureIndexStack[beginStructureIndexStackIndex] = currentIndex
                this
            }
            else -> {
                val binary = input[currentIndex++] as ByteArray
                val stream = ByteArrayInputStream(binary)
                val decoder = CborCopy.CborDecoder(stream)
                val reader = CborCopy.plain.CborReader(decoder)
                return reader.beginStructure(desc, *typeParams)
            }
        }
    }

    override fun decodeElementIndex(desc: SerialDescriptor): Int = CompositeDecoder.READ_ALL//currentIndex - beginStructureIndexStack[beginStructureIndexStackIndex]
    override fun endStructure(desc: SerialDescriptor) {
        beginStructureIndexStackIndex--
    }

    override fun <T : Any?> decodeSerializableElement(desc: SerialDescriptor, index: Int, deserializer: DeserializationStrategy<T>): T = decodeSerializableValue(deserializer)
    override fun <T : Any> decodeNullableSerializableElement(desc: SerialDescriptor, index: Int, deserializer: DeserializationStrategy<T?>): T? = decodeNullableSerializableValue(deserializer)
    override fun <T> updateSerializableElement(desc: SerialDescriptor, index: Int, deserializer: DeserializationStrategy<T>, old: T): T = updateSerializableValue(deserializer, old)
    override fun <T : Any> updateNullableSerializableElement(desc: SerialDescriptor, index: Int, deserializer: DeserializationStrategy<T?>, old: T?): T? = updateNullableSerializableValue(deserializer, old)
}