package com.lightningkite.mirror.archive.property

import kotlin.reflect.KClass

class LocalSuspendPropertyHandler : SuspendProperty.Handler {
    var defaultInvocation: Any.() -> SuspendProperty<*> = {
        throw IllegalArgumentException("No invocation for type ${this::class} is known.")
    }

    val invocations = HashMap<KClass<out SuspendProperty.Request<*>>, (Any) -> SuspendProperty<*>>()
    inline fun <reified R : SuspendProperty.Request<T>, T> invocation(noinline action: R.() -> T) {
        @Suppress("UNCHECKED_CAST")
        invocations[R::class] = action as (Any) -> SuspendProperty<T>
    }

    fun <R : SuspendProperty.Request<T>, T> invocation(kclass: KClass<R>, action: R.() -> SuspendProperty<T>) {
        @Suppress("UNCHECKED_CAST")
        invocations[kclass] = action as (Any) -> SuspendProperty<T>
    }


    @Suppress("UNCHECKED_CAST")
    override suspend fun <T> invoke(request: SuspendProperty.Request<T>): SuspendProperty<T> {
        val invocation = invocations[request::class] ?: defaultInvocation
        val result = invocation.invoke(request)
        return result as SuspendProperty<T>
    }
}