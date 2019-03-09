package com.lightningkite.mirror.archive.database

import com.lightningkite.mirror.archive.model.Condition
import com.lightningkite.mirror.archive.model.Operation
import com.lightningkite.mirror.archive.model.Sort
import com.lightningkite.mirror.request.Request

class RequestDatabaseHandler(val handler: Request.Handler) : Database.Handler {

    data class Get<T>(
            val databaseRequest: Database.Request<T>,
            val condition: Condition<T> = Condition.Always,
            val sort: Sort<T>? = null,
            val count: Int = 100,
            val after: T? = null
    ) : Request<List<T>>

    data class Insert<T>(
            val databaseRequest: Database.Request<T>,
            val values: List<T>
    ) : Request<List<T?>>

    data class Update<T>(
            val databaseRequest: Database.Request<T>,
            val condition: Condition<T>,
            val operation: Operation<T>
    ) : Request<Int>

    data class Delete<T>(
            val databaseRequest: Database.Request<T>,
            val condition: Condition<T>
    ) : Request<Int>

    override suspend fun <T> invoke(request: Database.Request<T>): Database<T> = object : Database<T> {
        override suspend fun get(condition: Condition<T>, sort: Sort<T>?, count: Int, after: T?): List<T> {
            return handler.invoke(Get(
                    databaseRequest = request,
                    condition = condition,
                    sort = sort,
                    count = count,
                    after = after
            ))
        }

        override suspend fun insert(values: List<T>): List<T?> {
            return handler.invoke(Insert(
                    databaseRequest = request,
                    values = values
            ))
        }

        override suspend fun update(condition: Condition<T>, operation: Operation<T>): Int {
            return handler.invoke(Update(
                    databaseRequest = request,
                    condition = condition,
                    operation = operation
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