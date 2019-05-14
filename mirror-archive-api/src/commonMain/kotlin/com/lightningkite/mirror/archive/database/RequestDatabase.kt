package com.lightningkite.mirror.archive.database

import com.lightningkite.mirror.archive.model.Condition
import com.lightningkite.mirror.archive.model.Operation
import com.lightningkite.mirror.archive.model.Sort
import com.lightningkite.mirror.request.Request

class RequestDatabase<T : Any>(val handler: Request.Handler, val request: Database.Request<T>) : Database<T> {

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
            val operation: Operation<T>
    ) : Request<Int>

    data class LimitedUpdate<T : Any>(
            val databaseRequest: Database.Request<T>,
            val condition: Condition<T>,
            val operation: Operation<T>,
            val sort: List<Sort<T, *>> = listOf(),
            val limit: Int
    ) : Request<Int>

    data class Delete<T : Any>(
            val databaseRequest: Database.Request<T>,
            val condition: Condition<T>
    ) : Request<Int>

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

    override suspend fun update(condition: Condition<T>, operation: Operation<T>): Int {
        return handler.invoke(Update(
                databaseRequest = request,
                condition = condition,
                operation = operation
        ))
    }

    override suspend fun limitedUpdate(condition: Condition<T>, operation: Operation<T>, sort: List<Sort<T, *>>, limit: Int): Int {
        return handler.invoke(LimitedUpdate(
                databaseRequest = request,
                condition = condition,
                operation = operation,
                sort = sort,
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