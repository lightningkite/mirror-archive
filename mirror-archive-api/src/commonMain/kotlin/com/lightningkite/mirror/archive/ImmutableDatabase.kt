package com.lightningkite.mirror.archive

import com.lightningkite.mirror.info.ClassInfo
import com.lightningkite.mirror.serialization.SerializationRegistry
import kotlin.reflect.KClass

interface ImmutableDatabase {

    val registry: SerializationRegistry

    fun <T: HasId> table(type: KClass<T>, name: String = registry.kClassToExternalNameRegistry[type]!!): ImmutableDatabase.Table<T>

    interface Table<T : HasId> {

        val classInfo: ClassInfo<T>

        suspend fun get(transaction: Transaction, id: Id): T?

        suspend fun getSure(transaction: Transaction, id: Id): T = get(transaction, id) ?: throw NoSuchElementException()

        suspend fun getMany(transaction: Transaction, ids: Iterable<Id>): Map<Id, T?> = ids.associate { it to get(transaction, it) }

        suspend fun queryOne(
                transaction: Transaction,
                condition: ConditionOnItem<T> = ConditionOnItem.Always<T>()
        ) = query(transaction = transaction, condition = condition, count = 1).results.firstOrNull()

        suspend fun query(
                transaction: Transaction,
                condition: ConditionOnItem<T> = ConditionOnItem.Always<T>(),
                sortedBy: List<SortOnItem<T, *>> = listOf(),
                continuationToken: String? = null,
                count: Int = 100
        ): QueryResult<T>
    }

}