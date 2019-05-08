package com.lightningkite.mirror.archive.property

import com.lightningkite.mirror.info.MirrorClass


interface SuspendProperty<T> {
    suspend fun get(): T
    suspend fun set(value: T)
    suspend fun compareAndSet(expected: T, value: T): Boolean

    interface Request<T>
    interface Handler {
        suspend fun <T> invoke(request: Request<T>): SuspendProperty<T>
    }

    interface Provider {
        fun <T : Any> get(
                mirrorClass: MirrorClass<T>,
                name: String,
                default: T
        ): SuspendProperty<T>

        interface FromConfiguration {
            val name: String
            val requiredArguments: Array<String> get() = arrayOf()
            val optionalArguments: Array<String> get() = arrayOf()
            operator fun invoke(arguments: Map<String, String>): Provider
        }
    }
}

