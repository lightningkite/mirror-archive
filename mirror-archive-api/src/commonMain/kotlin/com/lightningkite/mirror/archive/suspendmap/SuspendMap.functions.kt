package com.lightningkite.mirror.archive.database

import com.lightningkite.mirror.archive.model.Condition
import com.lightningkite.mirror.archive.model.Operation
import com.lightningkite.mirror.archive.model.Sort
import com.lightningkite.mirror.request.Request

interface SuspendMapServerFunction<K, V: Any> {

}

interface SuspendMapGetRequest<K, V: Any> : Request<V?>, SuspendMapServerFunction<K, V> {
    val key: K
}

interface SuspendMapPutRequest<K, V: Any> : Request<Boolean>, SuspendMapServerFunction<K, V> {
    val key: K
    val value: V
    val conditionIfExists: Condition<V>
    val create: Boolean
}

interface SuspendMapModifyRequest<K, V: Any> : Request<V?>, SuspendMapServerFunction<K, V> {
    val key: K
    val operation: Operation<V>
    val condition: Condition<V>
}

interface SuspendMapRemoveRequest<K, V: Any> : Request<Boolean>, SuspendMapServerFunction<K, V> {
    val key: K
    val condition: Condition<V>
}

interface SuspendMapQueryRequest<K, V: Any> : Request<List<SuspendMap.Entry<K, V>>>, SuspendMapServerFunction<K, V> {
    val condition: Condition<V>
    val keyCondition: Condition<K>
    val sortedBy: Sort<V>?
    val after: SuspendMap.Entry<K, V>?
    val count: Int
}

interface SuspendMapGetNewKeyRequest<K, V: Any> : Request<K>, SuspendMapServerFunction<K, V>

interface SuspendMapFindRequest<K, V: Any>: Request<SuspendMap.Entry<K, V>?>, SuspendMapServerFunction<K, V> {
    val condition: Condition<V>
    val sortedBy: Sort<V>?
}

interface SuspendMapGetManyRequest<K, V: Any>: Request<Map<K, V?>>, SuspendMapServerFunction<K, V> {
    val keys: List<K>
}

interface SuspendMapPutManyRequest<K, V: Any>: Request<Unit>, SuspendMapServerFunction<K, V> {
    val map: Map<K, V>
}

interface SuspendMapRemoveManyRequest<K, V: Any>: Request<Unit>, SuspendMapServerFunction<K, V> {
    val keys: List<K>
}