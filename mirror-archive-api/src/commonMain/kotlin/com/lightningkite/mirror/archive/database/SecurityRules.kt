package com.lightningkite.mirror.archive.database

import com.lightningkite.mirror.archive.model.*
import com.lightningkite.mirror.breaker.Breaker
import com.lightningkite.mirror.info.MirrorClass

data class SecurityRules<T : Any>(
        val type: MirrorClass<T>,
        val maskObject: T,
        var limitRead: Condition<T> = Condition.Never,
        var limitUpdate: Condition<T> = Condition.Never,
        var limitInsert: Condition<T> = Condition.Never
) {

    private var fieldRead = Array<Condition<T>?>(type.elementsCount) { null }
    private var fieldUpdate = Array<Condition<T>?>(type.elementsCount) { null }
    private var fieldIgnoresExplanation = Array<String?>(type.elementsCount) { null }
    private var fieldIgnores = Array<((Operation<Any?>) -> Boolean)?>(type.elementsCount) { null }
    private var fieldMasks = Breaker.snap(type, maskObject)
    private var fieldTweaks = Array<((Any?) -> Any?)?>(type.elementsCount) { null }
    private var fieldTweaksExplanation = Array<String?>(type.elementsCount) { null }

    /**
     * Sets the condition under which a field can be read.
     * If this field cannot be read, the value will be replaced in the output by the corresponding value in the mask object.
     */
    fun <V> MirrorClass.Field<T, V>.read(condition: Condition<T>) {
        fieldRead[index] = condition
    }

    /**
     * Sets the condition under which a field can be updated.
     * If this field cannot be updated, the update is simply ignored.
     */
    fun <V> MirrorClass.Field<T, V>.update(condition: Condition<T>) {
        fieldUpdate[index] = condition
    }

    /**
     * Sets the condition under which an update to the field is ignored.
     * This allows you to ignore certain operations, such as setting a particular field to null.
     */
    @Suppress("UNCHECKED_CAST")
    fun <V> MirrorClass.Field<T, V>.ignoresUpdates(explanation: String = "Updates to this field may be ignored.", operationPredicate: (Operation<V>) -> Boolean) {
        fieldIgnoresExplanation[index] = explanation
        fieldIgnores[index] = operationPredicate as ((Operation<Any?>) -> Boolean)
    }

    /**
     * Ignores any and all specifications for this field.
     * Upon insert, values from the mask are used instead.
     * Upon update, changes are ignored.
     */
    @Suppress("UNCHECKED_CAST")
    fun <V> MirrorClass.Field<T, V>.ignores() {
        tweaks { fieldMasks[index] as V }
        ignoresUpdates("This field cannot be updated.") { true }
    }

    /**
     * Tweaks the given value for the field before it is inserted or updated in the database.
     * Useful for hashing fields.
     */
    @Suppress("UNCHECKED_CAST")
    fun <V> MirrorClass.Field<T, V>.tweaks(explanation: String = "Tweaks to the given value for this field may be made.", modification: (V) -> V) {
        fieldTweaksExplanation[index] = explanation
        fieldTweaks[index] = modification as (Any?) -> Any?
    }

    /**
     * Describes the security rules for the given field.
     */
    data class Rules<T, V>(
            val read: Condition<T>? = null,
            val update: Condition<T>? = null,
            val ignored: String? = null,
            val mask: V? = null,
            val tweaked: String? = null
    )

    /**
     * Returns a description of the security rules for a field.
     */
    @Suppress("UNCHECKED_CAST")
    fun <V> rules(field: MirrorClass.Field<T, V>) = Rules<T, V>(
            read = fieldRead[field.index],
            update = fieldUpdate[field.index],
            ignored = fieldIgnoresExplanation[field.index],
            mask = fieldMasks[field.index] as V,
            tweaked = fieldTweaksExplanation[field.index]
    )


    //Secure functions

    fun T.secureOutput(): T = Breaker.modify(type, this) {
        for (index in it.indices) {
            val readable = fieldRead[index]?.invoke(this)
            if (readable != true) {
                it[index] = fieldMasks[index]
            }
        }
    }

    fun T.secureInsert(): T = Breaker.modify(type, this) {
        for (index in it.indices) {
            val tweak = fieldTweaks[index]
            if (tweak != null) {
                it[index] = tweak.invoke(it[index])
            }
        }
    }

    fun List<Sort<T, *>>.secureCondition(): Condition<T> {
        return Condition.And(this.map {
            fieldRead[it.field.index] ?: Condition.Never
        })
//        val conditions = this.iterable().
//        return if (conditions.isEmpty()) Condition.Always
//        else Condition.And(conditions)
    }

    fun Condition<T>.secure(): Condition<T> = when (this) {
        Condition.Never -> Condition.Never
        Condition.Always -> Condition.Always
        is Condition.And -> Condition.And(conditions.map { it.secure() })
        is Condition.Or -> Condition.Or(conditions.map { it.secure() })
        is Condition.Not -> Condition.Not(condition.secure())
        is Condition.Field<*, *> -> {
            val additional = fieldRead[field.index]
            if (additional == null) this
            else this and additional
        }
        else -> this
    }

    @Suppress("UNCHECKED_CAST")
    fun Operation<T>.secure(): Operation<T>? {
        return when (this) {
            is Operation.Multiple -> Operation.Multiple(operations.mapNotNull { it.secure() })
            is Operation.Field<*, *> -> {
                if (fieldIgnores[field.index]?.invoke(operation as Operation<Any?>) == true) return null
                val tweak = fieldTweaks[field.index]
                val newOp = if (tweak != null && operation is Operation.Set) {
                    Operation.Set(tweak(operation.value))
                } else operation
                Operation.Field(field as MirrorClass.Field<T, Any?>, newOp as Operation<Any?>)
            }
            is Operation.Set -> this.separate(type).secure()
            else -> this
        }
    }

    @Suppress("UNCHECKED_CAST")
    fun Operation<T>.secureCondition(): Condition<T> = when (this) {
        is Operation.Multiple -> Condition.And(operations.map { it.secureCondition() })
        is Operation.Field<*, *> -> {
            fieldUpdate[field.index] ?: Condition.Always
        }
        is Operation.Set -> this.separate(type).secureCondition()
        else -> Condition.Always
    }
}