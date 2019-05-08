package com.lightningkite.mirror.archive.database

import com.lightningkite.mirror.archive.model.Condition
import com.lightningkite.mirror.archive.model.Operation
import com.lightningkite.mirror.archive.model.Sort
import com.lightningkite.mirror.request.Request

class RequestDatabaseHandler(val handler: Request.Handler) : Database.Handler {

    data class Get<T : Any>(
            val databaseRequest: Database.Request<T>,
            val condition: Condition<T> = Condition.Always,
            val sort: List<Sort<T, *>> = listOf(),
            val count: Int = 100,
            val after: T? = null
    ) : Request<List<T>>

    data class Insert<T : Any>(
            val databaseRequest: Database.Request<T>,
            val values: List<T>
    ) : Request<List<T>>

    data class Update<T : Any>(
            val databaseRequest: Database.Request<T>,
            val condition: Condition<T>,
            val operation: Operation<T>,
            val limit: Int? = null
    ) : Request<Int>

    data class Delete<T : Any>(
            val databaseRequest: Database.Request<T>,
            val condition: Condition<T>
    ) : Request<Int>

    override fun <T : Any> invoke(request: Database.Request<T>): Database<T> = object : Database<T> {
        override suspend fun get(condition: Condition<T>, sort: List<Sort<T, *>>, count: Int, after: T?): List<T> {
            return handler.invoke(Get(
                    databaseRequest = request,
                    condition = condition,
                    sort = sort,
                    count = count,
                    after = after
            ))
        }

        override suspend fun insert(values: List<T>): List<T> {
            return handler.invoke(Insert(
                    databaseRequest = request,
                    values = values
            ))
        }

        override suspend fun update(condition: Condition<T>, operation: Operation<T>, limit: Int?): Int {
            return handler.invoke(Update(
                    databaseRequest = request,
                    condition = condition,
                    operation = operation,
                    limit = limit
            ))
        }

        override suspend fun delete(condition: Condition<T>): Int {
            return handler.invoke(Delete(
                    databaseRequest = request,
                    condition = condition
            ))
        }
    }
}