package com.lightningkite.mirror.archive.database

import com.lightningkite.mirror.archive.model.*
import com.lightningkite.mirror.breaker.Breaker
import com.lightningkite.mirror.info.MirrorClass
import com.lightningkite.mirror.info.MirrorType

class PartialDatabase<T : Any>(
        val underlying: Database<T>,
        val type: MirrorType<T>,
        val requirements: Map<MirrorClass.Field<T, *>, Any?>,
        additionalConditions: Condition<T> = Condition.Always
) : Database<T> {

    val partialCondition = Condition.And(requirements.map {
        @Suppress("UNCHECKED_CAST")
        (it.key as MirrorClass.Field<T, Any?>) equal it.value
    }) and additionalConditions

    override suspend fun get(condition: Condition<T>, sort: List<Sort<T, *>>, count: Int, after: T?): List<T> {
        return underlying.get(
                condition = condition and partialCondition,
                sort = sort,
                count = count,
                after = after
        )
    }

    override suspend fun insert(values: List<T>): List<T> {
        return underlying.insert(values)
    }

    override suspend fun update(condition: Condition<T>, operation: Operation<T>): Int {
        return underlying.update(condition and partialCondition, operation)
    }

    override suspend fun limitedUpdate(condition: Condition<T>, operation: Operation<T>, sort: List<Sort<T, *>>, limit: Int): Int {
        return underlying.limitedUpdate(condition and partialCondition, operation, sort, limit)
    }

    override suspend fun delete(condition: Condition<T>): Int {
        return underlying.delete(condition and partialCondition)
    }

}

fun <T : Any> Database<T>.limit(
        type: MirrorType<T>,
        requirements: Map<MirrorClass.Field<T, *>, Any?>,
        additionalConditions: Condition<T> = Condition.Always
) = PartialDatabase<T>(this, type, requirements, additionalConditions)