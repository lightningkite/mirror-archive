package com.lightningkite.mirror.archive.database

import com.lightningkite.mirror.archive.model.Condition
import com.lightningkite.mirror.archive.model.Operation
import com.lightningkite.mirror.archive.model.Sort
import com.lightningkite.mirror.info.MirrorClass

interface Database<T : Any> {

    interface Request<T : Any>
    interface Handler {
        suspend fun <T : Any> invoke(request: Request<T>): Database<T>
    }

    suspend fun get(
            condition: Condition<T> = Condition.Always,
            sort: List<Sort<T, *>> = listOf(),  //Always implied final sort is by PK
            count: Int = 100,
            after: T? = null
    ): List<T>

    suspend fun insert(
            values: List<T>
    ): List<T>

    suspend fun update(
            condition: Condition<T>,
            operation: Operation<T>
    ): Int

    suspend fun delete(
            condition: Condition<T>
    ): Int
}

