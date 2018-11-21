package com.lightningkite.mirror.archive

import com.lightningkite.mirror.info.ClassInfo
import com.lightningkite.mirror.serialization.SerializationRegistry
import kotlin.reflect.KClass

interface Database: Datalog {

    override fun <T: Model<ID>, ID> table(type: KClass<T>, name: String): Datalog.Table<T, ID>

    interface Table<T : Model<ID>, ID>: Datalog.Table<T, ID> {
        suspend fun update(transaction: Transaction, model: T): T

        suspend fun modify(transaction: Transaction, id: ID, modifications: List<ModificationOnItem<T, *>>): T

        suspend fun delete(transaction: Transaction, id: ID): Unit
    }

}

