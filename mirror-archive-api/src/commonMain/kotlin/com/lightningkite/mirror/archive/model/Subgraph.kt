package com.lightningkite.mirror.archive.model

import com.lightningkite.mirror.archive.database.Database
import com.lightningkite.mirror.archive.database.get
import com.lightningkite.mirror.info.MirrorClass
import com.lightningkite.mirror.info.MirrorRegistry
import kotlin.reflect.KClass

//typealias Subgraph = Map<Reference<*>, HasUuid>
//typealias MutableSubgraph = MutableMap<Reference<*>, HasUuid>
//
//
//fun <T: HasUuid> MutableSubgraph.add(reference: Reference<T>, item: T) = this.put(reference, item)
//operator fun <T: HasUuid> Subgraph.get(reference: Reference<T>): T? = this[reference] as? T
//
//data class TestObject(override val id: Uuid) : HasUuid {
//
//}
//
//fun test(graph: Subgraph){
//    val get: TestObject = graph[Reference<TestObject>(Uuid.randomUUID4())]
//}


inline class Subgraph(val map: MutableMap<KClass<*>, MutableMap<Any?, HasId<*>>> = HashMap()) {
    @Suppress("UNCHECKED_CAST")
    fun <T : HasId<ID>, ID> get(type: KClass<T>, id: ID): T? = map[type]?.get(id) as? T

    fun <T : HasId<ID>, ID> put(type: KClass<T>, item: T): T {
        map.getOrPut(type) { HashMap() }.put(item.id, item)
        return item
    }

    fun putAll(subgraph: Subgraph) {
        for ((type, submap) in subgraph.map) {
            map.getOrPut(type) { HashMap() }.putAll(submap)
        }
    }

    fun clear(){
        map.clear()
    }

    inline fun <reified T : HasId<ID>, ID> get(id: ID): T? = get(T::class, id)
    inline fun <reified T : HasId<ID>, ID> put(item: T): T? = put(T::class, item)
}

suspend inline fun <reified MODEL : HasUuid> Reference<MODEL>.resolve(provider: Database.Provider, subgraph: Subgraph? = null): MODEL? = resolve(MirrorRegistry[MODEL::class]!!, provider, subgraph)
suspend fun <MODEL : HasUuid> Reference<MODEL>.resolve(type: MirrorClass<MODEL>, provider: Database.Provider, subgraph: Subgraph? = null): MODEL? {
    val db = provider.get(type)
    @Suppress("UNCHECKED_CAST") val resolved = db.get(type.fields.find { it.name == "id" } as MirrorClass.Field<MODEL, Uuid>, key)
    if (resolved != null) {
        return subgraph?.put(type.kClass, resolved) ?: resolved
    } else {
        return null
    }
}