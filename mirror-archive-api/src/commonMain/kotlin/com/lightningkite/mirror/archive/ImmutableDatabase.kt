package com.lightningkite.mirror.archive

import com.lightningkite.mirror.info.ClassInfo
import com.lightningkite.mirror.serialization.externalName

interface ImmutableDatabase {
    fun <T: Model<ID>, ID> table(type: ClassInfo<T>, name: String = type.kClass.externalName): ImmutableDatabase.Table<T, ID>

    interface Table<T : Model<ID>, ID> {

        suspend fun get(transaction: Transaction, id: ID): T

        suspend fun getMany(transaction: Transaction, ids: Iterable<ID>): List<T> = ids.map { get(transaction, it) }

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