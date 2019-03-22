package com.lightningkite.mirror.archive.database

import com.lightningkite.mirror.archive.flatmap.Breaker
import com.lightningkite.mirror.archive.model.*
import com.lightningkite.mirror.info.MirrorClass
import com.lightningkite.mirror.info.MirrorType


/*

//REVERSED - default is disallow

allowRead(Condition.Always)
allowUpdate(Condition.Always)
allowInsertion { item -> }
fields.forEach {
    //Applies to gets (masking output, sorts, filters)
    permitRead(Condition.Always) //Using this field as a sort or filter adds this as a condition too

    //Specifically denotes the mask value
    mask(value)

    //Applies to updates
    permitUpdate(Condition.Always)

    //Applies to inserts and updates
    //Limits all operations to SET only
    tweak { value -> value }

    //Applies to updates
    ignore { value -> value == null }
}


PIPELINES
sort -> field permitted reads -> secure sort and additional condition
condition -> field permitted reads -> secure condition
item output -> field masks and permitted reads -> secure item output
item input -> field tweaks -> secure item input
operation -> field tweaks and ignores -> secure operation

PUBLICIZE SECURITY RULES
You can ask what the server can do for you, and find this information:
- This field is tweaked
- This field is ignored
- Updating this field is only permitted when X
- This field is only shown when X
*/
/**
 * An accessor for the given database that secures the individual fields of the type.
 *
 * Best used in combination with [SecureDatabase].
 */
class SecureByFieldDatabase<T : Any>(
        val underlying: Database<T>,
        val type: MirrorClass<T>,
        maskObject: T
) : Database<T> {

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
    fun <V> MirrorClass.Field<T, V>.ignores(explanation: String = "Updates to this field may be ignored.", operationPredicate: (Operation<V>) -> Boolean) {
        fieldIgnoresExplanation[index] = explanation
        fieldIgnores[index] = operationPredicate as ((Operation<Any?>) -> Boolean)
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

    fun T.secureInput(): T = Breaker.modify(type, this) {
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


    //Database Functions

    override suspend fun get(condition: Condition<T>, sort: List<Sort<T, *>>, count: Int, after: T?): List<T> {
        val c = condition.secure() and sort.secureCondition()
        return underlying.get(c, sort, count, after).map { it.secureOutput() }
    }

    override suspend fun insert(values: List<T>): List<T> {
        return underlying.insert(values.map { it.secureInput() }).map { it.secureOutput() }
    }

    override suspend fun update(condition: Condition<T>, operation: Operation<T>): Int {
        operation.secure()?.let {
            return underlying.update(condition.secure() and it.secureCondition(), it)
        }
        return 0
    }

    override suspend fun delete(condition: Condition<T>): Int {
        return underlying.delete(condition.secure())
    }
}

/**
 * Returns an accessor to an actual database with the given security rules for each field.
 */
inline fun <T : Any> Database<T>.secureFields(
        type: MirrorClass<T>,
        maskObject: T,
        configure: SecureByFieldDatabase<T>.() -> Unit
) = SecureByFieldDatabase(
        underlying = this,
        type = type,
        maskObject = maskObject
).apply(configure)