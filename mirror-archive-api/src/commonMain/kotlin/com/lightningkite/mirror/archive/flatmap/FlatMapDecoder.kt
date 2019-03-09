package com.lightningkite.mirror.flatmap

import com.lightningkite.mirror.cbor.CborCopy
import kotlinx.io.ByteArrayInputStream
import kotlinx.io.ByteArrayOutputStream
import kotlinx.serialization.*
import kotlinx.serialization.context.SerialContext
import kotlinx.serialization.internal.EnumDescriptor

class FlatMapDecoder(
        override val context: SerialContext,
        val map: Map<String, Any?>,
        val prefix: String = ""
) : Decoder, CompositeDecoder {
    override val updateMode: UpdateMode get() = UpdateMode.UPDATE

    override fun decodeBoolean(): Boolean = map[prefix] as Boolean
    override fun decodeByte(): Byte = map[prefix] as Byte
    override fun decodeChar(): Char = map[prefix] as Char
    override fun decodeFloat(): Float = map[prefix] as Float
    override fun decodeInt(): Int = map[prefix] as Int
    override fun decodeLong(): Long = map[prefix] as Long
    override fun decodeDouble(): Double = map[prefix] as Double
    override fun decodeShort(): Short = map[prefix] as Short
    override fun decodeString(): String = map[prefix] as String
    override fun decodeUnit() {}
    override fun decodeNull(): Nothing? = null

    override fun decodeEnum(enumDescription: EnumDescriptor): Int {
        return enumDescription.getElementIndex(decodeString())
    }

    override fun decodeNotNullMark(): Boolean = !(map[prefix nameCombine "null"] as Boolean)

    inline fun <T> decodeXElement(desc: SerialDescriptor, index: Int, action: (Any?) -> T): T {
        val result = action(map[prefix nameCombine desc.getElementName(index)])
        this.index = index + 1
        return result
    }

    override fun decodeBooleanElement(desc: SerialDescriptor, index: Int): Boolean = decodeXElement(desc, index) { it as Boolean }
    override fun decodeByteElement(desc: SerialDescriptor, index: Int): Byte = decodeXElement(desc, index) { it as Byte }
    override fun decodeCharElement(desc: SerialDescriptor, index: Int): Char = decodeXElement(desc, index) { it as Char }
    override fun decodeDoubleElement(desc: SerialDescriptor, index: Int): Double = decodeXElement(desc, index) { it as Double }
    override fun decodeFloatElement(desc: SerialDescriptor, index: Int): Float = decodeXElement(desc, index) { it as Float }
    override fun decodeIntElement(desc: SerialDescriptor, index: Int): Int = decodeXElement(desc, index) { it as Int }
    override fun decodeLongElement(desc: SerialDescriptor, index: Int): Long = decodeXElement(desc, index) { it as Long }
    override fun decodeShortElement(desc: SerialDescriptor, index: Int): Short = decodeXElement(desc, index) { it as Short }
    override fun decodeStringElement(desc: SerialDescriptor, index: Int): String = decodeXElement(desc, index) { it as String }
    override fun decodeUnitElement(desc: SerialDescriptor, index: Int) = decodeXElement(desc, index) { }


    var index = 0

    override fun beginStructure(desc: SerialDescriptor, vararg typeParams: KSerializer<*>): CompositeDecoder {
        return when (desc.kind) {
            StructureKind.CLASS -> FlatMapDecoder(context, map)
            else -> {
                val stream = ByteArrayInputStream(map[prefix] as ByteArray)
                val decoder = CborCopy.CborDecoder(stream)
                val reader = CborCopy.plain.CborReader(decoder)
                return reader.beginStructure(desc, *typeParams)
            }
        }
    }

    override fun endStructure(desc: SerialDescriptor) {
    }

    override fun <T : Any?> decodeSerializableElement(desc: SerialDescriptor, index: Int, deserializer: DeserializationStrategy<T>): T = decodeXElement(desc, index) {
        FlatMapDecoder(context, map, prefix nameCombine desc.getElementName(index)).decodeSerializableValue(deserializer)
    }

    override fun <T : Any> decodeNullableSerializableElement(desc: SerialDescriptor, index: Int, deserializer: DeserializationStrategy<T?>): T? = decodeXElement(desc, index) {
        FlatMapDecoder(context, map, prefix nameCombine desc.getElementName(index)).decodeNullableSerializableValue(deserializer)
    }

    override fun <T> updateSerializableElement(desc: SerialDescriptor, index: Int, deserializer: DeserializationStrategy<T>, old: T): T = decodeXElement(desc, index) {
        FlatMapDecoder(context, map, prefix nameCombine desc.getElementName(index)).updateSerializableValue(deserializer, old)
    }

    override fun <T : Any> updateNullableSerializableElement(desc: SerialDescriptor, index: Int, deserializer: DeserializationStrategy<T?>, old: T?): T? = decodeXElement(desc, index) {
        FlatMapDecoder(context, map, prefix nameCombine desc.getElementName(index)).updateNullableSerializableValue(deserializer, old)
    }

    override fun decodeElementIndex(desc: SerialDescriptor): Int = CompositeDecoder.READ_ALL
}