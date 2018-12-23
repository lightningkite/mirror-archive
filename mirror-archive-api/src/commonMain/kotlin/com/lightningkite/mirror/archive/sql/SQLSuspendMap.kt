package com.lightningkite.mirror.archive.sql

import com.lightningkite.mirror.archive.database.SuspendMap
import com.lightningkite.mirror.archive.model.Condition
import com.lightningkite.mirror.archive.model.HasId
import com.lightningkite.mirror.archive.model.Id
import com.lightningkite.mirror.archive.model.Sort

class SQLSuspendMap<T: HasId>(
        val connection: SQLConnection,
        val serializer: SQLSerializer
): SuspendMap<Id, T> {

    init {
        //setup
    }

    override suspend fun getNewKey(): Id = Id.key()

    override suspend fun get(key: Id): T? {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override suspend fun put(key: Id, value: T, conditionIfExists: Condition<T>, create: Boolean): Boolean {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override suspend fun remove(key: Id, condition: Condition<T>): Boolean {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override suspend fun query(condition: Condition<T>, sortedBy: Sort<T>, after: T?, count: Int): List<T> {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }
}