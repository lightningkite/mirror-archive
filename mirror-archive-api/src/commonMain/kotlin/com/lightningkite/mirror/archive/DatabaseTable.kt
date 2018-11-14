package com.lightningkite.mirror.archive

import com.lightningkite.kotlinx.observable.list.ObservableList
import com.lightningkite.kotlinx.observable.property.ObservableProperty

interface DatabaseTable<T : Model<ID>, ID> {

    suspend fun get(transaction: Transaction, id: ID): T

    suspend fun getMany(transaction: Transaction, ids: Iterable<ID>): List<T> = ids.map { get(transaction, it) }

    suspend fun insert(transaction: Transaction, model: T): T

    suspend fun insertMany(transaction: Transaction, models: Collection<T>): Collection<T> = models.map { insert(transaction, it) } //Parallelize?

    suspend fun update(transaction: Transaction, model: T): T

    suspend fun modify(transaction: Transaction, id: ID, modifications: List<ModificationOnItem<T, *>>): T

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

    suspend fun delete(transaction: Transaction, id: ID): Unit
}
