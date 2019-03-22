package com.lightningkite.mirror.archive.database

import com.lightningkite.mirror.request.LocalRequestHandler
import com.lightningkite.mirror.request.Request
import kotlin.reflect.KClass

@Suppress("UNCHECKED_CAST")
fun LocalRequestHandler.databases(databaseHandler: Database.Handler) {
    invocation(RequestDatabaseHandler.Get::class as KClass<RequestDatabaseHandler.Get<Any>>) {
        databaseHandler.invoke(this.databaseRequest).get(condition, sort, count, after)
    }
    invocation(RequestDatabaseHandler.Delete::class as KClass<RequestDatabaseHandler.Delete<Any>>) {
        databaseHandler.invoke(this.databaseRequest).delete(condition)
    }
    invocation(RequestDatabaseHandler.Update::class as KClass<RequestDatabaseHandler.Update<Any>>) {
        databaseHandler.invoke(this.databaseRequest).update(condition, operation)
    }
    invocation(RequestDatabaseHandler.Insert::class as KClass<RequestDatabaseHandler.Insert<Any>>) {
        databaseHandler.invoke(this.databaseRequest).insert(values)
    }
}