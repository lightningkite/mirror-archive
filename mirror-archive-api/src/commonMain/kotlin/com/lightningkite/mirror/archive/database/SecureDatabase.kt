package com.lightningkite.mirror.archive.database

import com.lightningkite.mirror.archive.model.Condition
import com.lightningkite.mirror.archive.model.Operation
import com.lightningkite.mirror.archive.model.Sort
import com.lightningkite.mirror.archive.model.and


/**
 * An accessor to an actual database with the given security rules.
 */
class SecureDatabase<T : Any>(
        val underlying: Database<T>,
        var limitRead: Condition<T> = Condition.Never,
        var limitUpdate: Condition<T> = Condition.Never,
        var limitInsert: Condition<T> = Condition.Never
) : Database<T> {

    override suspend fun get(condition: Condition<T>, sort: List<Sort<T, *>>, count: Int, after: T?): List<T> {
        return underlying.get(condition and limitRead, sort, count, after)
    }

    override suspend fun insert(values: List<T>): List<T> {
        return underlying.insert(values.filter { limitInsert(it) })
    }

    override suspend fun update(condition: Condition<T>, operation: Operation<T>): Int {
        return underlying.update(condition and limitUpdate, operation)
    }

    override suspend fun limitedUpdate(condition: Condition<T>, operation: Operation<T>, sort: List<Sort<T, *>>, limit: Int): Int {
        return underlying.limitedUpdate(condition and limitUpdate, operation, sort, limit)
    }

    override suspend fun delete(condition: Condition<T>): Int {
        return underlying.delete(condition and limitUpdate)
    }

    override suspend fun count(condition: Condition<T>): Int {
        return underlying.count(condition and limitRead)
    }
}

/**
 * Returns an accessor to an actual database with the given security rules.
 */
fun <T : Any> Database<T>.secure(
        limitRead: Condition<T> = Condition.Never,
        limitUpdate: Condition<T> = Condition.Never,
        limitInsert: Condition<T> = Condition.Never
) = SecureDatabase(
        underlying = this,
        limitRead = limitRead,
        limitUpdate = limitUpdate,
        limitInsert = limitInsert
)