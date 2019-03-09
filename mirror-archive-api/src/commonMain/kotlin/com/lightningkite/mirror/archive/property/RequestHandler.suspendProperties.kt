package com.lightningkite.mirror.archive.property

import com.lightningkite.mirror.request.LocalRequestHandler
import com.lightningkite.mirror.request.Request
import kotlin.reflect.KClass

@Suppress("UNCHECKED_CAST")
fun LocalRequestHandler.suspendProperties(databaseHandler: SuspendProperty.Handler) {
    invocation(RequestSuspendPropertyHandler.Get::class as KClass<RequestSuspendPropertyHandler.Get<Any?>>) {
        databaseHandler.invoke(this.propertyRequest).get()
    }
    invocation(RequestSuspendPropertyHandler.Set::class as KClass<RequestSuspendPropertyHandler.Set<Any?>>) {
        databaseHandler.invoke(this.propertyRequest).set(value)
    }
    invocation(RequestSuspendPropertyHandler.CompareAndSet::class as KClass<RequestSuspendPropertyHandler.CompareAndSet<Any?>>) {
        databaseHandler.invoke(this.propertyRequest).compareAndSet(expected, value)
    }
}