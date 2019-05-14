package com.lightningkite.mirror.archive.database

import com.lightningkite.mirror.request.LocalRequestHandler
import com.lightningkite.mirror.request.Request
import kotlin.reflect.KClass

@Suppress("UNCHECKED_CAST")
fun LocalRequestHandler.databases(databaseHandler: Database.Handler) {
    invocation(RequestDatabase.Get::class as KClass<RequestDatabase.Get<Any>>) {
        databaseHandler.invoke(this.databaseRequest).get(condition, sort, count, after)
    }
    invocation(RequestDatabase.Delete::class as KClass<RequestDatabase.Delete<Any>>) {
        databaseHandler.invoke(this.databaseRequest).delete(condition)
    }
    invocation(RequestDatabase.Update::class as KClass<RequestDatabase.Update<Any>>) {
        databaseHandler.invoke(this.databaseRequest).update(condition, operation)
    }
    invocation(RequestDatabase.Insert::class as KClass<RequestDatabase.Insert<Any>>) {
        databaseHandler.invoke(this.databaseRequest).insert(values)
    }
    invocation(RequestDatabase.LimitedUpdate::class as KClass<RequestDatabase.LimitedUpdate<Any>>) {
        databaseHandler.invoke(this.databaseRequest).limitedUpdate(condition, operation, sort, limit)
    }
}