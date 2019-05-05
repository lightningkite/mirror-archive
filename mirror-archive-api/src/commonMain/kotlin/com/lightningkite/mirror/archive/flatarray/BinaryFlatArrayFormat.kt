package com.lightningkite.mirror.archive.flatarray

import kotlinx.serialization.BinaryFormat
import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.SerialDescriptor
import kotlinx.serialization.SerializationStrategy
import kotlinx.serialization.cbor.Cbor
import kotlinx.serialization.modules.EmptyModule
import kotlinx.serialization.modules.SerialModule

open class BinaryFlatArrayFormat(
        val binaryFormat: BinaryFormat = Cbor.plain,
        context: SerialModule = EmptyModule,
        terminateAt: (SerialDescriptor) -> Boolean = { false }
) : FlatArrayFormat(context, terminateAt) {

    companion object : BinaryFlatArrayFormat()

    override fun <V> toArray(type: SerializationStrategy<V>, value: V): ArrayList<Any?> {
        val map = ArrayList<Any?>()
        BinaryFlatArrayEncoder(
                binaryFormat = binaryFormat,
                context = context,
                output = map,
                terminateAt = terminateAt
        ).encodeSerializableValue(
                serializer = type,
                value = value
        )
        return map
    }

    override fun <V> toArrayPartial(type: SerializationStrategy<V>, default: V, value: Any?, indexPath: IndexPath): ArrayList<Any?> {
        if (indexPath.isEmpty()) {
            @Suppress("UNCHECKED_CAST")
            return toArray(type, value as V)
        }
        val map = ArrayList<Any?>()
        BinaryFlatArrayEncoder.Partial(
                binaryFormat = binaryFormat,
                context = context,
                output = map,
                selectedIndicies = indexPath,
                overridingValue = value,
                terminateAt = terminateAt
        ).encodeSerializableValue(
                serializer = type,
                value = default
        )
        return map
    }

    override fun <V> fromArray(type: DeserializationStrategy<V>, list: List<Any?>): V {
        return BinaryFlatArrayDecoder(
                binaryFormat = binaryFormat,
                context = context,
                input = list,
                terminateAt = terminateAt
        ).decodeSerializableValue(type)
    }
}
