package com.lightningkite.mirror.archive.property

import com.lightningkite.kommon.atomic.AtomicValue
import com.lightningkite.mirror.info.MirrorClass

class RamSuspendProperty<T>(value: T) : SuspendProperty<T> {
    val backing = AtomicValue<T>(value)

    companion object FromConfiguration : SuspendProperty.Provider.FromConfiguration {
        override val name: String get() = "RAM"
        override fun invoke(arguments: Map<String, String>) = Provider
    }

    object Provider : SuspendProperty.Provider {
        override fun <T : Any> get(mirrorClass: MirrorClass<T>, name: String, default: T): SuspendProperty<T> {
            return RamSuspendProperty(default)
        }
    }

    override suspend fun get(): T = backing.value

    override suspend fun set(value: T) {
        backing.value = value
    }

    override suspend fun compareAndSet(expected: T, value: T): Boolean {
        return backing.compareAndSet(expected, value)
    }
}