package com.lightningkite.mirror.archive.property

import com.lightningkite.kommon.atomic.AtomicReference

class LocalSuspendProperty<T>(value: T) : SuspendProperty<T> {
    val backing = AtomicReference<T>(value)

    override suspend fun get(): T = backing.value

    override suspend fun set(value: T) {
        backing.value = value
    }

    override suspend fun compareAndSet(expected: T, value: T): Boolean {
        return backing.compareAndSet(expected, value)
    }
}