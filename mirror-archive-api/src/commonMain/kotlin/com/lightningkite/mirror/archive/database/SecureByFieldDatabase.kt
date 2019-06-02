package com.lightningkite.mirror.archive.database

import com.lightningkite.mirror.archive.model.*


/**
 * An accessor for the given database that enforces security rules.
 */
class SecureByFieldDatabase<T : Any>(
        val underlying: Database<T>,
        val rules: SecurityRules<T>
) : Database<T> {

    override suspend fun get(condition: Condition<T>, sort: List<Sort<T, *>>, count: Int, after: T?): List<T> = with(rules) {
        val c = (condition and limitReadLazy()).secure() and sort.secureCondition()
        return underlying.get(c, sort, count, after).map { it.secureOutput() }
    }

    override suspend fun insert(values: List<T>): List<T> = with(rules) {
        return underlying.insert(values.filter { limitInsertLazy()(it) }.map { it.secureInsert() }).map { it.secureOutput() }
    }

    override suspend fun update(condition: Condition<T>, operation: Operation<T>): Int = with(rules) {
        operation.secure()?.let {
            return underlying.update((condition and limitUpdateLazy()).secure() and it.secureCondition(), it)
        }
        return 0
    }

    override suspend fun limitedUpdate(condition: Condition<T>, operation: Operation<T>, sort: List<Sort<T, *>>, limit: Int): Int = with(rules) {
        operation.secure()?.let {
            return underlying.limitedUpdate(
                    condition = (condition and limitUpdateLazy()).secure() and it.secureCondition() and sort.secureCondition(),
                    operation = it,
                    sort = sort,
                    limit = limit
            )
        }
        return 0
    }

    override suspend fun delete(condition: Condition<T>): Int = with(rules) {
        return underlying.delete((condition and limitUpdateLazy()).secure())
    }
}

/**
 * Returns an accessor to an actual database with the given security rules for each field.
 */
fun <T : Any> Database<T>.secure(
        rules: SecurityRules<T>
) = SecureByFieldDatabase(
        underlying = this,
        rules = rules
)