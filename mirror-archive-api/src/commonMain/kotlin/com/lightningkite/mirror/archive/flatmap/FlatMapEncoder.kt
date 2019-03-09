package com.lightningkite.mirror.flatmap

import com.lightningkite.mirror.cbor.CborCopy
import kotlinx.io.ByteArrayOutputStream
import kotlinx.serialization.*
import kotlinx.serialization.context.SerialContext
import kotlinx.serialization.internal.EnumDescriptor

//C:\Users\josep\Documents\testKotlin

class FlatMapEncoder(
        override val context: SerialContext,
        val map: MutableMap<String, Any?>,
        val prefix: String = ""
) : Encoder, CompositeEncoder {
    override fun beginStructure(desc: SerialDescriptor, vararg typeParams: KSerializer<*>): CompositeEncoder {
        return when (desc.kind) {
            StructureKind.CLASS -> FlatMapEncoder(context, map)
            else -> {
                val outputStream = ByteArrayOutputStream()
                val encoder = CborCopy.CborEncoder(outputStream)
                val writer = CborCopy.plain.CborWriter(encoder)
                val underlying = writer.beginStructure(desc, *typeParams)
                return object : CompositeEncoder by underlying {
                    override fun endStructure(desc: SerialDescriptor) {
                        underlying.endStructure(desc)
                        map[prefix] = outputStream.toByteArray()
                    }
                }
            }
        }
    }

    override fun encodeBooleanElement(desc: SerialDescriptor, index: Int, value: Boolean) {
        map[prefix nameCombine desc.getElementName(index)] = value
    }

    override fun encodeByteElement(desc: SerialDescriptor, index: Int, value: Byte) {
        map[prefix nameCombine desc.getElementName(index)] = value
    }

    override fun encodeCharElement(desc: SerialDescriptor, index: Int, value: Char) {
        map[prefix nameCombine desc.getElementName(index)] = value
    }

    override fun encodeDoubleElement(desc: SerialDescriptor, index: Int, value: Double) {
        map[prefix nameCombine desc.getElementName(index)] = value
    }

    override fun encodeFloatElement(desc: SerialDescriptor, index: Int, value: Float) {
        map[prefix nameCombine desc.getElementName(index)] = value
    }

    override fun encodeIntElement(desc: SerialDescriptor, index: Int, value: Int) {
        map[prefix nameCombine desc.getElementName(index)] = value
    }

    override fun encodeLongElement(desc: SerialDescriptor, index: Int, value: Long) {
        map[prefix nameCombine desc.getElementName(index)] = value
    }

    override fun encodeShortElement(desc: SerialDescriptor, index: Int, value: Short) {
        map[prefix nameCombine desc.getElementName(index)] = value
    }

    override fun encodeStringElement(desc: SerialDescriptor, index: Int, value: String) {
        map[prefix nameCombine desc.getElementName(index)] = value
    }

    override fun encodeUnitElement(desc: SerialDescriptor, index: Int) {}

    override fun encodeNonSerializableElement(desc: SerialDescriptor, index: Int, value: Any) {
        map[prefix nameCombine desc.getElementName(index)] = value
    }

    override fun <T : Any> encodeNullableSerializableElement(desc: SerialDescriptor, index: Int, serializer: SerializationStrategy<T>, value: T?) {
        val newPrefix = prefix nameCombine desc.getElementName(index)
        if (value == null) {
            map[newPrefix] = false
        } else {
            map[newPrefix] = true
            serializer.serialize(FlatMapEncoder(context, map, newPrefix), value)
        }
    }

    override fun <T> encodeSerializableElement(desc: SerialDescriptor, index: Int, serializer: SerializationStrategy<T>, value: T) {
        serializer.serialize(FlatMapEncoder(context, map, prefix nameCombine desc.getElementName(index)), value)
    }

    override fun encodeBoolean(value: Boolean) {
        map[prefix] = value
    }

    override fun encodeByte(value: Byte) {
        map[prefix] = value
    }

    override fun encodeChar(value: Char) {
        map[prefix] = value
    }

    override fun encodeDouble(value: Double) {
        map[prefix] = value
    }

    override fun encodeFloat(value: Float) {
        map[prefix] = value
    }

    override fun encodeInt(value: Int) {
        map[prefix] = value
    }

    override fun encodeLong(value: Long) {
        map[prefix] = value
    }

    override fun encodeShort(value: Short) {
        map[prefix] = value
    }

    override fun encodeString(value: String) {
        map[prefix] = value
    }

    override fun encodeUnit() {}

    override fun encodeNull() {
        map[prefix nameCombine "null"] = true
    }

    override fun encodeEnum(enumDescription: EnumDescriptor, ordinal: Int) {
        map[prefix] = enumDescription.getElementName(ordinal)
    }

    override fun encodeNotNullMark() {
        map[prefix nameCombine "null"] = false
    }
}