package com.lightningkite.mirror.archive

import kotlin.reflect.KClass

interface Datalog: ImmutableDatabase {
    override fun <T: HasId> table(type: KClass<T>, name: String): Datalog.Table<T>

    interface Table<T : HasId>: ImmutableDatabase.Table<T> {
        suspend fun insert(transaction: Transaction, model: T): T
        suspend fun insertMany(transaction: Transaction, models: Collection<T>): Collection<T> = models.map { insert(transaction, it) }
    }

}