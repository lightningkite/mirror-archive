package com.lightningkite.mirror.archive.property

import com.lightningkite.mirror.request.Request

class RequestSuspendPropertyHandler(val handler: Request.Handler) : SuspendProperty.Handler {

    data class Get<T>(
            val propertyRequest: SuspendProperty.Request<T>
    ) : Request<T>

    data class Set<T>(
            val propertyRequest: SuspendProperty.Request<T>,
            val value: T
    ) : Request<Unit>

    data class CompareAndSet<T>(
            val propertyRequest: SuspendProperty.Request<T>,
            val expected: T,
            val value: T
    ) : Request<Boolean>

    override fun <T> invoke(request: SuspendProperty.Request<T>): SuspendProperty<T> = object : SuspendProperty<T> {
        override suspend fun get(): T {
            return handler.invoke(Get(request))
        }

        override suspend fun set(value: T) {
            return handler.invoke(Set(request, value))
        }

        override suspend fun compareAndSet(expected: T, value: T): Boolean {
            return handler.invoke(CompareAndSet(request, expected, value))
        }

    }
}