package com.lightningkite.mirror.archive.property


interface SuspendProperty<T> {
    suspend fun get(): T
    suspend fun set(value: T)
    suspend fun compareAndSet(expected: T, value: T): Boolean

    interface Request<T>
    interface Handler {
        suspend fun <T> invoke(request: Request<T>): SuspendProperty<T>
    }
}

