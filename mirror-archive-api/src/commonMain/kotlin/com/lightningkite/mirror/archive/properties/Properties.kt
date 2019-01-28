package com.lightningkite.mirror.archive.properties

import com.lightningkite.mirror.info.Type
import com.lightningkite.reacktive.property.ObservableProperty

interface Properties {
    interface Property<T> {
        suspend fun get(): T
        suspend fun set(value: T)
        suspend fun compareAndSet(current: T, next: T): Boolean
        suspend fun listen(): ObservableProperty<T>
        suspend fun clear()
    }

    fun <T> property(name: String, type: Type<T>, defaultIfNotPresent: T): Property<T>
}

