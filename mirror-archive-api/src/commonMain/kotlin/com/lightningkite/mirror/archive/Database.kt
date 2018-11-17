package com.lightningkite.mirror.archive

import com.lightningkite.mirror.info.ClassInfo

interface Database: Datalog {
    override fun <T: Model<ID>, ID> table(type: ClassInfo<T>, name: String): Datalog.Table<T, ID>

    interface Table<T : Model<ID>, ID>: Datalog.Table<T, ID> {
        suspend fun update(transaction: Transaction, model: T): T

        suspend fun modify(transaction: Transaction, id: ID, modifications: List<ModificationOnItem<T, *>>): T

        suspend fun delete(transaction: Transaction, id: ID): Unit
    }

}

