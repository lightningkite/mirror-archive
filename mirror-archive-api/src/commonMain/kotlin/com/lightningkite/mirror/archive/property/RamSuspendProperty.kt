package com.lightningkite.mirror.archive.property

import com.lightningkite.kommon.atomic.AtomicValue

class RamSuspendProperty<T>(value: T) : SuspendProperty<T> {
    val backing = AtomicValue<T>(value)

    override suspend fun get(): T = backing.value

    override suspend fun set(value: T) {
        println("It's been set to $value")
        backing.value = value
    }

    override suspend fun compareAndSet(expected: T, value: T): Boolean {
        println("It's currently ${get()}, should be $expected and will be $value")
        val result = backing.compareAndSet(expected, value)
        println("$result: It's changed to ${backing.value}")
        return result
    }
}