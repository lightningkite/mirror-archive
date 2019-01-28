package com.lightningkite.mirror.archive.database

import com.lightningkite.mirror.archive.model.Condition
import com.lightningkite.mirror.archive.model.Operation
import com.lightningkite.mirror.archive.model.Sort
import com.lightningkite.mirror.info.ClassInfo
import com.lightningkite.mirror.info.ClassInfoRegistry
import com.lightningkite.mirror.info.Type
import com.lightningkite.mirror.info.allImplements
import com.lightningkite.mirror.request.Request
import com.lightningkite.mirror.request.RequestHandler

class SuspendMapFunctions<K, V : Any>(
        val keyType: Type<K>,
        val valueType: Type<V>,
        val requestHandler: RequestHandler,
        val getInfo: ClassInfo<out SuspendMapGetRequest<K, V>>? = null,
        val putInfo: ClassInfo<out SuspendMapPutRequest<K, V>>? = null,
        val modifyInfo: ClassInfo<out SuspendMapModifyRequest<K, V>>? = null,
        val removeInfo: ClassInfo<out SuspendMapRemoveRequest<K, V>>? = null,
        val queryInfo: ClassInfo<out SuspendMapQueryRequest<K, V>>? = null,
        val getNewKeyInfo: ClassInfo<out SuspendMapGetNewKeyRequest<K, V>>? = null,
        val findInfo: ClassInfo<out SuspendMapFindRequest<K, V>>? = null,
        val getManyInfo: ClassInfo<out SuspendMapGetManyRequest<K, V>>? = null,
        val putManyInfo: ClassInfo<out SuspendMapPutManyRequest<K, V>>? = null,
        val removeManyInfo: ClassInfo<out SuspendMapRemoveManyRequest<K, V>>? = null
) : SuspendMap<K, V> {

    class Builder<K, V : Any>(
            val keyType: Type<K>,
            val valueType: Type<V>,
            val requestHandler: RequestHandler,
            var get: ClassInfo<out SuspendMapGetRequest<K, V>>? = null,
            var put: ClassInfo<out SuspendMapPutRequest<K, V>>? = null,
            var modify: ClassInfo<out SuspendMapModifyRequest<K, V>>? = null,
            var remove: ClassInfo<out SuspendMapRemoveRequest<K, V>>? = null,
            var query: ClassInfo<out SuspendMapQueryRequest<K, V>>? = null,
            var getNewKey: ClassInfo<out SuspendMapGetNewKeyRequest<K, V>>? = null,
            var find: ClassInfo<out SuspendMapFindRequest<K, V>>? = null,
            var getMany: ClassInfo<out SuspendMapGetManyRequest<K, V>>? = null,
            var putMany: ClassInfo<out SuspendMapPutManyRequest<K, V>>? = null,
            var removeMany: ClassInfo<out SuspendMapRemoveManyRequest<K, V>>? = null
    ) {
        fun build(
        ): SuspendMapFunctions<K, V> = SuspendMapFunctions(
                keyType = keyType,
                valueType = valueType,
                requestHandler = requestHandler,
                getInfo = get,
                putInfo = put,
                modifyInfo = modify,
                removeInfo = remove,
                queryInfo = query,
                getNewKeyInfo = getNewKey,
                findInfo = find,
                getManyInfo = getMany,
                putManyInfo = putMany,
                removeManyInfo = removeMany
        )
    }

    suspend inline fun <T> Request<T>.invoke(): T {
        @Suppress("UNCHECKED_CAST")
        return requestHandler.invoke(this)
    }

    override suspend fun find(condition: Condition<V>, sortedBy: Sort<V>?): SuspendMap.Entry<K, V>? {
        return if (findInfo != null) {
            findInfo.construct(mapOf(
                    "condition" to condition,
                    "sortedBy" to sortedBy
            )).invoke()
        } else if (queryInfo != null) {
            queryInfo.construct(mapOf(
                    "condition" to condition,
                    "keyCondition" to Condition.Always<K>(),
                    "sortedBy" to sortedBy,
                    "after" to null,
                    "count" to 1
            )).invoke().firstOrNull()
        } else throw UnsupportedOperationException()
    }

    override suspend fun get(key: K): V? {
        return if (getInfo != null) {
            getInfo.construct(mapOf(
                    "key" to key
            )).invoke()
        } else throw UnsupportedOperationException()
    }

    override suspend fun getMany(keys: List<K>): Map<K, V?> {
        return if (getManyInfo != null) {
            getManyInfo.construct(mapOf(
                    "keys" to keys.toList()
            )).invoke()
        } else super.getMany(keys)
    }

    override suspend fun getNewKey(): K {
        return if (getNewKeyInfo != null) {
            getNewKeyInfo.construct(mapOf()).invoke()
        } else throw UnsupportedOperationException()
    }

    override suspend fun modify(key: K, operation: Operation<V>, condition: Condition<V>): V? {
        return if (modifyInfo != null) {
            modifyInfo.construct(mapOf(
                    "key" to key,
                    "operation" to operation,
                    "condition" to condition
            )).invoke()
        } else super.modify(key, operation, condition)
    }

    override suspend fun put(key: K, value: V, conditionIfExists: Condition<V>, create: Boolean): Boolean {
        return if (putInfo != null) {
            putInfo.construct(mapOf(
                    "key" to key,
                    "value" to value,
                    "conditionIfExists" to conditionIfExists,
                    "create" to create
            )).invoke()
        } else throw UnsupportedOperationException()
    }

    override suspend fun putMany(map: Map<K, V>) {
        return if (putManyInfo != null) {
            putManyInfo.construct(mapOf(
                    "map" to map
            )).invoke()
        } else super.putMany(map)
    }

    override suspend fun query(
            condition: Condition<V>,
            keyCondition: Condition<K>,
            sortedBy: Sort<V>?,
            after: SuspendMap.Entry<K, V>?,
            count: Int
    ): List<SuspendMap.Entry<K, V>> {
        return if (queryInfo != null) {
            queryInfo.construct(mapOf(
                    "condition" to condition,
                    "keyCondition" to keyCondition,
                    "sortedBy" to sortedBy,
                    "after" to after,
                    "count" to count
            )).invoke()
        } else throw UnsupportedOperationException()
    }

    override suspend fun remove(key: K, condition: Condition<V>): Boolean {
        return if (removeInfo != null) {
            removeInfo.construct(mapOf(
                    "key" to key,
                    "condition" to condition
            )).invoke()
        } else throw UnsupportedOperationException()
    }

    override suspend fun removeMany(keys: List<K>) {
        return if (removeManyInfo != null) {
            removeManyInfo.construct(mapOf(
                    "keys" to keys.toList()
            )).invoke()
        } else super.removeMany(keys)
    }

    companion object {

        @Suppress("UNCHECKED_CAST")
        fun all(
                requestHandler: RequestHandler,
                classInfoRegistry: ClassInfoRegistry
        ): Map<Pair<Type<Any?>, Type<Any>>, SuspendMapFunctions<Any?, Any>> {

            val map = HashMap<Type<*>, HashMap<Type<*>, SuspendMapFunctions.Builder<Any?, Any>>>()
            classInfoRegistry.values.forEach { classInfo ->
                classInfo.allImplements(classInfoRegistry).forEach { implements ->
                    when (implements.kClass) {
                        SuspendMapGetRequest::class -> {
                            map.getOrPut(implements.typeParameters[0].type) { HashMap() }
                                    .getOrPut(implements.typeParameters[1].type) {
                                        SuspendMapFunctions.Builder(
                                                keyType = implements.typeParameters[0].type as Type<Any?>,
                                                valueType = implements.typeParameters[1].type as Type<Any>,
                                                requestHandler = requestHandler
                                        )
                                    }
                                    .let { builder ->
                                        @Suppress("UNCHECKED_CAST")
                                        builder.get = classInfo as ClassInfo<out SuspendMapGetRequest<Any?, Any>>
                                    }
                        }
                        SuspendMapPutRequest::class -> {
                            map.getOrPut(implements.typeParameters[0].type) { HashMap() }
                                    .getOrPut(implements.typeParameters[1].type) {
                                        SuspendMapFunctions.Builder(
                                                keyType = implements.typeParameters[0].type as Type<Any?>,
                                                valueType = implements.typeParameters[1].type as Type<Any>,
                                                requestHandler = requestHandler
                                        )
                                    }
                                    .let { builder ->
                                        @Suppress("UNCHECKED_CAST")
                                        builder.put = classInfo as ClassInfo<out SuspendMapPutRequest<Any?, Any>>
                                    }
                        }
                        SuspendMapModifyRequest::class -> {
                            map.getOrPut(implements.typeParameters[0].type) { HashMap() }
                                    .getOrPut(implements.typeParameters[1].type) {
                                        SuspendMapFunctions.Builder(
                                                keyType = implements.typeParameters[0].type as Type<Any?>,
                                                valueType = implements.typeParameters[1].type as Type<Any>,
                                                requestHandler = requestHandler
                                        )
                                    }
                                    .let { builder ->
                                        @Suppress("UNCHECKED_CAST")
                                        builder.modify = classInfo as ClassInfo<out SuspendMapModifyRequest<Any?, Any>>
                                    }
                        }
                        SuspendMapRemoveRequest::class -> {
                            map.getOrPut(implements.typeParameters[0].type) { HashMap() }
                                    .getOrPut(implements.typeParameters[1].type) {
                                        SuspendMapFunctions.Builder(
                                                keyType = implements.typeParameters[0].type as Type<Any?>,
                                                valueType = implements.typeParameters[1].type as Type<Any>,
                                                requestHandler = requestHandler
                                        )
                                    }
                                    .let { builder ->
                                        @Suppress("UNCHECKED_CAST")
                                        builder.remove = classInfo as ClassInfo<out SuspendMapRemoveRequest<Any?, Any>>
                                    }
                        }
                        SuspendMapQueryRequest::class -> {
                            map.getOrPut(implements.typeParameters[0].type) { HashMap() }
                                    .getOrPut(implements.typeParameters[1].type) {
                                        SuspendMapFunctions.Builder(
                                                keyType = implements.typeParameters[0].type as Type<Any?>,
                                                valueType = implements.typeParameters[1].type as Type<Any>,
                                                requestHandler = requestHandler
                                        )
                                    }
                                    .let { builder ->
                                        @Suppress("UNCHECKED_CAST")
                                        builder.query = classInfo as ClassInfo<out SuspendMapQueryRequest<Any?, Any>>
                                    }
                        }
                        SuspendMapGetNewKeyRequest::class -> {
                            map.getOrPut(implements.typeParameters[0].type) { HashMap() }
                                    .getOrPut(implements.typeParameters[1].type) {
                                        SuspendMapFunctions.Builder(
                                                keyType = implements.typeParameters[0].type as Type<Any?>,
                                                valueType = implements.typeParameters[1].type as Type<Any>,
                                                requestHandler = requestHandler
                                        )
                                    }
                                    .let { builder ->
                                        @Suppress("UNCHECKED_CAST")
                                        builder.getNewKey = classInfo as ClassInfo<out SuspendMapGetNewKeyRequest<Any?, Any>>
                                    }
                        }
                        SuspendMapFindRequest::class -> {
                            map.getOrPut(implements.typeParameters[0].type) { HashMap() }
                                    .getOrPut(implements.typeParameters[1].type) {
                                        SuspendMapFunctions.Builder(
                                                keyType = implements.typeParameters[0].type as Type<Any?>,
                                                valueType = implements.typeParameters[1].type as Type<Any>,
                                                requestHandler = requestHandler
                                        )
                                    }
                                    .let { builder ->
                                        @Suppress("UNCHECKED_CAST")
                                        builder.find = classInfo as ClassInfo<out SuspendMapFindRequest<Any?, Any>>
                                    }
                        }
                        SuspendMapGetManyRequest::class -> {
                            map.getOrPut(implements.typeParameters[0].type) { HashMap() }
                                    .getOrPut(implements.typeParameters[1].type) {
                                        SuspendMapFunctions.Builder(
                                                keyType = implements.typeParameters[0].type as Type<Any?>,
                                                valueType = implements.typeParameters[1].type as Type<Any>,
                                                requestHandler = requestHandler
                                        )
                                    }
                                    .let { builder ->
                                        @Suppress("UNCHECKED_CAST")
                                        builder.getMany = classInfo as ClassInfo<out SuspendMapGetManyRequest<Any?, Any>>
                                    }
                        }
                        SuspendMapPutManyRequest::class -> {
                            map.getOrPut(implements.typeParameters[0].type) { HashMap() }
                                    .getOrPut(implements.typeParameters[1].type) {
                                        SuspendMapFunctions.Builder(
                                                keyType = implements.typeParameters[0].type as Type<Any?>,
                                                valueType = implements.typeParameters[1].type as Type<Any>,
                                                requestHandler = requestHandler
                                        )
                                    }
                                    .let { builder ->
                                        @Suppress("UNCHECKED_CAST")
                                        builder.putMany = classInfo as ClassInfo<out SuspendMapPutManyRequest<Any?, Any>>
                                    }
                        }
                        SuspendMapRemoveManyRequest::class -> {
                            map.getOrPut(implements.typeParameters[0].type) { HashMap() }
                                    .getOrPut(implements.typeParameters[1].type) {
                                        SuspendMapFunctions.Builder(
                                                keyType = implements.typeParameters[0].type as Type<Any?>,
                                                valueType = implements.typeParameters[1].type as Type<Any>,
                                                requestHandler = requestHandler
                                        )
                                    }
                                    .let { builder ->
                                        @Suppress("UNCHECKED_CAST")
                                        builder.removeMany = classInfo as ClassInfo<out SuspendMapRemoveManyRequest<Any?, Any>>
                                    }
                        }
                        else -> {
                        }
                    }
                }
            }
            return map.values.asSequence()
                    .flatMap { it.values.asSequence() }
                    .map { it.build() }
                    .associate { (it.keyType to it.valueType) to it }
        }
    }
}