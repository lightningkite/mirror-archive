package com.lightningkite.mirror.archive.flatarray

import com.lightningkite.kommon.collection.forEachBetween
import com.lightningkite.mirror.archive.model.Condition
import com.lightningkite.mirror.archive.model.Operation
import kotlinx.serialization.*
import kotlinx.serialization.internal.BooleanDescriptor
import kotlinx.serialization.modules.EmptyModule
import kotlinx.serialization.modules.SerialModule

abstract class FlatArrayFormat(
        context: SerialModule = EmptyModule,
        val terminateAt: (SerialDescriptor) -> Boolean = { false }
) : AbstractSerialFormat(context) {

    abstract fun <V> toArray(type: SerializationStrategy<V>, value: V): ArrayList<Any?>
    abstract fun <V> toArrayPartial(type: SerializationStrategy<V>, default: V, value: Any?, indexPath: IndexPath): ArrayList<Any?>
    abstract fun <V> fromArray(type: DeserializationStrategy<V>, list: List<Any?>): V

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
        if (terminateAt(serialDescriptor)) {
            to.add(Column(
                    name = prefix,
                    indexPath = indexPath,
                    type = serialDescriptor,
                    annotations = annotations
            ))
        } else {
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

    fun <T> schema(type: KSerializer<T>, default: T): Schema<T> {
        return Schema(
                format = this,
                overallType = type,
                default = default,
                columns = columns(type.descriptor)
        )
    }


    data class Column(
            val name: String,
            val indexPath: IndexPath,
            val type: SerialDescriptor,
            val annotations: List<Annotation>
    )

    data class ColumnCondition(
            val condition: Condition<*>,
            val column: Column? = null
    )

    data class Schema<T>(
            val format: FlatArrayFormat,
            val overallType: KSerializer<T>,
            val default: T,
            val columns: List<Column>
    ) {
        enum class ConditionMode { AND, OR, NOT }

        val byIndexPath = columns.associateBy { it.indexPath }
        val byLowercaseName = columns.associateBy { it.name.toLowerCase() }

        fun operationStream(
                op: Operation<*>,
                indexPath: IndexPath,
                action: (operation: Operation<*>, column: Column?) -> Unit
        ) {
            when (op) {
                is Operation.Multiple -> {
                    for (item in op.operations) {
                        operationStream(item, indexPath, action)
                    }
                }
                is Operation.AddNumeric -> {
                    action(op, byIndexPath[indexPath])
                }
                is Operation.Append -> {
                    action(op, byIndexPath[indexPath])
                }
                is Operation.Field<*, *> -> {
                    operationStream(op.operation, indexPath + op.field.index, action)
                }
                is Operation.Set -> {
//                    val col = byIndexPath[indexPath]
//                    if(col == null){
//                    } else {
//                        action(op, col)
//                    }
                    val broken = format.toArrayPartial(overallType, default, op.value, indexPath)
                    columns
                            .asSequence()
                            .filter { it.indexPath.startsWith(indexPath) }
                            .forEachIndexed { index, it ->
                                val part = broken[index]
                                action(Operation.Set(part), it)
                            }
                }
            }
        }

        fun conditionStream(
                cond: Condition<*>,
                indexPath: IndexPath,
                startGroup: (mode: ConditionMode) -> Unit,
                groupDivider: (mode: ConditionMode) -> Unit,
                endGroup: (mode: ConditionMode) -> Unit,
                action: (condition: Condition<*>, column: Column?) -> Unit
        ) {
            when (cond) {
                Condition.Never,
                Condition.Always -> action(cond, null)
                is Condition.And -> {
                    startGroup(ConditionMode.AND)
                    cond.conditions.forEachBetween(
                            forItem = { part ->
                                conditionStream(part, indexPath, startGroup, groupDivider, endGroup, action)
                            },
                            between = { groupDivider(ConditionMode.AND) }
                    )
                    endGroup(ConditionMode.AND)
                }
                is Condition.Or -> {
                    startGroup(ConditionMode.OR)
                    cond.conditions.forEachBetween(
                            forItem = { part ->
                                conditionStream(part, indexPath, startGroup, groupDivider, endGroup, action)
                            },
                            between = { groupDivider(ConditionMode.OR) }
                    )
                    endGroup(ConditionMode.OR)
                }
                is Condition.Not -> {
                    startGroup(ConditionMode.NOT)
                    conditionStream(cond.condition, indexPath, startGroup, groupDivider, endGroup, action)
                    endGroup(ConditionMode.NOT)
                }
                is Condition.Field<*, *> -> {
                    conditionStream(cond.condition, indexPath + cond.field.index, startGroup, groupDivider, endGroup, action)
                }
                is Condition.Equal -> {
                    val col = byIndexPath[indexPath]
                    if (col == null) {
                        val broken = format.toArrayPartial(overallType, default, cond.value, indexPath)
                        startGroup(ConditionMode.AND)

                        var index = 0
                        columns
                                .asSequence()
                                .filter { it.indexPath.startsWith(indexPath) }
                                .asIterable()
                                .forEachBetween(
                                        forItem = {
                                            val part = broken[index++]
                                            action(Condition.Equal(part), it)
                                        },
                                        between = { groupDivider(ConditionMode.AND) }
                                )
                        endGroup(ConditionMode.AND)
                    } else {
                        action(cond, col)
                    }
                }
                is Condition.EqualToOne -> {
                    val col = byIndexPath[indexPath]
                    if (col == null) {
                        val brokens = cond.values.map {
                            format.toArrayPartial(overallType, default, it, indexPath)
                        }

                        startGroup(ConditionMode.OR)
                        for (broken in brokens) {
                            startGroup(ConditionMode.AND)
                            var index = 0
                            columns
                                    .asSequence()
                                    .filter { it.indexPath.startsWith(indexPath) }
                                    .asIterable()
                                    .forEachBetween(
                                            forItem = {
                                                val part = broken[index++]
                                                action(Condition.Equal(part), it)
                                            },
                                            between = { groupDivider(ConditionMode.AND) }
                                    )
                            endGroup(ConditionMode.AND)
                        }
                        endGroup(ConditionMode.OR)
                    } else {
                        action(cond, col)
                    }
                }
                is Condition.NotEqual -> {
                    val col = byIndexPath[indexPath]
                    if (col == null) {
                        val broken = format.toArrayPartial(overallType, default, cond.value, indexPath)
                        startGroup(ConditionMode.OR)

                        var index = 0
                        columns
                                .asSequence()
                                .filter { it.indexPath.startsWith(indexPath) }
                                .asIterable()
                                .forEachBetween(
                                        forItem = {
                                            val part = broken[index++]
                                            action(Condition.NotEqual(part), it)
                                        },
                                        between = { groupDivider(ConditionMode.AND) }
                                )
                        endGroup(ConditionMode.OR)
                    } else {
                        action(cond, col)
                    }
                }
                is Condition.LessThan,
                is Condition.GreaterThan,
                is Condition.LessThanOrEqual,
                is Condition.GreaterThanOrEqual,
                is Condition.TextSearch,
                is Condition.StartsWith,
                is Condition.EndsWith,
                is Condition.RegexTextSearch -> {
                    action(cond, byIndexPath[indexPath]!!)
                }
            }
        }
    }
}
