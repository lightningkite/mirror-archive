package com.lightningkite.mirror.archive.database.secure

import com.lightningkite.kommon.exception.ForbiddenException
import com.lightningkite.mirror.archive.database.SuspendMap
import com.lightningkite.mirror.archive.model.Condition
import com.lightningkite.mirror.archive.model.Operation
import com.lightningkite.mirror.archive.model.Sort
import com.lightningkite.mirror.archive.model.and
import com.lightningkite.mirror.info.ClassInfo
import com.lightningkite.mirror.info.FieldInfo

class SecureSuspendMap<K, V : Any, USER>(
        val underlying: SuspendMap<K, V>,
        val rules: Rules<K, V, USER>
) {

    class Rules<K, V : Any, USER>(
            val readRule: ((USER?) -> Condition<V>)? = null,
            val overwriteRule: ((USER?) -> Condition<V>)? = null,
            val mask: ((USER?, V) -> V)? = null,
            val sortPermitted: ((USER?, Sort<V>) -> Unit)? = null,
            val validate: ((USER?, V) -> V?)? = null,
            val operation: ((USER?, Operation<V>) -> Operation<V>)? = null,
            val create: ((USER?) -> Boolean)? = null
    ) {
        val readRuleOrDefault get() = readRule ?: { Condition.Always() }
        val overwriteRuleOrDefault get() = overwriteRule ?: { Condition.Always() }
        val maskOrDefault get() = mask ?: { _, it -> it }
        val sortPermittedOrDefault get() = sortPermitted ?: { _, _ -> }
        val validateOrDefault get() = validate ?: { _, it -> it }
        val operationOrDefault get() = operation ?: { _, it -> it }
        val createOrDefault get() = create ?: { true }
        operator fun plus(other: Rules<K, V, USER>): Rules<K, V, USER> {
            return Rules(
                    readRule = pick(
                            a = this.readRule,
                            b = other.readRule
                    ) { a, b -> { user -> a(user) and b(user) } },
                    overwriteRule = pick(
                            a = this.overwriteRule,
                            b = other.overwriteRule
                    ) { a, b -> { user -> a(user) and b(user) } },
                    sortPermitted = pick(
                            a = this.sortPermitted,
                            b = other.sortPermitted
                    ) { a, b -> { user, sort -> a(user, sort); b(user, sort) } },
                    operation = pick(
                            a = this.operation,
                            b = other.operation
                    ) { a, b -> { user, op -> b(user, a(user, op)) } },
                    mask = pick(
                            a = this.mask,
                            b = other.mask
                    ) { a, b -> { user, item -> b(user, a(user, item)) } },
                    validate = pick(
                            a = this.validate,
                            b = other.validate
                    ) { a, b -> { user, item -> a(user, item)?.let { b(user, it) } } },
                    create = pick(
                            a = this.create,
                            b = other.create
                    ) { a, b -> { user -> a(user) && b(user) } }
            )
        }

        private inline fun <T> pick(a: T?, b: T?, merge: (T, T) -> T): T? {
            return if (a != null && b != null) merge(a, b)
            else a ?: b
        }
    }

    fun forUser(user: USER?) = object : SuspendMap<K, V> {

        override suspend fun getNewKey(): K = underlying.getNewKey()

        override suspend fun get(key: K): V? = underlying.get(key)
                ?.takeIf { rules.readRuleOrDefault(user).invoke(it) }
                ?.let { rules.maskOrDefault(user, it) }

        override suspend fun put(key: K, value: V, conditionIfExists: Condition<V>, create: Boolean): Boolean {
            return underlying.put(
                    key = key,
                    value = rules.validateOrDefault(user, value) ?: throw ForbiddenException(),
                    conditionIfExists = rules.overwriteRuleOrDefault(user) and conditionIfExists,
                    create = create && rules.createOrDefault(user)
            )
        }

        override suspend fun modify(key: K, operation: Operation<V>, condition: Condition<V>): V? {
            return underlying.modify(
                    key = key,
                    operation = rules.operationOrDefault(user, operation),
                    condition = condition
            )?.let { rules.maskOrDefault(user, it) }
        }

        override suspend fun remove(key: K, condition: Condition<V>): Boolean {
            return underlying.remove(key, condition and rules.overwriteRuleOrDefault(user))
        }

        override suspend fun find(condition: Condition<V>, sortedBy: Sort<V>?): Pair<K, V>? {
            sortedBy?.let { rules.sortPermittedOrDefault(user, it) }
            return underlying.find(condition and rules.readRuleOrDefault(user), sortedBy)?.let { it.first to rules.maskOrDefault(user, it.second) }
        }

        override suspend fun getMany(keys: List<K>): Map<K, V?> {
            val map = HashMap<K, V?>()
            for ((key, value) in underlying.getMany(keys)) {
                val maskedValue = value
                        ?.takeIf { rules.readRuleOrDefault(user).invoke(it) }
                        ?.let { rules.maskOrDefault(user, it) }
                if (maskedValue != null) {
                    map[key] = maskedValue
                }
            }
            return map
        }

        override suspend fun query(condition: Condition<V>, keyCondition: Condition<K>, sortedBy: Sort<V>?, after: Pair<K, V>?, count: Int): List<Pair<K, V>> {
            sortedBy?.let { rules.sortPermittedOrDefault(user, it) }
            return underlying.query(
                    condition = condition and rules.readRuleOrDefault(user),
                    keyCondition = keyCondition,
                    sortedBy = sortedBy,
                    after = after,
                    count = count
            ).map { it.first to rules.maskOrDefault(user, it.second) }
        }
    }
}