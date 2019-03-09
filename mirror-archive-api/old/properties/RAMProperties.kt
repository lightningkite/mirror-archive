package com.lightningkite.mirror.archive.properties

import com.lightningkite.mirror.info.Type

class RAMProperties : Properties {

    val properties = HashMap<String, Property<*>>()

    class Property<T>(var value: T) : Properties.Property<T> {
        override suspend fun get(): T = value

        override suspend fun set(value: T) {
            this.value = value
        }

        override suspend fun compareAndSet(current: T, next: T): Boolean {
            if(value == current){
                value = next
                return true
            }
            return false
        }
    }

    @Suppress("UNCHECKED_CAST")
    override fun <T> property(name: String, type: Type<T>, defaultIfNotPresent: T): Properties.Property<T> = properties.getOrPut(name){
        Property(defaultIfNotPresent)
    } as Property<T>
}