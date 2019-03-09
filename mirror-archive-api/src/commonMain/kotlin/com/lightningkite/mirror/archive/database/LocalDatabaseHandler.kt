package com.lightningkite.mirror.archive.database

import kotlin.reflect.KClass

class LocalDatabaseHandler : Database.Handler {

    var defaultInvocation: Any.() -> Database<*> = {
        throw IllegalArgumentException("No invocation for type ${this::class} is known.")
    }

    val invocations = HashMap<KClass<out Database.Request<*>>, (Any) -> Database<*>>()
    inline fun <reified R : Database.Request<T>, T> invocation(noinline action: R.() -> T) {
        @Suppress("UNCHECKED_CAST")
        invocations[R::class] = action as (Any) -> Database<T>
    }

    fun <R : Database.Request<T>, T> invocation(kclass: KClass<R>, action: R.() -> Database<T>) {
        @Suppress("UNCHECKED_CAST")
        invocations[kclass] = action as (Any) -> Database<T>
    }


    @Suppress("UNCHECKED_CAST")
    override suspend fun <T> invoke(request: Database.Request<T>): Database<T> {
        val invocation = invocations[request::class] ?: defaultInvocation
        val result = invocation.invoke(request)
        return result as Database<T>
    }
}

