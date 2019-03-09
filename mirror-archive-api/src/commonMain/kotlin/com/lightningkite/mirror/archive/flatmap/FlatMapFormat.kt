package com.lightningkite.mirror.flatmap

import com.lightningkite.kommon.atomic.AtomicReference
import kotlinx.serialization.*
import kotlinx.serialization.internal.BooleanDescriptor

object FlatMapFormat : AbstractSerialFormat() {

    fun <V> toMap(type: SerializationStrategy<V>, value: V): HashMap<String, Any?> {
        val map = HashMap<String, Any?>()
        type.serialize(FlatMapEncoder(context, map), value)
        return map
    }

    fun <V> fromMap(type: DeserializationStrategy<V>, map: Map<String, Any?>): V {
        return type.deserialize(FlatMapDecoder(context, map))
    }

    data class Table(val name: String, val columns: List<Column>)
    data class Column(val name: String, val type: SerialDescriptor, val annotations: List<Annotation>)

    fun columns(serialDescriptor: SerialDescriptor, prefix: String = "", annotations: List<Annotation> = listOf()): List<Column> {
        return when (serialDescriptor.kind) {
            PrimitiveKind.UNIT -> {
                if (serialDescriptor.isNullable) {
                    listOf(
                            Column(
                                    name = prefix nameCombine "null",
                                    type = BooleanDescriptor,
                                    annotations = annotations
                            )
                    )
                } else {
                    listOf()
                }
            }
            StructureKind.CLASS -> {
                val fieldColumns = (0 until serialDescriptor.elementsCount).flatMap {
                    columns(
                            serialDescriptor = serialDescriptor.getElementDescriptor(it),
                            prefix = prefix nameCombine serialDescriptor.getElementName(it),
                            annotations = annotations + serialDescriptor.getElementAnnotations(it)
                    )
                }
                if (serialDescriptor.isNullable) {
                    fieldColumns.plus(Column(
                            name = prefix nameCombine "null",
                            type = BooleanDescriptor,
                            annotations = annotations
                    ))
                } else {
                    fieldColumns
                }
            }
            else -> {
                val dataColumn = Column(
                        name = prefix,
                        type = serialDescriptor,
                        annotations = annotations
                )
                if (serialDescriptor.isNullable) {
                    listOf(
                            Column(
                                    name = prefix nameCombine "null",
                                    type = BooleanDescriptor,
                                    annotations = annotations
                            ),
                            dataColumn
                    )
                } else {
                    listOf(dataColumn)
                }
            }
        }
    }

}

infix fun String.nameCombine(other: String): String {
    return if (this.isBlank()) other
    else if (other.isBlank()) this
    else this + "_" + other
}