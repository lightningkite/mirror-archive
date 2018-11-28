package com.lightningkite.mirror.archive

import kotlin.reflect.KClass

interface Database: Datalog {

    override fun <T: HasId> table(type: KClass<T>, name: String): Database.Table<T>

    interface Table<T : HasId>: Datalog.Table<T> {
        suspend fun update(transaction: Transaction, model: T): T

        suspend fun modify(transaction: Transaction, id: Id, modifications: List<ModificationOnItem<T, *>>): T

        suspend fun delete(transaction: Transaction, id: Id): Unit
    }

}

