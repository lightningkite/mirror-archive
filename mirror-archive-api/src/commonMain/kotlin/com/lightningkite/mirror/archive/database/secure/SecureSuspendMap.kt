package com.lightningkite.mirror.archive.database.secure

import com.lightningkite.kommon.exception.ForbiddenException
import com.lightningkite.mirror.archive.database.SuspendMap
import com.lightningkite.mirror.archive.model.Condition
import com.lightningkite.mirror.archive.model.Operation
import com.lightningkite.mirror.archive.model.Sort
import com.lightningkite.mirror.archive.model.and
import com.lightningkite.mirror.info.FieldInfo

class SecureSuspendMap<K, V : Any, USER>(
        val underlying: SuspendMap<K, V>,
        val rules: Rules<K, V, USER>
) {

    class Rules<K, V : Any, USER>(
            val readRule: (USER?) -> Condition<V> = { Condition.Always() },
            val overwriteRule: (USER?) -> Condition<V> = { Condition.Always() },
            val mask: (USER?, V) -> V = { _, v -> v },
            val sortPermitted: (USER?, Sort<V>) -> Unit = { _, sort -> },
            val validate: (USER?, V) -> V? = { _, v -> v },
            val operation: (USER?, Operation<V>) -> Operation<V> = { _, op -> op },
            val create: (USER?) -> Boolean = { true }
    ) {
        operator fun plus(other: Rules<K, V, USER>): Rules<K, V, USER> {
            return Rules(
                    readRule = {
                        Condition.And(listOf(this.readRule(it), other.readRule(it)))
                    },
                    overwriteRule = {
                        Condition.And(listOf(this.overwriteRule(it), other.overwriteRule(it)))
                    },
                    sortPermitted = { user, sort ->
                        this.sortPermitted(user, sort)
                        other.sortPermitted(user, sort)
                    },
                    operation = { user, operation ->
                        other.operation(user, this.operation(user, operation))
                    },
                    mask = { user, value ->
                        other.mask(user, this.mask(user, value))
                    },
                    validate = { user, value ->
                        this.validate(user, value)?.let { other.validate(user, it) }
                    },
                    create = {
                        this.create(it) && other.create(it)
                    }
            )
        }
    }

    fun forUser(user: USER?) = object : SuspendMap<K, V> {

        override suspend fun getNewKey(): K = underlying.getNewKey()

        override suspend fun get(key: K): V? = underlying.get(key)
                ?.takeIf { rules.readRule(user).invoke(it) }
                ?.let { rules.mask(user, it) }

        override suspend fun put(key: K, value: V, conditionIfExists: Condition<V>, create: Boolean): Boolean {
            return underlying.put(
                    key = key,
                    value = rules.validate(user, value) ?: throw ForbiddenException(),
                    conditionIfExists = rules.overwriteRule(user) and conditionIfExists,
                    create = create && rules.create(user)
            )
        }

        override suspend fun modify(key: K, operation: Operation<V>, condition: Condition<V>): V? {
            return underlying.modify(
                    key = key,
                    operation = rules.operation(user, operation),
                    condition = condition
            )?.let { rules.mask(user, it) }
        }

        override suspend fun remove(key: K, condition: Condition<V>): Boolean {
            return underlying.remove(key, condition and rules.overwriteRule(user))
        }

        override suspend fun find(condition: Condition<V>, sortedBy: Sort<V>?): Pair<K, V>? {
            sortedBy?.let { rules.sortPermitted(user, it) }
            return underlying.find(condition and rules.readRule(user), sortedBy)?.let { it.first to rules.mask(user, it.second) }
        }

        override suspend fun getMany(keys: Collection<K>): Map<K, V?> {
            val map = HashMap<K, V?>()
            for ((key, value) in underlying.getMany(keys)) {
                val maskedValue = value
                        ?.takeIf { rules.readRule(user).invoke(it) }
                        ?.let { rules.mask(user, it) }
                if (maskedValue != null) {
                    map[key] = maskedValue
                }
            }
            return map
        }

        override suspend fun query(condition: Condition<V>, sortedBy: Sort<V>?, after: Pair<K, V>?, count: Int): List<Pair<K, V>> {
            sortedBy?.let { rules.sortPermitted(user, it) }
            return underlying.query(
                    condition = condition and rules.readRule(user),
                    sortedBy = sortedBy,
                    after = after,
                    count = count
            ).map { it.first to rules.mask(user, it.second) }
        }
    }
}