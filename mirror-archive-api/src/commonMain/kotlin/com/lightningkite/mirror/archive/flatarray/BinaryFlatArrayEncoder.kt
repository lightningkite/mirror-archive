package com.lightningkite.mirror.archive.flatarray

import kotlinx.io.ByteArrayOutputStream
import kotlinx.serialization.*
import kotlinx.serialization.internal.EnumDescriptor
import kotlinx.serialization.modules.SerialModule

open class BinaryFlatArrayEncoder(
        override val context: SerialModule,
        val binaryFormat: BinaryFormat,
        val output: MutableList<Any?>,
        val terminateAt: (SerialDescriptor)->Boolean
) : Encoder, CompositeEncoder {
    override fun beginStructure(desc: SerialDescriptor, vararg typeParams: KSerializer<*>): CompositeEncoder {
        return when (desc.kind) {
            StructureKind.CLASS -> this
            else -> throw IllegalArgumentException()
        }
    }

    inline fun write(value: Any?) {
        output.add(value)
    }

    val seen = ArrayList<String>()

    fun skipSerializableValue(desc: SerialDescriptor) {
        if(terminateAt(desc)) write("")
        else when (desc.kind) {
            PrimitiveKind.UNIT -> {
            }
            PrimitiveKind.INT -> write(0)
            PrimitiveKind.BOOLEAN -> write(false)
            PrimitiveKind.BYTE -> write(0.toByte())
            PrimitiveKind.SHORT -> write(0.toShort())
            PrimitiveKind.LONG -> write(0L)
            PrimitiveKind.FLOAT -> write(0f)
            PrimitiveKind.DOUBLE -> write(0.0)
            PrimitiveKind.CHAR -> write(' ')
            PrimitiveKind.STRING -> write("")
            UnionKind.ENUM_KIND -> write("")
            StructureKind.CLASS -> {
                if (desc.name in seen) {
                    write(ByteArray(0))
                } else {
                    seen.add(desc.name)
                    for (index in 0 until desc.elementsCount) {
                        skipSerializableValue(desc.getElementDescriptor(index))
                    }
                    seen.removeAt(seen.lastIndex)
                }
            }
            else -> write(ByteArray(0))
        }
    }

    override fun <T> encodeSerializableValue(serializer: SerializationStrategy<T>, value: T) {
        if(terminateAt(serializer.descriptor)) write(value)
        else when (serializer.descriptor.kind) {
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
                serializer.serialize(this, value)
            }
            StructureKind.CLASS -> {
                val desc = serializer.descriptor
                if (desc.isNullable) {
                    serializer.serialize(this, value)
                } else if (desc.name in seen) {
                    val binary = binaryFormat.dump(serializer, value)
                    write(binary)
                } else {
                    seen.add(desc.name)
                    serializer.serialize(this, value)
                    seen.removeAt(seen.lastIndex)
                }
            }
            else -> {
                val binary = binaryFormat.dump(serializer, value)
                write(binary)
            }
        }
    }

    override fun <T : Any> encodeNullableSerializableValue(serializer: SerializationStrategy<T>, value: T?) {
        if (value == null) {
            write(true)
            skipSerializableValue(serializer.descriptor)
        } else {
            write(false)
            encodeSerializableValue(serializer, value)
        }
    }

    override fun encodeBooleanElement(desc: SerialDescriptor, index: Int, value: Boolean) {
        write(value)
    }

    override fun encodeByteElement(desc: SerialDescriptor, index: Int, value: Byte) {
        write(value)
    }

    override fun encodeCharElement(desc: SerialDescriptor, index: Int, value: Char) {
        write(value)
    }

    override fun encodeDoubleElement(desc: SerialDescriptor, index: Int, value: Double) {
        write(value)
    }

    override fun encodeFloatElement(desc: SerialDescriptor, index: Int, value: Float) {
        write(value)
    }

    override fun encodeIntElement(desc: SerialDescriptor, index: Int, value: Int) {
        write(value)
    }

    override fun encodeLongElement(desc: SerialDescriptor, index: Int, value: Long) {
        write(value)
    }

    override fun encodeShortElement(desc: SerialDescriptor, index: Int, value: Short) {
        write(value)
    }

    override fun encodeStringElement(desc: SerialDescriptor, index: Int, value: String) {
        write(value)
    }

    override fun encodeUnitElement(desc: SerialDescriptor, index: Int) {}

    override fun encodeNonSerializableElement(desc: SerialDescriptor, index: Int, value: Any) {
        write(value)
    }

    override fun <T : Any> encodeNullableSerializableElement(desc: SerialDescriptor, index: Int, serializer: SerializationStrategy<T>, value: T?) {
        if (value == null) {
            write(true)
            skipSerializableValue(desc.getElementDescriptor(index))
        } else {
            write(false)
            encodeSerializableValue(serializer, value)
        }
    }

    override fun <T> encodeSerializableElement(desc: SerialDescriptor, index: Int, serializer: SerializationStrategy<T>, value: T) = encodeSerializableValue(serializer, value)

    override fun encodeBoolean(value: Boolean) {
        write(value)
    }

    override fun encodeByte(value: Byte) {
        write(value)
    }

    override fun encodeChar(value: Char) {
        write(value)
    }

    override fun encodeDouble(value: Double) {
        write(value)
    }

    override fun encodeFloat(value: Float) {
        write(value)
    }

    override fun encodeInt(value: Int) {
        write(value)
    }

    override fun encodeLong(value: Long) {
        write(value)
    }

    override fun encodeShort(value: Short) {
        write(value)
    }

    override fun encodeString(value: String) {
        write(value)
    }

    override fun encodeUnit() {}

    override fun encodeNull() {
        write(true)
    }

    override fun encodeNotNullMark() {
        write(false)
    }

    override fun encodeEnum(enumDescription: EnumDescriptor, ordinal: Int) {
        encodeString(enumDescription.getElementName(ordinal))
    }

    class Partial(
            context: SerialModule,
            binaryFormat: BinaryFormat,
            output: MutableList<Any?>,
            terminateAt: (SerialDescriptor)->Boolean,
            val selectedIndicies: IndexPath,
            val overridingValue: Any?
    ) : BinaryFlatArrayEncoder(context, binaryFormat, output, terminateAt) {

        var position: Int = 0

        inline fun encodeElement(desc: SerialDescriptor, index: Int, action: () -> Unit) {
            if (selectedIndicies[position] == index) {
                position++
                action()
                position--
            }
            //Otherwise do nothing; we'll prevent this.
        }

        override fun encodeBooleanElement(desc: SerialDescriptor, index: Int, value: Boolean) = encodeElement(desc, index) {
            super.encodeBooleanElement(desc, index, overridingValue as Boolean)
        }

        override fun encodeByteElement(desc: SerialDescriptor, index: Int, value: Byte) = encodeElement(desc, index) {
            super.encodeByteElement(desc, index, overridingValue as Byte)
        }

        override fun encodeCharElement(desc: SerialDescriptor, index: Int, value: Char) = encodeElement(desc, index) {
            super.encodeCharElement(desc, index, overridingValue as Char)
        }

        override fun encodeDoubleElement(desc: SerialDescriptor, index: Int, value: Double) = encodeElement(desc, index) {
            super.encodeDoubleElement(desc, index, overridingValue as Double)
        }

        override fun encodeFloatElement(desc: SerialDescriptor, index: Int, value: Float) = encodeElement(desc, index) {
            super.encodeFloatElement(desc, index, overridingValue as Float)
        }

        override fun encodeIntElement(desc: SerialDescriptor, index: Int, value: Int) = encodeElement(desc, index) {
            super.encodeIntElement(desc, index, overridingValue as Int)
        }

        override fun encodeLongElement(desc: SerialDescriptor, index: Int, value: Long) = encodeElement(desc, index) {
            super.encodeLongElement(desc, index, overridingValue as Long)
        }

        override fun encodeNonSerializableElement(desc: SerialDescriptor, index: Int, value: Any) = encodeElement(desc, index) {
            super.encodeNonSerializableElement(desc, index, overridingValue!!)
        }

        override fun <T : Any> encodeNullableSerializableElement(desc: SerialDescriptor, index: Int, serializer: SerializationStrategy<T>, value: T?) = encodeElement(desc, index) {
            super.encodeNullableSerializableElement(desc, index, serializer, if (position >= selectedIndicies.size) overridingValue as T else value)
        }

        override fun <T> encodeSerializableElement(desc: SerialDescriptor, index: Int, serializer: SerializationStrategy<T>, value: T) = encodeElement(desc, index) {
            super.encodeSerializableElement(desc, index, serializer, if (position >= selectedIndicies.size) overridingValue as T else value)
        }

        override fun encodeShortElement(desc: SerialDescriptor, index: Int, value: Short) = encodeElement(desc, index) {
            super.encodeShortElement(desc, index, overridingValue as Short)
        }

        override fun encodeStringElement(desc: SerialDescriptor, index: Int, value: String) = encodeElement(desc, index) {
            super.encodeStringElement(desc, index, overridingValue as String)
        }

        override fun encodeUnitElement(desc: SerialDescriptor, index: Int) = encodeElement(desc, index) {
            super.encodeUnitElement(desc, index)
        }

        override fun beginStructure(desc: SerialDescriptor, vararg typeParams: KSerializer<*>): CompositeEncoder {
            super.beginStructure(desc, *typeParams)
            return if (position >= selectedIndicies.size)
                BinaryFlatArrayEncoder(context, binaryFormat, output, terminateAt)
            else
                this
        }
    }
}