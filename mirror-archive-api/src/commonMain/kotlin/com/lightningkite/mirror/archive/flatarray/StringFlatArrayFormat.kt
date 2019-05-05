package com.lightningkite.mirror.archive.flatarray

import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.SerialDescriptor
import kotlinx.serialization.SerializationStrategy
import kotlinx.serialization.StringFormat
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.EmptyModule
import kotlinx.serialization.modules.SerialModule

open class StringFlatArrayFormat(
        val stringFormat: StringFormat = Json,
        context: SerialModule = EmptyModule,
        terminateAt: (SerialDescriptor) -> Boolean = { false }
) : FlatArrayFormat(context, terminateAt) {

    companion object : StringFlatArrayFormat()

    override fun <V> toArray(type: SerializationStrategy<V>, value: V): ArrayList<Any?> {
        val map = ArrayList<Any?>()
        StringFlatArrayEncoder(
                stringFormat = stringFormat,
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
        StringFlatArrayEncoder.Partial(
                stringFormat = stringFormat,
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
        return StringFlatArrayDecoder(
                stringFormat = stringFormat,
                context = context,
                input = list,
                terminateAt = terminateAt
        ).decodeSerializableValue(type)
    }
}
