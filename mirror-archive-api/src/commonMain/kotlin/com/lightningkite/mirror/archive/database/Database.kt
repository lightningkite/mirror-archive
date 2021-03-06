package com.lightningkite.mirror.archive.database

import com.lightningkite.mirror.archive.model.Condition
import com.lightningkite.mirror.archive.model.Operation
import com.lightningkite.mirror.archive.model.Sort
import com.lightningkite.mirror.info.MirrorClass

interface Database<T : Any> {

    interface Request<T : Any>
    interface Handler {
        suspend fun <T : Any> invoke(request: Request<T>): Database<T>
    }

    interface Provider {
        operator fun <T : Any> get(
                mirrorClass: MirrorClass<T>
        ): Database<T> = getOrNull(mirrorClass) ?: throw IllegalArgumentException("No database for type ${mirrorClass.localName} accessible")
        fun <T : Any> getOrNull(
                mirrorClass: MirrorClass<T>
        ): Database<T>?

        interface FromConfiguration {
            val name: String
            val requiredArguments: Array<String> get() = arrayOf()
            val optionalArguments: Array<String> get() = arrayOf()
            operator fun invoke(arguments: Map<String, String>): Provider
        }
    }

    suspend fun get(
            condition: Condition<T> = Condition.Always,
            sort: List<Sort<T, *>> = listOf(),  //Always implied final sort is by PK
            count: Int = 100,
            after: T? = null
    ): List<T>

    suspend fun insert(
            values: List<T>
    ): List<T>

    suspend fun update(
            condition: Condition<T>,
            operation: Operation<T>
    ): Int

    suspend fun limitedUpdate(
            condition: Condition<T>,
            operation: Operation<T>,
            sort: List<Sort<T, *>> = listOf(),
            limit: Int
    ): Int

    suspend fun delete(
            condition: Condition<T>
    ): Int

    suspend fun count(
            condition: Condition<T>
    ): Int
}

