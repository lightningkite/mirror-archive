package com.lightningkite.mirror.archive

import com.lightningkite.mirror.info.ClassInfo
import kotlin.reflect.KClass

interface Datalog: ImmutableDatabase {
    override fun <T: Model<ID>, ID> table(type: KClass<T>, name: String): Datalog.Table<T, ID>

    interface Table<T : Model<ID>, ID>: ImmutableDatabase.Table<T, ID> {
        suspend fun insert(transaction: Transaction, model: T): T
        suspend fun insertMany(transaction: Transaction, models: Collection<T>): Collection<T> = models.map { insert(transaction, it) }
    }

}