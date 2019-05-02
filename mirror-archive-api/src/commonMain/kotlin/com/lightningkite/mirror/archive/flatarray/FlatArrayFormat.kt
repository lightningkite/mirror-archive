package com.lightningkite.mirror.archive.flatarray

import kotlinx.serialization.*
import kotlinx.serialization.internal.BooleanDescriptor

object FlatArrayFormat : AbstractSerialFormat() {

    fun <V> toArray(type: SerializationStrategy<V>, value: V): ArrayList<Any?> {
        val map = ArrayList<Any?>()
        type.serialize(FlatArrayEncoder(context, map), value)
        return map
    }

    fun <V> toArrayPartial(type: SerializationStrategy<V>, default: V, value: Any?, indexPath: IndexPath): ArrayList<Any?> {
        if (indexPath.isEmpty()) {
            @Suppress("UNCHECKED_CAST")
            return toArray(type, value as V)
        }
        val map = ArrayList<Any?>()
        type.serialize(
                encoder = FlatArrayEncoder.Partial(
                        context = context,
                        output = map,
                        selectedIndicies = indexPath,
                        overridingValue = value
                ),
                obj = default
        )
        return map
    }

    fun <V> fromArray(type: DeserializationStrategy<V>, list: List<Any?>): V {
        return type.deserialize(FlatArrayDecoder(context, list))
    }

    data class Column(
            val name: String,
            val indexPath: IndexPath,
            val type: SerialDescriptor,
            val annotations: List<Annotation>
    )

    private fun columns(
            serialDescriptor: SerialDescriptor,
            prefix: String = "",
            indexPath: IndexPath = IndexPath.empty,
            annotations: List<Annotation> = listOf(),
            seen: ArrayList<String> = ArrayList(),
            to: ArrayList<Column>
    ) {
        if (serialDescriptor.isNullable) {
            to.add(Column(
                    name = prefix nameCombine "null",
                    indexPath = indexPath,
                    type = BooleanDescriptor,
                    annotations = annotations
            ))
        }
        when (serialDescriptor.kind) {
            PrimitiveKind.UNIT -> {
            }
            StructureKind.CLASS -> {
                if (serialDescriptor.name in seen) {
                    to.add(Column(
                            name = prefix,
                            indexPath = indexPath,
                            type = serialDescriptor,
                            annotations = annotations
                    ))
                } else {
                    seen.add(serialDescriptor.name)
                    for (it in 0 until serialDescriptor.elementsCount) {
                        columns(
                                serialDescriptor = serialDescriptor.getElementDescriptor(it),
                                prefix = prefix nameCombine serialDescriptor.getElementName(it),
                                indexPath = indexPath + it,
                                annotations = annotations + serialDescriptor.getElementAnnotations(it),
                                seen = seen,
                                to = to
                        )
                    }
                    seen.removeAt(seen.lastIndex)
                }
            }
            else -> {
                to.add(Column(
                        name = prefix,
                        indexPath = indexPath,
                        type = serialDescriptor,
                        annotations = annotations
                ))
            }
        }
    }

    fun columns(serialDescriptor: SerialDescriptor, prefix: String = "", annotations: List<Annotation> = listOf()): List<Column> {
        val out = ArrayList<Column>()
        columns(
                serialDescriptor = serialDescriptor,
                prefix = prefix,
                annotations = annotations,
                to = out
        )
        return out
    }

}

infix fun String.nameCombine(other: String): String {
    return if (this.isBlank()) other
    else if (other.isBlank()) this
    else this + "_" + other
}